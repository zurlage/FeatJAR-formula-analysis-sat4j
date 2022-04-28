/* -----------------------------------------------------------------------------
 * Formula-Analysis-Sat4J Lib - Library to analyze propositional formulas with Sat4J.
 * Copyright (C) 2021  Sebastian Krieter
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
package org.spldev.analysis.sat4j.solver;

import java.util.*;
import java.util.stream.*;

import org.sat4j.minisat.*;
import org.sat4j.specs.*;
import org.sat4j.tools.xplain.*;
import org.spldev.analysis.solver.*;
import org.spldev.clauses.*;
import org.spldev.formula.*;

/**
 * Implements a {@link MusSolver} using Sat4J.
 *
 * <br>
 * <br>
 * Sat4J only support the extraction of one minimal unsatisfiable subset, thus
 * {@link #getAllMinimalUnsatisfiableSubsets()} only returns one solution.
 *
 * <br>
 * <br>
 * Note: The usage of a solver to solve expression and to find minimal
 * unsatisfiable subset should be divided into two task because the native
 * solver for the MUS extractor are substantially slower in solving
 * satisfiability requests. If for solving the usage of the {@link Sat4JSolver}
 * is recommended.
 *
 * @author Joshua Sprey
 * @author Sebastian Krieter
 */
public class Sat4JMusSolver extends AbstractSat4JSolver<Xplain<ISolver>> implements MusSolver<IConstr> {

	public Sat4JMusSolver(ModelRepresentation modelRepresentation) {
		this(modelRepresentation.getCache().get(CNFProvider.fromFormula()).get());
	}

	public Sat4JMusSolver(CNF cnf) {
		super(cnf);
	}

	@Override
	protected Xplain<ISolver> createSolver() {
		return new Xplain<>(SolverFactory.newDefault());
	}

	@Override
	public List<IConstr> getMinimalUnsatisfiableSubset() throws IllegalStateException {
		if (hasSolution() == SatResult.TRUE) {
			throw new IllegalStateException("Problem is satisfiable");
		}
		try {
			return IntStream.of(solver.minimalExplanation()) //
				.mapToObj(getFormula().getConstraints()::get) //
				.collect(Collectors.toList());
		} catch (final TimeoutException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public List<List<IConstr>> getAllMinimalUnsatisfiableSubsets() throws IllegalStateException {
		return Collections.singletonList(getMinimalUnsatisfiableSubset());
	}

}
