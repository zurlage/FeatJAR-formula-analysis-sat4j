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
import de.featjar.formula.analysis.GetSolutionAnalysis;
import de.featjar.formula.analysis.HasSolutionAnalysis;
import de.featjar.formula.analysis.bool.BooleanAssignment;
import de.featjar.formula.analysis.bool.BooleanClauseList;
import de.featjar.formula.analysis.bool.BooleanSolution;
import de.featjar.formula.analysis.bool.BooleanSolutionList;

public class SAT4JGetSolutionAnalysis extends SAT4JAnalysis.Solution<SAT4JGetSolutionAnalysis, BooleanSolution> implements
        GetSolutionAnalysis<BooleanClauseList, BooleanSolution, BooleanAssignment> {
    public SAT4JGetSolutionAnalysis(Computation<BooleanClauseList> clauseListComputation) {
        super(clauseListComputation);
    }

    @Override
    public FutureResult<BooleanSolution> compute() {
        return initializeSolver().thenComputeResult((solver, monitor) -> solver.findSolution());
    }
}
