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
package de.featjar.formula.configuration.list;

import de.featjar.formula.analysis.sat4j.solver.Sat4JSolutionSolver;
import de.featjar.formula.analysis.sat4j.twise.CoverageStatistic;
import de.featjar.formula.analysis.sat4j.twise.PresenceConditionManager;
import de.featjar.formula.analysis.sat4j.twise.TWiseConfigurationGenerator;
import de.featjar.formula.analysis.sat4j.twise.TWiseConfigurationUtil;
import de.featjar.formula.analysis.sat4j.twise.TWiseConfigurationUtil.InvalidClausesList;
import de.featjar.formula.analysis.sat4j.twise.TWiseStatisticGenerator;
import de.featjar.formula.analysis.bool.BooleanAssignmentList;
import de.featjar.formula.analysis.metrics.SampleMetric;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests whether a set of configurations achieves t-wise feature coverage.
 *
 * @author Sebastian Krieter
 */
public class TWiseCoverageMetrics {

    public class TWiseCoverageMetric implements SampleMetric {
        private final int t;
        private boolean firstUse = true;

        public TWiseCoverageMetric(int t) {
            this.t = t;
        }

        @Override
        public double get(DNF sample) {
            final TWiseStatisticGenerator tWiseStatisticGenerator = new TWiseStatisticGenerator(util);
            if (firstUse) {
                firstUse = false;
            } else {
                util.setInvalidClausesList(InvalidClausesList.Use);
            }

            final CoverageStatistic statistic = tWiseStatisticGenerator
                    .getCoverage(
                            Arrays.asList(sample.getSolutionList()), //
                            presenceConditionManager.getGroupedPresenceConditions(), //
                            t, //
                            TWiseStatisticGenerator.ConfigurationScore.NONE, //
                            true)
                    .get(0);

            final long numberOfValidConditions = statistic.getNumberOfValidConditions();
            final long numberOfCoveredConditions = statistic.getNumberOfCoveredConditions();
            if (numberOfValidConditions == 0) {
                return 1;
            } else {
                return (double) numberOfCoveredConditions / numberOfValidConditions;
            }
        }

        @Override
        public String getName() {
            return "T" + t + "_" + name + "_" + "Coverage";
        }
    }

    private TWiseConfigurationUtil util;
    private PresenceConditionManager presenceConditionManager;
    private String name;
    private CNF cnf;
    private List<List<BooleanAssignmentList>> expressions;

    public void setCNF(CNF cnf) {
        this.cnf = cnf;
    }

    public void setExpressions(List<List<BooleanAssignmentList>> expressions) {
        this.expressions = expressions;
    }

    public void init() {
        if (!cnf.getClauseList().isEmpty()) {
            util = new TWiseConfigurationUtil(cnf, new Sat4JSolutionSolver(cnf));
        } else {
            util = new TWiseConfigurationUtil(cnf, null);
        }
        util.setInvalidClausesList(InvalidClausesList.Create);
        util.computeRandomSample(1000);
        if (!cnf.getClauseList().isEmpty()) {
            util.computeMIG(false, false);
        }
        if (expressions == null) {
            expressions = TWiseConfigurationGenerator.convertLiterals(Deprecated.getLiterals(cnf.getVariableMap()));
        }
        presenceConditionManager = new PresenceConditionManager(util, expressions);
    }

    public TWiseCoverageMetric getTWiseCoverageMetric(int t) {
        return new TWiseCoverageMetric(t);
    }

    public static List<TWiseCoverageMetric> getTWiseCoverageMetrics(
            CNF cnf, List<List<BooleanAssignmentList>> expressions, String name, int... tValues) {
        final TWiseCoverageMetrics metrics = new TWiseCoverageMetrics();
        metrics.setName(name);
        metrics.setExpressions(expressions);
        metrics.setCNF(cnf);
        if (cnf != null) {
            metrics.init();
        }
        final List<TWiseCoverageMetric> coverageMetrics = new ArrayList<>(tValues.length);
        for (final int t : tValues) {
            coverageMetrics.add(metrics.getTWiseCoverageMetric(t));
        }
        return coverageMetrics;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
