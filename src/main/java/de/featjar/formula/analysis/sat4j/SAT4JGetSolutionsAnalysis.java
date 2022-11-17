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

import de.featjar.base.data.FutureResult;
import de.featjar.base.data.Result;
import de.featjar.formula.analysis.AssignmentList;
import de.featjar.formula.analysis.CountSolutionsAnalysis;
import de.featjar.formula.analysis.GetSolutionsAnalysis;
import de.featjar.formula.analysis.Solution;
import de.featjar.formula.analysis.bool.BooleanAssignment;
import de.featjar.formula.analysis.bool.BooleanClauseList;
import de.featjar.formula.analysis.bool.BooleanSolution;
import de.featjar.formula.analysis.bool.BooleanSolutionList;

import java.math.BigInteger;

public class SAT4JGetSolutionsAnalysis extends SAT4JAnalysis.Solution<SAT4JGetSolutionsAnalysis, BooleanSolutionList> implements
        GetSolutionsAnalysis<BooleanClauseList, BooleanSolutionList, BooleanAssignment> {
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Override
    public FutureResult<BooleanSolutionList> compute() {
        return initializeSolver().thenComputeResult((solver, monitor) -> {
            BooleanSolutionList solutionList = new BooleanSolutionList();
            Result<Boolean> hasSolution = solver.hasSolution();
            while (hasSolution.equals(Result.of(true))) {
                BooleanSolution solution = solver.getSolutionHistory().getLastSolution().get();
                solutionList.add(solution);
                solver.getClauseList().add(solution.toClause().negate());
                hasSolution = solver.hasSolution();
            }
            // todo: if timeout is reached, return subset with a warning
            return hasSolution.map(_hasSolution -> solutionList);
        });
    }
}
