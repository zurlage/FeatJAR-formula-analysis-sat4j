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

import de.featjar.base.computation.AComputation;
import de.featjar.base.computation.Computations;
import de.featjar.base.computation.Dependency;
import de.featjar.base.computation.IComputation;
import de.featjar.base.computation.Progress;
import de.featjar.base.data.ExpandableIntegerList;
import de.featjar.base.data.Result;
import de.featjar.formula.analysis.bool.BooleanClauseList;
import de.featjar.formula.analysis.bool.BooleanSolution;
import de.featjar.formula.analysis.bool.BooleanSolutionList;
import de.featjar.formula.analysis.combinations.LexicographicIterator;
import de.featjar.formula.analysis.combinations.LexicographicIterator.Combination;
import de.featjar.formula.analysis.mig.solver.ModalImplicationGraph;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Calculates statistics regarding t-wise feature coverage of a set of
 * solutions.
 *
 * @author Sebastian Krieter
 */
public class RelativeTWiseCoverageComputation extends AComputation<CoverageStatistic> {
    public static final Dependency<Integer> T = Dependency.newDependency(Integer.class);
    public static final Dependency<BooleanSolutionList> SAMPLE = Dependency.newDependency(BooleanSolutionList.class);
    public static final Dependency<BooleanSolutionList> REFERENCE_SAMPLE =
            Dependency.newDependency(BooleanSolutionList.class);

    public class Environment {
        private final CoverageStatistic statistic = new CoverageStatistic(t);
        private final ExpandableIntegerList[] selectedIndexedSolutions = new ExpandableIntegerList[t];
        private final int[] literals = new int[t];

        public CoverageStatistic getStatistic() {
            return statistic;
        }
    }

    public RelativeTWiseCoverageComputation(IComputation<BooleanClauseList> booleanClauseList) {
        super(
                Computations.of(2), //
                Computations.of(new BooleanSolutionList()), //
                Computations.of(new BooleanSolutionList()));
    }

    public RelativeTWiseCoverageComputation(RelativeTWiseCoverageComputation other) {
        super(other);
    }

    private ArrayList<ExpandableIntegerList> indexedSolutions;
    private ArrayList<ExpandableIntegerList> indexedReferenceSolutions;
    private ArrayList<Environment> statisticList = new ArrayList<>();
    private int t;

    @Override
    public Result<CoverageStatistic> compute(List<Object> dependencyList, Progress progress) {
        BooleanSolutionList sample = SAMPLE.get(dependencyList);
        BooleanSolutionList referenceSample = REFERENCE_SAMPLE.get(dependencyList);
        t = T.get(dependencyList);

        if (!sample.isEmpty()) {
            assert referenceSample.isEmpty()
                    || sample.get(0).get().size()
                            == referenceSample.get(0).get().size();
            final int size = sample.get(0).get().size();

            indexedSolutions = new ArrayList<>(2 * size);
            indexedReferenceSolutions = new ArrayList<>(2 * size);
            for (int i = 2 * size; i >= 0; --i) {
                indexedSolutions.add(new ExpandableIntegerList());
                indexedReferenceSolutions.add(new ExpandableIntegerList());
            }
            addConfigurations(sample, indexedSolutions);
            addConfigurations(referenceSample, indexedReferenceSolutions);

            final int pow = (int) Math.pow(2, t);
            boolean[][] masks = new boolean[pow][t];
            for (int i = 0; i < masks.length; i++) {
                boolean[] p = masks[i];
                for (int j = 0; j < t; j++) {
                    p[j] = (i >> j & 1) == 0;
                }
            }
            LexicographicIterator.stream(t, size, this::createStatistic).forEach(combo -> {
                final int[] elementIndices = combo.elementIndices;
                for (boolean[] mask : masks) {
                    checkCancel();
                    for (int k = 0; k < mask.length; k++) {
                        combo.environment.literals[k] = mask[k] ? (elementIndices[k] + 1) : -(elementIndices[k] + 1);
                    }
                    if (isCovered(combo.environment, indexedReferenceSolutions)) {
                        if (isCovered(combo.environment, indexedSolutions)) {
                            combo.environment.statistic.incNumberOfCoveredConditions();
                        } else {
                            combo.environment.statistic.incNumberOfUncoveredConditions();
                        }
                    } else {
                        combo.environment.statistic.incNumberOfInvalidConditions();
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
        Environment env = new Environment();
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
}
