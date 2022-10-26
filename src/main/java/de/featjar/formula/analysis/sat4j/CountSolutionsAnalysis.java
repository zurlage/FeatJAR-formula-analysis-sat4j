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
package de.featjar.formula.analysis.sat4j;

import de.featjar.formula.analysis.sat4j.solver.Sat4JSolver;
import de.featjar.formula.analysis.solver.SolverContradictionException;
import de.featjar.formula.analysis.solver.SATSolver;
import de.featjar.formula.clauses.CNF;
import de.featjar.formula.clauses.LiteralList;
import de.featjar.base.task.Monitor;

/**
 * Attempts to count the number of possible solutions of a given {@link CNF}.
 *
 * @author Sebastian Krieter
 */
public class CountSolutionsAnalysis extends Sat4JAnalysis<Long> {

    @Override
    public Long analyze(Sat4JSolver solver, Monitor monitor) throws Exception {
        solver.setGlobalTimeout(true);
        long solutionCount = 0;
        SATSolver.SATResult hasSolution = solver.hasSolution();
        while (hasSolution == SATSolver.SATResult.TRUE) {
            solutionCount++;
            final int[] solution = solver.getInternalSolution();
            try {
                solver.getFormula().push(new LiteralList(solution, LiteralList.Order.INDEX, false).negate());
            } catch (final SolverContradictionException e) {
                break;
            }
            hasSolution = solver.hasSolution();
        }
        return hasSolution == SATSolver.SATResult.TIMEOUT ? -(solutionCount + 1) : solutionCount;
    }
}
