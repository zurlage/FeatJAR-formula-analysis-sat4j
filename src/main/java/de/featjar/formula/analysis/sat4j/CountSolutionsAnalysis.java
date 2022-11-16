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

import de.featjar.base.data.Computation;
import de.featjar.base.data.FutureResult;
import de.featjar.base.data.Result;

/**
 * Attempts to count the number of possible solutions of a given {@link CNF}.
 *
 * @author Sebastian Krieter
 */
public class CountSolutionsAnalysis extends Sat4JAnalysis<Long> {

    public CountSolutionsAnalysis(Computation<CNF> inputComputation) {
        super(inputComputation);
    }

    public CountSolutionsAnalysis(Computation<CNF> inputComputation, Assignment assumptions, long timeoutInMs, long randomSeed) {
        super(inputComputation, assumptions, timeoutInMs, randomSeed);
    }

    @Override
    public FutureResult<Long> compute() {
        return initializeSolver().thenCompute(((solver, monitor) -> {
            solver.setGlobalTimeout(true);
            long solutionCount = 0;
            Result<Boolean> hasSolution = solver.hasSolution();
            while (hasSolution.equals(Result.of(true))) {
                solutionCount++;
                final int[] solution = solver.getInternalSolution();
                try {
                    solver.getSolverFormula().push(new SortedIntegerList(solution, SortedIntegerList.Order.INDEX, false).negate());
                } catch (final SolverContradictionException e) {
                    break;
                }
                hasSolution = solver.hasSolution();
            }
            return hasSolution.isEmpty() ? -(solutionCount + 1) : solutionCount;
        }));
    }
}
