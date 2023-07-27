/*
 * Copyright (C) 2023 Sebastian Krieter
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
package de.featjar.formula.analysis.sat4j;

import static de.featjar.base.computation.Computations.async;
import static de.featjar.base.computation.Computations.await;
import static de.featjar.formula.structure.Expressions.literal;
import static de.featjar.formula.structure.Expressions.or;
import static org.junit.jupiter.api.Assertions.assertEquals;

import de.featjar.base.computation.Computations;
import de.featjar.base.computation.IComputation;
import de.featjar.formula.analysis.bool.BooleanClauseList;
import de.featjar.formula.analysis.bool.BooleanRepresentationComputation;
import de.featjar.formula.analysis.bool.BooleanSolutionList;
import de.featjar.formula.analysis.sat4j.twise.CoverageStatistic;
import de.featjar.formula.analysis.sat4j.twise.TWiseStatisticGenerator;
import de.featjar.formula.analysis.sat4j.twise.YASA;
import de.featjar.formula.structure.formula.IFormula;
import de.featjar.formula.transformer.ComputeCNFFormula;
import de.featjar.formula.transformer.ComputeNNFFormula;
import org.junit.jupiter.api.Test;

public class YASATest {

    public void getTWiseSample(IFormula formula, int t) {
        BooleanRepresentationComputation<IFormula, BooleanClauseList> cnf = async(formula)
                .map(ComputeNNFFormula::new)
                .map(ComputeCNFFormula::new)
                .map(BooleanRepresentationComputation::new);
        IComputation<BooleanClauseList> clauses = cnf.map(Computations::getKey);

        BooleanSolutionList sample = await(clauses.map(YASA::new).setDependency(YASA.T, async(t)));

        CoverageStatistic statistic = await(clauses.map(TWiseStatisticGenerator::new)
                .setDependency(TWiseStatisticGenerator.SAMPLE, async(sample))
                .setDependency(TWiseStatisticGenerator.T, async(t)));

        assertEquals(1.0, statistic.coverage());
    }

    @Test
    void test() {
        getTWiseSample(or(literal("x"), literal(false, "y"), literal(false, "z")), 2);
    }
}
