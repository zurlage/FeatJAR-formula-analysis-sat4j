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
package de.featjar.formula.analysis.sat4j;

import static de.featjar.base.computation.Computations.async;
import static de.featjar.formula.structure.Expressions.literal;
import static de.featjar.formula.structure.Expressions.or;
import static org.junit.jupiter.api.Assertions.assertEquals;

import de.featjar.base.FeatJAR;
import de.featjar.base.computation.Computations;
import de.featjar.base.computation.IComputation;
import de.featjar.base.io.IO;
import de.featjar.formula.analysis.bool.BooleanAssignment;
import de.featjar.formula.analysis.bool.BooleanClauseList;
import de.featjar.formula.analysis.bool.BooleanRepresentationComputation;
import de.featjar.formula.analysis.bool.BooleanSolutionList;
import de.featjar.formula.analysis.sat4j.solver.ISelectionStrategy;
import de.featjar.formula.analysis.sat4j.twise.CoverageStatistic;
import de.featjar.formula.analysis.sat4j.twise.RelativeTWiseCoverageComputation;
import de.featjar.formula.analysis.sat4j.twise.TWiseCoverageComputation;
import de.featjar.formula.analysis.sat4j.twise.TWiseStatisticGenerator;
import de.featjar.formula.analysis.sat4j.twise.YASA;
import de.featjar.formula.io.FormulaFormats;
import de.featjar.formula.structure.formula.IFormula;
import de.featjar.formula.transformer.ComputeCNFFormula;
import de.featjar.formula.transformer.ComputeNNFFormula;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class YASATest {

    @BeforeAll
    public static void init() {
        FeatJAR.initialize();
    }

    public void getTWiseSample(IFormula formula, int t) {
        IComputation<BooleanClauseList> clauses = getClauses(formula);
        BooleanSolutionList sample = clauses.map(YASA::new)
                .setDependencyComputation(YASA.T, async(t))
                .compute();
        checkCoverage(t, clauses, sample);
    }

    private void checkCoverage(int t, IComputation<BooleanClauseList> clauses, BooleanSolutionList sample) {
        // TODO split into multiple tests
        CoverageStatistic statistic1 = clauses.map(TWiseCoverageComputation::new)
                .set(TWiseCoverageComputation.SAMPLE, sample)
                .set(TWiseCoverageComputation.T, t)
                .compute();
        assertEquals(1.0, statistic1.coverage());

        CoverageStatistic statistic2 = clauses.map(RelativeTWiseCoverageComputation::new)
                .set(RelativeTWiseCoverageComputation.SAMPLE, sample)
                .setDependencyComputation(
                        RelativeTWiseCoverageComputation.REFERENCE_SAMPLE, clauses.map(ComputeSolutionsSAT4J::new))
                .set(RelativeTWiseCoverageComputation.T, t)
                .compute();
        assertEquals(1.0, statistic2.coverage());

        assertEquals(statistic1.covered(), statistic2.covered());
        assertEquals(statistic1.uncovered(), statistic2.uncovered());
        assertEquals(statistic1.invalid(), statistic2.invalid());

        CoverageStatistic statistic3 = clauses.map(TWiseStatisticGenerator::new)
                .set(TWiseStatisticGenerator.SAMPLE, sample)
                .set(TWiseStatisticGenerator.CORE, new BooleanAssignment())
                .set(TWiseStatisticGenerator.T, t)
                .compute();
        assertEquals(1.0, statistic3.coverage());
        assertEquals(statistic1.covered(), statistic3.covered());
        assertEquals(statistic1.uncovered(), statistic3.uncovered());
        assertEquals(statistic1.invalid(), statistic3.invalid());
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

    @Test
    void both() {
        IFormula formula = loadModel("models_stability_light/busybox_monthlySnapshot/2007-05-20_17-12-43/clean.dimacs");
        int t = 2;
        IComputation<BooleanClauseList> clauses = getClauses(formula);
        BooleanSolutionList sample = clauses.map(ComputeSolutionsSAT4J::new)
                .set(ComputeSolutionsSAT4J.SELECTION_STRATEGY, ISelectionStrategy.Strategy.FastRandom)
                .set(ComputeSolutionsSAT4J.LIMIT, 10)
                .set(ComputeSolutionsSAT4J.RANDOM_SEED, 1L)
                .compute();

        CoverageStatistic statistic1 = clauses.map(TWiseCoverageComputation::new)
                .set(TWiseCoverageComputation.SAMPLE, sample)
                .set(TWiseCoverageComputation.T, t)
                .compute();

        CoverageStatistic statistic3 = clauses.map(TWiseStatisticGenerator::new)
                .set(TWiseStatisticGenerator.SAMPLE, sample)
                .set(TWiseStatisticGenerator.CORE, new BooleanAssignment())
                .set(TWiseStatisticGenerator.T, t)
                .compute();
        System.out.println("total     " + statistic1.total() + " vs " + statistic3.total());
        System.out.println("covered   " + statistic1.covered() + " vs " + statistic3.covered());
        System.out.println("uncovered " + statistic1.uncovered() + " vs " + statistic3.uncovered());
        System.out.println("invalid   " + statistic1.invalid() + " vs " + statistic3.invalid());
        assertEquals(statistic1.total(), statistic3.total());
        assertEquals(statistic1.covered(), statistic3.covered());
        assertEquals(statistic1.uncovered(), statistic3.uncovered());
        assertEquals(statistic1.invalid(), statistic3.invalid());
    }

    @Test
    void onlyNew() {
        IFormula formula = loadModel("models_stability_light/busybox_monthlySnapshot/2007-05-20_17-12-43/clean.dimacs");
        int t = 2;
        IComputation<BooleanClauseList> clauses = getClauses(formula);
        BooleanSolutionList sample = clauses.map(ComputeSolutionsSAT4J::new)
                .set(ComputeSolutionsSAT4J.SELECTION_STRATEGY, ISelectionStrategy.Strategy.FastRandom)
                .set(ComputeSolutionsSAT4J.LIMIT, 10)
                .set(ComputeSolutionsSAT4J.RANDOM_SEED, 1L)
                .compute();

        CoverageStatistic statistic1 = clauses.map(TWiseCoverageComputation::new)
                .set(TWiseCoverageComputation.SAMPLE, sample)
                .set(TWiseCoverageComputation.T, t)
                .compute();
    }

    @Test
    void onlyOld() {
        IFormula formula = loadModel("models_stability_light/busybox_monthlySnapshot/2007-05-20_17-12-43/clean.dimacs");
        int t = 2;
        IComputation<BooleanClauseList> clauses = getClauses(formula);
        BooleanSolutionList sample = clauses.map(ComputeSolutionsSAT4J::new)
                .set(ComputeSolutionsSAT4J.SELECTION_STRATEGY, ISelectionStrategy.Strategy.FastRandom)
                .set(ComputeSolutionsSAT4J.LIMIT, 10)
                .set(ComputeSolutionsSAT4J.RANDOM_SEED, 1L)
                .compute();

        CoverageStatistic statistic3 = clauses.map(TWiseStatisticGenerator::new)
                .set(TWiseStatisticGenerator.SAMPLE, sample)
                .set(TWiseStatisticGenerator.CORE, new BooleanAssignment())
                .set(TWiseStatisticGenerator.T, t)
                .compute();
    }

    private IFormula loadModel(String modelPath) {
        return IO.load(Paths.get("src/test/resources/" + modelPath), FormulaFormats.getInstance())
                .orElseThrow();
    }
}
