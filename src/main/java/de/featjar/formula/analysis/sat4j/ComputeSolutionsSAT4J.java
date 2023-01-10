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

import de.featjar.base.computation.DependencyList;
import de.featjar.base.computation.IComputation;
import de.featjar.base.data.Result;
import de.featjar.base.computation.Progress;
import de.featjar.base.tree.structure.ITree;
import de.featjar.formula.analysis.ISolutionsAnalysis;
import de.featjar.formula.analysis.bool.BooleanAssignment;
import de.featjar.formula.analysis.bool.BooleanClauseList;
import de.featjar.formula.analysis.bool.BooleanSolution;
import de.featjar.formula.analysis.bool.BooleanSolutionList;
import de.featjar.formula.analysis.sat4j.solver.SAT4JSolver;

public class ComputeSolutionsSAT4J extends ASAT4JAnalysis.Solution<BooleanSolutionList> implements
        ISolutionsAnalysis<BooleanClauseList, BooleanSolutionList, BooleanAssignment> {
    public ComputeSolutionsSAT4J(IComputation<BooleanClauseList> booleanClauseList) {
        super(booleanClauseList);
    }

    @Override
    public Result<BooleanSolutionList> compute(DependencyList dependencyList, Progress progress) {
        SAT4JSolver solver = initializeSolver(dependencyList);
        BooleanSolutionList solutionList = new BooleanSolutionList();
        Result<Boolean> hasSolution = solver.hasSolution();
        while (hasSolution.equals(Result.of(true))) {
            BooleanSolution solution = solver.getSolutionHistory().getLastSolution().get();
            solutionList.add(solution);
            solver.getClauseList().add(solution.toClause().negate());
            hasSolution = solver.hasSolution();
        }
        return solver.createResult(solutionList, "result is a subset");
    }

    @Override
    public ITree<IComputation<?>> cloneNode() {
        return new ComputeSolutionsSAT4J(getInput());
    }
}
