/*
 * Copyright (C) 2024 FeatJAR-Development-Team
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
package de.featjar.analysis.sat4j.twise;

import de.featjar.base.computation.AComputation;
import de.featjar.base.computation.Computations;
import de.featjar.base.computation.Dependency;
import de.featjar.base.computation.IComputation;
import de.featjar.base.computation.Progress;
import de.featjar.base.data.ExpandableIntegerList;
import de.featjar.base.data.LexicographicIterator;
import de.featjar.base.data.LexicographicIterator.Combination;
import de.featjar.base.data.Result;
import de.featjar.formula.assignment.ABooleanAssignment;
import de.featjar.formula.assignment.ABooleanAssignmentList;
import de.featjar.formula.assignment.BooleanAssignment;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Calculates statistics regarding t-wise feature coverage of a set of
 * solutions.
 *
 * @author Sebastian Krieter
 */
public class TWiseCountComputation extends AComputation<Long> {

    public static class CombinationList {
        private List<int[]> set;

        private CombinationList(List<int[]> set) {
            this.set = set;
        }

        public static CombinationList of(List<int[]> set) {
            return new CombinationList(set);
        }
    }

    @SuppressWarnings("rawtypes")
    public static final Dependency<ABooleanAssignmentList> SAMPLE =
            Dependency.newDependency(ABooleanAssignmentList.class);

    public static final Dependency<Integer> T = Dependency.newDependency(Integer.class);
    public static final Dependency<BooleanAssignment> VARIABLE_FILTER =
            Dependency.newDependency(BooleanAssignment.class);
    public static final Dependency<CombinationList> COMBINATION_FILTER =
            Dependency.newDependency(CombinationList.class);

    public class Environment {
        private long statistic = 0;
        private final CoverageChecker coverageChecker = new CoverageChecker(indexedSolutions, t);
        private final int[] literals = new int[t];

        public long getStatistic() {
            return statistic;
        }
    }

    public TWiseCountComputation(@SuppressWarnings("rawtypes") IComputation<? extends ABooleanAssignmentList> sample) {
        super(
                sample,
                Computations.of(2), //
                Computations.of(new BooleanAssignment()), //
                Computations.of(new CombinationList(List.of())));
    }

    public TWiseCountComputation(TWiseCountComputation other) {
        super(other);
    }

    private ArrayList<ExpandableIntegerList> indexedSolutions;
    private ArrayList<Environment> statisticList = new ArrayList<>();
    private int t;

    @Override
    public Result<Long> compute(List<Object> dependencyList, Progress progress) {
        @SuppressWarnings("unchecked")
        List<? extends ABooleanAssignment> sample = SAMPLE.get(dependencyList).getAll();

        if (sample.isEmpty()) {
            return Result.of(0L);
        }

        List<int[]> filterCombinations = COMBINATION_FILTER.get(dependencyList).set;

        final int size = sample.get(0).size();
        indexedSolutions = initIndexedLists(sample, size);

        t = T.get(dependencyList);

        final int[] literals =
                TWiseCoverageComputationUtils.getFilteredLiterals(size, VARIABLE_FILTER.get(dependencyList));
        final int[] gray = IntStream.rangeClosed(1, 1 << t)
                .map(Integer::numberOfTrailingZeros)
                .toArray();
        gray[gray.length - 1] = 0;

        LexicographicIterator.parallelStream(t, literals.length, this::createStatistic)
                .forEach(combo -> {
                    for (int k = 0; k < t; k++) {
                        combo.environment.literals[k] = literals[combo.elementIndices[k]];
                    }
                    for (int i = 0; i < gray.length; i++) {
                        if (combo.environment.coverageChecker.test(combo.environment.literals)) {
                            combo.environment.statistic++;
                        }
                        int g = gray[i];
                        combo.environment.literals[g] = -combo.environment.literals[g];
                    }
                });

        CoverageChecker coverageChecker = new CoverageChecker(indexedSolutions, t);
        long filterCombinationsCount =
                filterCombinations.stream().filter(coverageChecker).count();
        return Result.ofOptional(statisticList.stream() //
                .map(Environment::getStatistic) //
                .reduce((s1, s2) -> s1 + s2)
                .map(s -> s - filterCombinationsCount));
    }

    private ArrayList<ExpandableIntegerList> initIndexedLists(List<? extends ABooleanAssignment> list, final int size) {
        ArrayList<ExpandableIntegerList> indexedSolutions = new ArrayList<>(2 * size);
        for (int i = 2 * size; i >= 0; --i) {
            indexedSolutions.add(new ExpandableIntegerList());
        }
        int configurationIndex = 0;
        for (ABooleanAssignment configuration : list) {
            TWiseCoverageComputationUtils.addConfigurations(
                    indexedSolutions, configuration.get(), configurationIndex++);
        }
        return indexedSolutions;
    }

    private Environment createStatistic(Combination<Environment> combo) {
        Environment env = new Environment();
        synchronized (statisticList) {
            statisticList.add(env);
        }
        return env;
    }
}
