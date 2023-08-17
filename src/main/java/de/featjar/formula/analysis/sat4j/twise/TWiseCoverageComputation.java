/*
 * Copyright (C) 2023 FeatJAR-Development-Team
 *
 * This file is part of FeatJAR-formula-analysis-sat4j.
 *
 * formula-analysis-sat4j is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3.0 of the License,
 * or (at your option) any later version.
 *
 * formula-analysis-sat4j is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with formula-analysis-sat4j. If not, see <https://www.gnu.org/licenses/>.
 *
 * See <https://github.com/FeatureIDE/FeatJAR-formula-analysis-sat4j> for further information.
 */
package de.featjar.formula.analysis.sat4j.twise;

import de.featjar.base.computation.Computations;
import de.featjar.base.computation.Dependency;
import de.featjar.base.computation.IComputation;
import de.featjar.base.computation.Progress;
import de.featjar.base.data.ExpandableIntegerList;
import de.featjar.base.data.Result;
import de.featjar.formula.analysis.RuntimeContradictionException;
import de.featjar.formula.analysis.bool.BooleanClauseList;
import de.featjar.formula.analysis.bool.BooleanSolution;
import de.featjar.formula.analysis.bool.BooleanSolutionList;
import de.featjar.formula.analysis.combinations.LexicographicIterator;
import de.featjar.formula.analysis.combinations.LexicographicIterator.Combination;
import de.featjar.formula.analysis.mig.solver.MIGBuilder;
import de.featjar.formula.analysis.mig.solver.ModalImplicationGraph;
import de.featjar.formula.analysis.sat4j.ASAT4JAnalysis;
import de.featjar.formula.analysis.sat4j.solver.ISelectionStrategy;
import de.featjar.formula.analysis.sat4j.solver.SAT4JSolutionSolver;
import de.featjar.formula.analysis.sat4j.solver.SAT4JSolver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Calculates statistics regarding t-wise feature coverage of a set of
 * solutions.
 *
 * @author Sebastian Krieter
 */
public class TWiseCoverageComputation extends ASAT4JAnalysis<CoverageStatistic> {
    public static final Dependency<Integer> T = Dependency.newDependency(Integer.class);
    public static final Dependency<ModalImplicationGraph> MIG = Dependency.newDependency(ModalImplicationGraph.class);
    public static final Dependency<BooleanSolutionList> SAMPLE = Dependency.newDependency(BooleanSolutionList.class);

    public class Environment {
        private final CoverageStatistic statistic = new CoverageStatistic(t);
        private final SAT4JSolutionSolver solver = initializeSolver(dependencyList);
        private final ModalImplicationGraph.Visitor visitor = mig.getVisitor();
        private final ExpandableIntegerList[] selectedIndexedSolutions = new ExpandableIntegerList[t];
        private final int[] literals = new int[t];
        private final Random random;

        public Environment(Combination<Environment> combination) {
            random = new Random(RANDOM_SEED.get(dependencyList) + combination.spliteratorId);
            solver.setSelectionStrategy(ISelectionStrategy.random(random));
        }

        public CoverageStatistic getStatistic() {
            return statistic;
        }
    }

    public TWiseCoverageComputation(IComputation<BooleanClauseList> booleanClauseList) {
        super(
                booleanClauseList, //
                Computations.of(2), //
                new MIGBuilder(booleanClauseList), //
                Computations.of(new BooleanSolutionList()));
    }

    public TWiseCoverageComputation(TWiseCoverageComputation other) {
        super(other);
    }

    private static final int GLOBAL_SOLUTION_LIMIT = 10_000;

    private ArrayList<ExpandableIntegerList> indexedSolutions;
    private ArrayList<ExpandableIntegerList> indexedRandomSolutions;
    private ArrayList<Environment> statisticList = new ArrayList<>();
    private int t;
    private int randomSolutionCount = 0;

    private ModalImplicationGraph mig;
    private List<Object> dependencyList;
    private BooleanSolutionList sample;

