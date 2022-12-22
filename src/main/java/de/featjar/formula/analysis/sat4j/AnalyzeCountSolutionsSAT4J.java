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

import de.featjar.base.computation.IComputation;
import de.featjar.base.computation.FutureResult;
import de.featjar.base.data.Result;
import de.featjar.base.tree.structure.ITree;
import de.featjar.formula.analysis.ICountSolutionsAnalysis;
import de.featjar.formula.analysis.bool.BooleanAssignment;
import de.featjar.formula.analysis.bool.BooleanClauseList;
import de.featjar.formula.analysis.bool.BooleanSolution;

import java.math.BigInteger;

public class AnalyzeCountSolutionsSAT4J extends ASAT4JAnalysis.Solution<BigInteger> implements
        ICountSolutionsAnalysis<BooleanClauseList, BooleanAssignment> {
    public AnalyzeCountSolutionsSAT4J(IComputation<BooleanClauseList> booleanClauseList) {
        super(booleanClauseList);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Override
    public FutureResult<BigInteger> compute() {
        return computeSolver().get().thenComputeResult((solver, monitor) -> {
            BigInteger solutionCount = BigInteger.ZERO;
            Result<Boolean> hasSolution = solver.hasSolution();
            while (hasSolution.equals(Result.of(true))) {
                solutionCount = solutionCount.add(BigInteger.ONE);
                BooleanSolution solution = solver.getSolutionHistory().getLastSolution().get();
                solver.getClauseList().add(solution.toClause().negate());
                hasSolution = solver.hasSolution();
            }
            BigInteger finalSolutionCount = solutionCount;
            // TODO: if timeout is reached, return lower bound with a warning
            return hasSolution.map(_hasSolution -> finalSolutionCount);
        });
    }

    @Override
    public ITree<IComputation<?>> cloneNode() {
        return new AnalyzeCountSolutionsSAT4J(getInput());
    }
}
