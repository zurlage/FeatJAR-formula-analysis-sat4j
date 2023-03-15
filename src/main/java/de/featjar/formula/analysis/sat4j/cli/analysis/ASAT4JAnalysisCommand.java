/*
 * Copyright (C) 2023 Elias Kuiter
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
package de.featjar.formula.analysis.sat4j.cli.analysis;

import static de.featjar.base.computation.Computations.*;

import de.featjar.base.cli.ICommand;
import de.featjar.base.cli.Option;
import de.featjar.base.computation.IComputation;
import de.featjar.formula.analysis.VariableMap;
import de.featjar.formula.analysis.bool.BooleanClauseList;
import de.featjar.formula.analysis.bool.ComputeBooleanRepresentationOfCNFFormula;
import de.featjar.formula.analysis.sat4j.ASAT4JAnalysis;
import de.featjar.formula.cli.analysis.AAnalysisCommand;
import de.featjar.formula.transformer.ComputeCNFFormula;
import de.featjar.formula.transformer.ComputeNNFFormula;
import java.util.List;

public abstract class ASAT4JAnalysisCommand<T, U> extends AAnalysisCommand<T> {
    @Override
    public List<Option<?>> getOptions() {
        return ICommand.addOptions(super.getOptions(), ASSIGNMENT_OPTION, CLAUSES_OPTION, TIMEOUT_OPTION);
    }

    @SuppressWarnings("unchecked")
    @Override
    public IComputation<T> newComputation() {
        var booleanRepresentation = async(formula)
                .map(ComputeNNFFormula::new)
                .map(ComputeCNFFormula::new)
                .map(ComputeBooleanRepresentationOfCNFFormula::new);
        var booleanClauseList = getKey(booleanRepresentation);
        var variableMap = getValue(booleanRepresentation);
        var analysis = newAnalysis(booleanClauseList);
        analysis.setAssumedAssignment(optionParser.get(ASSIGNMENT_OPTION).get().toBoolean(variableMap));
        analysis.setAssumedClauseList(
                optionParser.get(CLAUSES_OPTION).get().toBoolean(variableMap));
        analysis.setTimeout(async(optionParser.get(TIMEOUT_OPTION)));
        return interpret(analysis, variableMap);
    }

    public abstract ASAT4JAnalysis<U> newAnalysis(IComputation<BooleanClauseList> clauseList);

    public abstract IComputation<T> interpret(IComputation<U> result, IComputation<VariableMap> variableMap);
}
