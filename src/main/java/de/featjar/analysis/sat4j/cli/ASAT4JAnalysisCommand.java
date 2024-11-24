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
package de.featjar.analysis.sat4j.cli;

import de.featjar.analysis.AAnalysisCommand;
import de.featjar.base.cli.Option;
import de.featjar.base.cli.OptionList;
import de.featjar.base.computation.Computations;
import de.featjar.base.computation.IComputation;
import de.featjar.base.io.IO;
import de.featjar.formula.assignment.BooleanAssignmentGroups;
import de.featjar.formula.assignment.BooleanClauseList;
import de.featjar.formula.assignment.ComputeBooleanClauseList;
import de.featjar.formula.computation.ComputeCNFFormula;
import de.featjar.formula.computation.ComputeNNFFormula;
import de.featjar.formula.io.BooleanAssignmentGroupsFormats;
import de.featjar.formula.io.FormulaFormats;
import de.featjar.formula.structure.IFormula;
import java.nio.file.Path;
import java.time.Duration;

public abstract class ASAT4JAnalysisCommand<T, U> extends AAnalysisCommand<T> {

    /**
     * Option for setting the seed for the pseudo random generator.
     */
    public static final Option<Long> RANDOM_SEED_OPTION = Option.newOption("seed", Option.LongParser) //
            .setDescription("Seed for the pseudo random generator") //
            .setDefaultValue(1L);

    /**
     * Timeout option for canceling running computations.
     */
    public static final Option<Duration> SAT_TIMEOUT_OPTION = Option.newOption(
                    "solver_timeout", s -> Duration.ofMillis(Long.parseLong(s)))
            .setDescription("Timeout in milliseconds")
            .setValidator(timeout -> !timeout.isNegative())
            .setDefaultValue(Duration.ZERO);

    @Override
    protected IComputation<T> newComputation(OptionList optionParser) {
        Path inputPath = optionParser.getResult(INPUT_OPTION).orElseThrow();
        IComputation<BooleanClauseList> computation;
        BooleanAssignmentGroups cnf =
                IO.load(inputPath, BooleanAssignmentGroupsFormats.getInstance()).orElse(null);
        if (cnf != null) {
            computation = Computations.of(cnf.getFirstGroup().toClauseList());
        } else {
            IFormula formula = IO.load(inputPath, FormulaFormats.getInstance()).orElseThrow();
            computation = Computations.of(formula)
                    .map(ComputeNNFFormula::new)
                    .map(ComputeCNFFormula::new)
                    .map(ComputeBooleanClauseList::new);
        }
        return newAnalysis(optionParser, computation);
    }

    protected abstract IComputation<T> newAnalysis(OptionList optionParser, IComputation<BooleanClauseList> formula);
}
