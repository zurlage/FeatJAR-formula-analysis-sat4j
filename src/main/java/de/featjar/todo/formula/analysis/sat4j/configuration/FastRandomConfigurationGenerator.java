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
package de.featjar.todo.formula.analysis.sat4j.configuration;

import de.featjar.formula.analysis.sat4j.solver.ISelectionStrategy;
import de.featjar.formula.analysis.sat4j.solver.SAT4JSolutionSolver;

/**
 * Generates random configurations for a given propositional formula.
 *
 * @author Sebastian Krieter
 */
public class FastRandomConfigurationGenerator extends RandomConfigurationGenerator {

    private ISelectionStrategy originalSelectionStrategy;

    @Override
    protected void prepareSolver(SAT4JSolutionSolver solver) {
        super.prepareSolver(solver);
        originalSelectionStrategy = solver.getSelectionStrategy();
        solver.setSelectionStrategy(ISelectionStrategy.random(random));
    }

    @Override
    protected void resetSolver(SAT4JSolutionSolver solver) {
        super.resetSolver(solver);
        solver.setSelectionStrategy(originalSelectionStrategy);
        originalSelectionStrategy = null;
    }
}
