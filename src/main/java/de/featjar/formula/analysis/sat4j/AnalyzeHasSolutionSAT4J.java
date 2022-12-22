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
import de.featjar.base.tree.structure.Traversable;
import de.featjar.formula.analysis.HasSolutionAnalysis;
import de.featjar.formula.analysis.bool.BooleanAssignment;
import de.featjar.formula.analysis.bool.BooleanClauseList;

public class AnalyzeHasSolutionSAT4J extends ASAT4JAnalysis.Solution<Boolean> implements
        HasSolutionAnalysis<BooleanClauseList, BooleanAssignment> {
    public AnalyzeHasSolutionSAT4J(IComputation<BooleanClauseList> booleanClauseList) {
        super(booleanClauseList);
    }

    @Override
    public FutureResult<Boolean> compute() {
        return computeSolver().get().thenComputeResult((solver, monitor) -> solver.hasSolution());
    }

    @Override
    public Traversable<IComputation<?>> cloneNode() {
        return new AnalyzeHasSolutionSAT4J(getInput());
    }
}
