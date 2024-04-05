/*
 * Copyright (C) 2024 FeatJAR-Development-Team
 *
 * This file is part of FeatJAR-formula-analysis-sat4j.
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
package de.featjar.formula.analysis.cli;

import de.featjar.base.computation.Computations;
import de.featjar.base.computation.IComputation;
import de.featjar.formula.analysis.bool.BooleanAssignment;
import de.featjar.formula.analysis.bool.ComputeBooleanClauseList;
import de.featjar.formula.analysis.mig.ComputeCoreDead;
import java.util.Optional;

/**
 * Computes core and dead variables for a given formula using SAT4J.
 *
 * @author Sebastian Krieter
 * @author Andreas Gerasimow
 */
public class CoreCommand extends ASAT4JAnalysisCommand<BooleanAssignment, BooleanAssignment> {

    @Override
    public Optional<String> getDescription() {
        return Optional.of("Computes core and dead variables for a given formula using SAT4J.");
    }

    @Override
    public IComputation<BooleanAssignment> newAnalysis(ComputeBooleanClauseList formula) {
        return formula.map(Computations::getKey).map(ComputeCoreDead::new);
    }

    @Override
    public String serializeResult(BooleanAssignment assignment) {
        return assignment.print();
    }

    @Override
    public Optional<String> getShortName() {
        return Optional.of("core-sat4j");
    }
}
