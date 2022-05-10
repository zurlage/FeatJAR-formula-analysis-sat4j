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
package org.spldev.analysis.sat4j;

import org.spldev.analysis.solver.*;
import org.spldev.clauses.*;
import org.spldev.clauses.solutions.*;
import org.spldev.util.data.*;

/**
 * Generates all configurations for a given propositional formula.
 *
 * @author Sebastian Krieter
 */
public class AllConfigurationGenerator extends AbstractConfigurationGenerator {

	public static final Identifier<SolutionList> identifier = new Identifier<>();

	@Override
	protected Identifier<SolutionList> getIdentifier() {
		return identifier;
	}

	private boolean satisfiable = true;

	@Override
	public LiteralList get() {
		if (!satisfiable) {
			return null;
		}
		final LiteralList solution = solver.findSolution();
		if (solution == null) {
			satisfiable = false;
			return null;
		}
		try {
			solver.getFormula().push(solution.negate());
		} catch (final RuntimeContradictionException e) {
			satisfiable = false;
		}
		return solution;
	}

}
