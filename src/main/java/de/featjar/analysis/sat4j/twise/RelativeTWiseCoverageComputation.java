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
import de.featjar.formula.assignment.BooleanAssignment;
import de.featjar.formula.assignment.BooleanSolution;
import de.featjar.formula.assignment.BooleanSolutionList;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Calculates statistics regarding t-wise feature coverage of a set of
 * solutions.
 *
 * @author Sebastian Krieter
 */
public class RelativeTWiseCoverageComputation extends AComputation<CoverageStatistic> {
    public static final Dependency<BooleanSolutionList> REFERENCE_SAMPLE =
            Dependency.newDependency(BooleanSolutionList.class);
    public static final Dependency<Integer> T = Dependency.newDependency(Integer.class);
    public static final Dependency<BooleanSolutionList> SAMPLE = Dependency.newDependency(BooleanSolutionList.class);
    public static final Dependency<BooleanAssignment> FILTER = Dependency.newDependency(BooleanAssignment.class);

    public class Environment {
        private final CoverageStatistic statistic = new CoverageStatistic(t);
        private final ExpandableIntegerList[] selectedIndexedSolutions = new ExpandableIntegerList[t];
        private final int[] literals = new int[t];

        public CoverageStatistic getStatistic() {
            return statistic;
        }
    }

    public RelativeTWiseCoverageComputation(IComputation<BooleanSolutionList> reference) {
        super(
                reference,
                Computations.of(2), //
                Computations.of(new BooleanSolutionList()), //
                Computations.of(new BooleanAssignment()));
    }

    public RelativeTWiseCoverageComputation(RelativeTWiseCoverageComputation other) {
        super(other);
    }

    private ArrayList<ExpandableIntegerList> indexedSolutions;
    private BitSet[] bitSetReference;
    private ArrayList<Environment> statisticList = new ArrayList<>();
    private int t;

    @Override
    public Result<CoverageStatistic> compute(List<Object> dependencyList, Progress progress) {
        BooleanSolutionList sample = SAMPLE.get(dependencyList);

        if (!sample.isEmpty()) {
            BooleanSolutionList referenceSample = REFERENCE_SAMPLE.get(dependencyList);
            assert referenceSample.isEmpty()
                    || sample.get(0).get().size()
                            >= referenceSample.get(0).get().size();

            t = T.get(dependencyList);
            final int size = sample.get(0).get().size();
            indexedSolutions = initIndexedLists(sample, size);
            bitSetReference = initBitSet(referenceSample, size);

            final int[] literals = TWiseCoverageComputationUtils.getFilteredLiterals(size, FILTER.get(dependencyList));
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
                            if (combinedIndex(size, combo.environment.literals, bitSetReference)
                                    .isEmpty()) {
                                combo.environment.statistic.incNumberOfInvalidConditions();
                            } else {
                                if (TWiseCoverageComputationUtils.isCovered(
                                        indexedSolutions,
                                        t,
                                        combo.environment.literals,
                                        combo.environment.selectedIndexedSolutions)) {
                                    combo.environment.statistic.incNumberOfCoveredConditions();
                                } else {
                                    combo.environment.statistic.incNumberOfUncoveredConditions();
                                }
                            }
                            int g = gray[i];
                            combo.environment.literals[g] = -combo.environment.literals[g];
                        }
                    });
        }
        return Result.ofOptional(statisticList.stream() //
                .map(Environment::getStatistic) //
                .reduce((s1, s2) -> s1.merge(s2)));
    }

    private BitSet combinedIndex(final int size, int[] literals, BitSet[] bitSets) {
        BitSet first = bitSets[literals[0] + size];
        BitSet bitSet = new BitSet(first.size());
        bitSet.xor(first);
        for (int k = 1; k < literals.length; k++) {
            bitSet.and(bitSets[literals[k] + size]);
        }
        return bitSet;
    }

    private ArrayList<ExpandableIntegerList> initIndexedLists(BooleanSolutionList sample, final int size) {
        ArrayList<ExpandableIntegerList> indexedSolutions = new ArrayList<>(2 * size);
        for (int i = 2 * size; i >= 0; --i) {
            indexedSolutions.add(new ExpandableIntegerList());
        }
        int configurationIndex = 0;
        for (BooleanSolution configuration : sample) {
            TWiseCoverageComputationUtils.addConfigurations(
                    indexedSolutions, configuration.get(), configurationIndex++);
        }
        return indexedSolutions;
    }

    private BitSet[] initBitSet(BooleanSolutionList sample, final int size) {
        final int indexedListSize = 2 * size;
        BitSet[] indexedSolutions = new BitSet[indexedListSize + 1];

        for (int j = 1; j <= size; j++) {
            BitSet negIndices = new BitSet(sample.size());
            BitSet posIndices = new BitSet(sample.size());
            for (int i = 0; i < sample.size(); i++) {
                BooleanSolution config = sample.getAll().get(i);
                if (config.get(j - 1) < 0) {
                    negIndices.set(i);
                } else {
                    posIndices.set(i);
                }
            }
            indexedSolutions[size - j] = negIndices;
            indexedSolutions[j + size] = posIndices;
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
