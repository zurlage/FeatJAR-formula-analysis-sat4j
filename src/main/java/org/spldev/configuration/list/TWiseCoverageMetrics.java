/* -----------------------------------------------------------------------------
 * Formula-Analysis-Sat4J Lib - Library to analyze propositional formulas with Sat4J.
 * Copyright (C) 2021-2022  Sebastian Krieter
 * 
 * This file is part of Formula-Analysis-Sat4J Lib.
 * 
 * Formula-Analysis-Sat4J Lib is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 * 
 * Formula-Analysis-Sat4J Lib is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Formula-Analysis-Sat4J Lib.  If not, see <https://www.gnu.org/licenses/>.
 * 
 * See <https://github.com/skrieter/formula-analysis-sat4j> for further information.
 * -----------------------------------------------------------------------------
 */
package org.spldev.configuration.list;

import java.util.*;

import org.spldev.analysis.sat4j.solver.*;
import org.spldev.analysis.sat4j.twise.*;
import org.spldev.analysis.sat4j.twise.TWiseConfigurationUtil.*;
import org.spldev.clauses.*;
import org.spldev.clauses.solutions.*;
import org.spldev.clauses.solutions.metrics.*;

/**
 * Tests whether a set of configurations achieves t-wise feature coverage.
 *
 * @author Sebastian Krieter
 */
public class TWiseCoverageMetrics {

	public final class TWiseCoverageMetric implements SampleMetric {
		private final int t;
		private boolean firstUse = true;

		public TWiseCoverageMetric(int t) {
			this.t = t;
		}

		@Override
		public double get(SolutionList sample) {
			final TWiseStatisticGenerator tWiseStatisticGenerator = new TWiseStatisticGenerator(util);
			if (firstUse) {
				firstUse = false;
			} else {
				util.setInvalidClausesList(InvalidClausesList.Use);
			}

			final CoverageStatistic statistic = tWiseStatisticGenerator
				.getCoverage(Arrays.asList(sample.getSolutions()), //
					presenceConditionManager.getGroupedPresenceConditions(), //
					t, //
					TWiseStatisticGenerator.ConfigurationScore.NONE, //
					true).get(0);

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
	private List<List<ClauseList>> expressions;

	public void setCNF(CNF cnf) {
		this.cnf = cnf;
	}

	public void setExpressions(List<List<ClauseList>> expressions) {
		this.expressions = expressions;
	}

	public void init() {
		if (!cnf.getClauses().isEmpty()) {
			util = new TWiseConfigurationUtil(cnf, new Sat4JSolver(cnf));
		} else {
			util = new TWiseConfigurationUtil(cnf, null);
		}
		util.setInvalidClausesList(InvalidClausesList.Create);
		util.computeRandomSample(1000);
		if (!cnf.getClauses().isEmpty()) {
			util.computeMIG(false, false);
		}
		if (expressions == null) {
			expressions = TWiseConfigurationGenerator.convertLiterals(Clauses.getLiterals(cnf.getVariables()));
		}
		presenceConditionManager = new PresenceConditionManager(util, expressions);
	}

	public TWiseCoverageMetric getTWiseCoverageMetric(int t) {
		return new TWiseCoverageMetric(t);
	}

	public static List<TWiseCoverageMetric> getTWiseCoverageMetrics(CNF cnf, List<List<ClauseList>> expressions,
		String name,
		int... tValues) {
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