    @Override
    public Result<CoverageStatistic> compute(List<Object> dependencyList, Progress progress) {
        this.dependencyList = dependencyList;
        sample = SAMPLE.get(dependencyList);
        mig = MIG.get(dependencyList);
        t = T.get(dependencyList);

        if (!sample.isEmpty()) {
            final int size = sample.get(0).get().size();

            indexedSolutions = new ArrayList<>(2 * size);
            indexedRandomSolutions = new ArrayList<>(2 * size);
            for (int i = 2 * size; i >= 0; --i) {
                indexedSolutions.add(new ExpandableIntegerList());
                indexedRandomSolutions.add(new ExpandableIntegerList());
            }
            addConfigurations(sample, indexedSolutions);

            final int pow = (int) Math.pow(2, t);
            boolean[][] masks = new boolean[pow][t];
            for (int i = 0; i < masks.length; i++) {
                boolean[] p = masks[i];
                for (int j = 0; j < t; j++) {
                    p[j] = (i >> j & 1) == 0;
                }
            }
            LexicographicIterator.parallelStream(t, size, this::createStatistic).forEach(combo -> {
                final int[] elementIndices = combo.elementIndices;
                for (boolean[] mask : masks) {
                    checkCancel();
                    for (int k = 0; k < mask.length; k++) {
                        combo.environment.literals[k] = mask[k] ? (elementIndices[k] + 1) : -(elementIndices[k] + 1);
                    }
                    if (isCovered(combo.environment, indexedSolutions)) {
                        combo.environment.statistic.incNumberOfCoveredConditions();
                    } else if (isCombinationInvalidMIG(combo.environment)) {
                        combo.environment.statistic.incNumberOfInvalidConditions();
                    } else if (isCovered(combo.environment, indexedRandomSolutions)) {
                        combo.environment.statistic.incNumberOfUncoveredConditions();
                    } else if (isCombinationInvalidSAT(combo.environment)) {
                        combo.environment.statistic.incNumberOfInvalidConditions();
                    } else {
                        combo.environment.statistic.incNumberOfUncoveredConditions();
                    }
                }
            });
        }
        return Result.ofOptional(statisticList.stream() //
                .map(Environment::getStatistic) //
                .reduce((s1, s2) -> s1.merge(s2)));
    }

    private void addConfigurations(
            BooleanSolutionList sample, final ArrayList<ExpandableIntegerList> indexedSolutions) {
        int configurationIndex = 0;
        for (BooleanSolution configuration : sample) {
            for (int i = 0; i < configuration.size(); i++) {
                final int literal = configuration.get(i);
                if (literal != 0) {
                    indexedSolutions
                            .get(ModalImplicationGraph.getVertexIndex(literal))
                            .add(configurationIndex);
                }
            }
            configurationIndex++;
        }
    }

    private Environment createStatistic(Combination<Environment> combo) {
        Environment env = new Environment(combo);
        synchronized (statisticList) {
            statisticList.add(env);
        }
        return env;
    }

    private boolean isCovered(Environment env, ArrayList<ExpandableIntegerList> indexedSolutions) {
        if (t < 2) {
            return !indexedSolutions
                    .get(ModalImplicationGraph.getVertexIndex(env.literals[0]))
                    .isEmpty();
        }
        for (int i = 0; i < t; i++) {
            final ExpandableIntegerList indexedSolution =
                    indexedSolutions.get(ModalImplicationGraph.getVertexIndex(env.literals[i]));
            if (indexedSolution.size() == 0) {
                return false;
            }
            env.selectedIndexedSolutions[i] = indexedSolution;
        }
        Arrays.sort(env.selectedIndexedSolutions, (a, b) -> a.size() - b.size());
        final int[] ix = new int[t - 1];

        final ExpandableIntegerList i0 = env.selectedIndexedSolutions[0];
        final int[] ia0 = i0.toArray();
        loop:
        for (int i = 0; i < i0.size(); i++) {
            int id0 = ia0[i];
            for (int j = 1; j < t; j++) {
                final ExpandableIntegerList i1 = env.selectedIndexedSolutions[j];
                int binarySearch = Arrays.binarySearch(i1.toArray(), ix[j - 1], i1.size(), id0);
                if (binarySearch < 0) {
                    ix[j - 1] = -binarySearch - 1;
                    continue loop;
                } else {
                    ix[j - 1] = binarySearch;
                }
            }
            return true;
        }
        return false;
    }

    private boolean isCombinationInvalidMIG(Environment env) {
        try {
            env.visitor.propagate(env.literals);
        } catch (RuntimeContradictionException e) {
            return true;
        } finally {
            env.visitor.reset();
        }
        return false;
    }

    private boolean isCombinationInvalidSAT(Environment env) {
        final int orgAssignmentLength = env.solver.getAssignment().size();
        try {
            env.solver.getAssignment().addAll(env.literals);

            final Result<Boolean> hasSolution = env.solver.hasSolution();
            if (hasSolution.orElse(false)) {
                final boolean b;
                final int count;
                synchronized (indexedRandomSolutions) {
                    count = randomSolutionCount;
                    b = randomSolutionCount > GLOBAL_SOLUTION_LIMIT;
                    if (b) {
                        randomSolutionCount++;
                    }
                }
                if (b) {
                    int[] solution = env.solver.getInternalSolution();
                    synchronized (indexedRandomSolutions) {
                        for (int i = 0; i < solution.length; i++) {
                            ExpandableIntegerList indexList =
                                    indexedRandomSolutions.get(ModalImplicationGraph.getVertexIndex(solution[i]));
                            final int idIndex = Arrays.binarySearch(indexList.toArray(), 0, indexList.size(), count);
                            if (idIndex < 0) {
                                indexList.add(count, -(idIndex + 1));
                            }
                        }
                    }
                    env.solver.shuffleOrder(env.random);
                }
                return false;
            } else {
                return true;
            }
        } finally {
            env.solver.getAssignment().clear(orgAssignmentLength);
        }
    }

    @Override
    protected SAT4JSolver newSolver(BooleanClauseList clauseList) {
        return new SAT4JSolutionSolver(clauseList);
    }
}
