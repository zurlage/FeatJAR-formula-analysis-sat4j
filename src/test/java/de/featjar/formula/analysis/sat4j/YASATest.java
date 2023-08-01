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
import de.featjar.base.io.IO;
import de.featjar.formula.analysis.bool.BooleanClauseList;
import de.featjar.formula.analysis.bool.BooleanRepresentationComputation;
import de.featjar.formula.analysis.bool.BooleanSolutionList;
import de.featjar.formula.analysis.sat4j.twise.CoverageStatistic;
import de.featjar.formula.analysis.sat4j.twise.TWiseStatisticGenerator;
import de.featjar.formula.analysis.sat4j.twise.YASA;
import de.featjar.formula.io.FormulaFormats;
import de.featjar.formula.structure.formula.IFormula;
import de.featjar.formula.transformer.ComputeCNFFormula;
import de.featjar.formula.transformer.ComputeNNFFormula;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

public class YASATest {

    public void getTWiseSample(IFormula formula, int t) {
        IComputation<BooleanClauseList> clauses = getClauses(formula);
        BooleanSolutionList sample = await(clauses.map(YASA::new).setDependency(YASA.T, async(t)));
        checkCoverage(t, clauses, sample);
    }

    private void checkCoverage(int t, IComputation<BooleanClauseList> clauses, BooleanSolutionList sample) {
        CoverageStatistic statistic = await(clauses.map(TWiseStatisticGenerator::new)
                .setDependency(TWiseStatisticGenerator.SAMPLE, async(sample))
                .setDependency(TWiseStatisticGenerator.T, async(t)));
        System.out.println(sample.size());
        assertEquals(1.0, statistic.coverage());
    }

    private IComputation<BooleanClauseList> getClauses(IFormula formula) {
        BooleanRepresentationComputation<IFormula, BooleanClauseList> cnf = async(formula)
                .map(ComputeNNFFormula::new)
                .map(ComputeCNFFormula::new)
                .map(BooleanRepresentationComputation::new);
        IComputation<BooleanClauseList> clauses = cnf.map(Computations::getKey);
        return clauses;
    }

    @Test
    void formulaHas1WiseCoverage() {
        getTWiseSample(or(literal("x"), literal(false, "y"), literal(false, "z")), 1);
    }

    @Test
    void gplHas1WiseCoverage() {
        getTWiseSample(loadModel("GPL/model.xml"), 1);
    }

    @Test
    void formulaHas2WiseCoverage() {
        getTWiseSample(or(literal("x"), literal(false, "y"), literal(false, "z")), 2);
    }

    @Test
    void gplHas2WiseCoverage() {
        getTWiseSample(loadModel("GPL/model.xml"), 3);
    }

    @Test
    void formulaHas3WiseCoverage() {
        getTWiseSample(or(literal("x"), literal(false, "y"), literal(false, "z")), 3);
    }

    @Test
    void gplHas3WiseCoverage() {
        getTWiseSample(loadModel("GPL/model.xml"), 3);
    }

    private IFormula loadModel(String modelPath) {
        return IO.load(Paths.get("src/test/resources/" + modelPath), FormulaFormats.getInstance())
                .orElseThrow();
    }
}
