/* -----------------------------------------------------------------------------
 * formula-analysis-sat4j - Analysis of propositional formulas using Sat4j
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
 * See <https://github.com/FeatJAR/formula-analysis-sat4j> for further information.
 * -----------------------------------------------------------------------------
 */
package org.spldev.analysis.sat4j;

import org.spldev.analysis.sat4j.solver.*;
import org.spldev.analysis.solver.*;
import org.spldev.analysis.solver.SatSolver.*;
import org.spldev.clauses.*;
import org.spldev.clauses.LiteralList.*;
import org.spldev.util.data.*;
import org.spldev.util.job.*;

/**
 * Attempts to count the number of possible solutions of a given {@link CNF}.
 *
 * @author Sebastian Krieter
 */
public class CountSolutionsAnalysis extends Sat4JAnalysis<Long> {

	public static final Identifier<Long> identifier = new Identifier<>();

	@Override
	public Identifier<Long> getIdentifier() {
		return identifier;
	}

	@Override
	public Long analyze(Sat4JSolver solver, InternalMonitor monitor) throws Exception {
		solver.setGlobalTimeout(true);
		long solutionCount = 0;
		SatResult hasSolution = solver.hasSolution();
		while (hasSolution == SatResult.TRUE) {
			solutionCount++;
			final int[] solution = solver.getInternalSolution();
			try {
				solver.getFormula().push(new LiteralList(solution, Order.INDEX, false).negate());
			} catch (final RuntimeContradictionException e) {
				break;
			}
			hasSolution = solver.hasSolution();
		}
		return hasSolution == SatResult.TIMEOUT ? -(solutionCount + 1) : solutionCount;
	}

}
