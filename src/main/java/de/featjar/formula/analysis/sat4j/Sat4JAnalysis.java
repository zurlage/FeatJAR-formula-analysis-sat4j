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
import de.featjar.formula.analysis.Analysis;
import de.featjar.formula.analysis.sat4j.solver.Sat4JSolver;
import de.featjar.formula.assignment.VariableAssignment;
import de.featjar.formula.clauses.CNF;

import java.util.Random;

/**
 * Base class for analyses using a {@link Sat4JSolver}.
 *
 * @param <T> the type of the analysis result.
 *
 * @author Sebastian Krieter
 */
public abstract class Sat4JAnalysis<T> extends Analysis<T, Sat4JSolver, CNF> {
    protected Sat4JAnalysis(Computation<CNF> inputComputation) {
        super(inputComputation, Sat4JSolver::new);
    }

    protected Sat4JAnalysis(Computation<CNF> inputComputation, VariableAssignment assumptions, long timeoutInMs, long randomSeed) {
        super(inputComputation, Sat4JSolver::new, assumptions, timeoutInMs, randomSeed);
    }
}
