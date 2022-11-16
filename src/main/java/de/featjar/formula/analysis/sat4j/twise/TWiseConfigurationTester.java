/*
 * Copyright (C) 2022 Sebastian Krieter
 *
 * This file is part of formula-analysis-sat4j.
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

import de.featjar.formula.analysis.sat4j.solver.Sat4JSolutionSolver;
import de.featjar.formula.analysis.sat4j.twise.TWiseStatisticGenerator.ConfigurationScore;
import de.featjar.formula.analysis.bool.BooleanAssignmentList;
import de.featjar.formula.analysis.combinations.CombinationIterator;
import de.featjar.formula.analysis.combinations.LexicographicIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests whether a set of configurations achieves t-wise feature coverage.
 *
 * @author Sebastian Krieter
 */
public class TWiseConfigurationTester {

    private final TWiseConfigurationUtil util;

    private List<SortedIntegerList> sample;
    private PresenceConditionManager presenceConditionManager;
    private int t;

    public TWiseConfigurationTester(CNF cnf) {
        if (!cnf.getClauseList().isEmpty()) {
            util = new TWiseConfigurationUtil(cnf, new Sat4JSolutionSolver(cnf));
        } else {
            util = new TWiseConfigurationUtil(cnf, null);
        }

        getUtil().computeRandomSample(TWiseConfigurationGenerator.DEFAULT_RANDOM_SAMPLE_SIZE);
        if (!cnf.getClauseList().isEmpty()) {
            getUtil().computeMIG(false, false);
        }
    }

    public void setNodes(List<List<BooleanAssignmentList>> expressions) {
        presenceConditionManager = new PresenceConditionManager(getUtil(), expressions);
    }

    public void setNodes(PresenceConditionManager expressions) {
        presenceConditionManager = expressions;
    }

    public void setT(int t) {
        this.t = t;
    }

    public void setSample(List<SortedIntegerList> sample) {
        this.sample = sample;
    }

    public List<SortedIntegerList> getSample() {
        return sample;
    }

    /**
     * Creates statistic values about covered combinations.<br>
     * To get a percentage value of covered combinations use:<br>
     *
     * <pre>
     * {
     * 	&#64;code
     * 	CoverageStatistic coverage = getCoverage();
     * 	double covered = (double) coverage.getNumberOfCoveredConditions() / coverage.getNumberOfValidConditions();
     * }
     * </pre>
     *
     *
     * @return a statistic object containing multiple values:<br>
     *         <ul>
     *         <li>number of valid combinations
     *         <li>number of invalid combinations
     *         <li>number of covered combinations
     *         <li>number of uncovered combinations
     *         <li>value of each configuration
     *         </ul>
     */
    public CoverageStatistic getCoverage() {
        final List<CoverageStatistic> coveragePerSample = new TWiseStatisticGenerator(util)
                .getCoverage(
                        Arrays.asList(sample),
                        presenceConditionManager.getGroupedPresenceConditions(),
                        t,
                        ConfigurationScore.NONE,
                        true);
        return coveragePerSample.get(0);
    }

    public ValidityStatistic getValidity() {
        final List<ValidityStatistic> validityPerSample =
                new TWiseStatisticGenerator(util).getValidity(Arrays.asList(sample));
        return validityPerSample.get(0);
    }

    public boolean hasUncoveredConditions() {
        final List<BooleanAssignmentList> uncoveredConditions = getUncoveredConditions(true);
        return !uncoveredConditions.isEmpty();
    }

    public BooleanAssignmentList getFirstUncoveredCondition() {
        final List<BooleanAssignmentList> uncoveredConditions = getUncoveredConditions(true);
        return uncoveredConditions.isEmpty() ? null : uncoveredConditions.get(0);
    }

    public List<BooleanAssignmentList> getUncoveredConditions() {
        return getUncoveredConditions(false);
    }

    private List<BooleanAssignmentList> getUncoveredConditions(boolean cancelAfterFirst) {
        final ArrayList<BooleanAssignmentList> uncoveredConditions = new ArrayList<>();
        final TWiseCombiner combiner =
                new TWiseCombiner(getUtil().getCnf().getVariableMap().getVariableCount());
        BooleanAssignmentList combinedCondition = new BooleanAssignmentList();
        final PresenceCondition[] clauseListArray = new PresenceCondition[t];

        groupLoop:
        for (final List<PresenceCondition> expressions : presenceConditionManager.getGroupedPresenceConditions()) {
            for (final CombinationIterator iterator = new LexicographicIterator(t, expressions.size());
                    iterator.hasNext(); ) {
                final int[] next = iterator.next();
                if (next == null) {
                    break;
                }
                CombinationIterator.select(expressions, next, clauseListArray);

                combinedCondition.clear();
                combiner.combineConditions(clauseListArray, combinedCondition);
                if (!TWiseConfigurationUtil.isCovered(combinedCondition, sample)
                        && getUtil().isCombinationValid(combinedCondition)) {
                    uncoveredConditions.add(combinedCondition);
                    combinedCondition = new BooleanAssignmentList();
                    if (cancelAfterFirst) {
                        break groupLoop;
                    }
                }
            }
        }
        return uncoveredConditions;
    }

    public boolean hasInvalidSolutions() {
        final List<SortedIntegerList> invalidSolutions = getInvalidSolutions(true);
        return !invalidSolutions.isEmpty();
    }

    public SortedIntegerList getFirstInvalidSolution() {
        final List<SortedIntegerList> invalidSolutions = getInvalidSolutions(true);
        return invalidSolutions.isEmpty() ? null : invalidSolutions.get(0);
    }

    public List<SortedIntegerList> getInvalidSolutions() {
        return getInvalidSolutions(false);
    }

    private List<SortedIntegerList> getInvalidSolutions(boolean cancelAfterFirst) {
        final ArrayList<SortedIntegerList> invalidSolutions = new ArrayList<>();
        configLoop:
        for (final SortedIntegerList solution : sample) {
            for (final SortedIntegerList sortedIntegerList : getUtil().getCnf().getClauseList()) {
                if (solution.isDisjoint(sortedIntegerList)) {
                    invalidSolutions.add(solution);
                    if (cancelAfterFirst) {
                        break configLoop;
                    }
                }
            }
        }
        return invalidSolutions;
    }

    public TWiseConfigurationUtil getUtil() {
        return util;
    }
}
