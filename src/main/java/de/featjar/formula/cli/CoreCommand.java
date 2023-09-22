/*
 * Copyright (C) 2023 FeatJAR-Development-Team
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
package de.featjar.formula.cli;

import static de.featjar.base.computation.Computations.async;

import de.featjar.base.FeatJAR;
import de.featjar.base.cli.ICommand;
import de.featjar.base.cli.IOptionInput;
import de.featjar.base.cli.Option;
import de.featjar.base.computation.Computations;
import de.featjar.base.data.Result;
import de.featjar.base.io.IO;
import de.featjar.formula.analysis.bool.BooleanAssignment;
import de.featjar.formula.analysis.bool.BooleanClauseList;
import de.featjar.formula.analysis.bool.BooleanRepresentationComputation;
import de.featjar.formula.analysis.mig.ComputeCoreDead;
import de.featjar.formula.io.FormulaFormats;
import de.featjar.formula.structure.formula.IFormula;
import de.featjar.formula.transformer.ComputeCNFFormula;
import de.featjar.formula.transformer.ComputeNNFFormula;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class CoreCommand implements ICommand {

    public static final Option<Path> formulaPath = new Option<>("formula", Option.ExistingPathParser);

    @Override
    public void run(IOptionInput optionParser) {
        Result<Path> p = optionParser.get(formulaPath);
        p.get();
        Result<IFormula> load = IO.load(p.get(), FormulaFormats.getInstance());

        BooleanRepresentationComputation<IFormula, BooleanClauseList> cnf = async(load.get())
                .map(ComputeNNFFormula::new)
                .map(ComputeCNFFormula::new)
                .map(BooleanRepresentationComputation::new);
        BooleanAssignment compute =
                cnf.map(Computations::getKey).map(ComputeCoreDead::new).compute();
        FeatJAR.log().info(compute);
    }

    @Override
    public String getDescription() {
        return "Computes core and dead variables for a given formula";
    }

    @Override
    public List<Option<?>> getOptions() {
        return Arrays.asList(formulaPath);
    }
}
