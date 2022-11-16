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

/**
 * Determines whether a given {@link CNF} is satisfiable and returns the found
 * solution.
 *
 * @author Sebastian Krieter
 */
public class HasSolutionAnalysis extends Sat4JAnalysis<Boolean> {
    public HasSolutionAnalysis(Computation<CNF> inputComputation) {
        super(inputComputation);
    }

    public HasSolutionAnalysis(Computation<CNF> inputComputation, Assignment assumptions, long timeoutInMs, long randomSeed) {
        super(inputComputation, assumptions, timeoutInMs, randomSeed);
    }

    @Override
    public FutureResult<Boolean> compute() {
        return initializeSolver().thenComputeResult(((solver, monitor) -> {
            // todo: log output
            return solver.hasSolution();
        }));
    }
}
