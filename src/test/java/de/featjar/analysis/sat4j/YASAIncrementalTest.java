/*
 * Copyright (C) 2025 FeatJAR-Development-Team
 *
 * This file is part of FeatJAR-FeatJAR-formula-analysis-sat4j.
 *
 * FeatJAR-formula-analysis-sat4j is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3.0 of the License,
 * or (at your option) any later version.
 *
 * FeatJAR-formula-analysis-sat4j is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FeatJAR-formula-analysis-sat4j. If not, see <https://www.gnu.org/licenses/>.
 *
 * See <https://github.com/FeatureIDE/FeatJAR-formula-analysis-sat4j> for further information.
 */
package de.featjar.analysis.sat4j;

import static de.featjar.base.computation.Computations.async;
import static de.featjar.formula.structure.Expressions.literal;
import static de.featjar.formula.structure.Expressions.or;
import static org.junit.jupiter.api.Assertions.assertEquals;

import de.featjar.Common;
import de.featjar.analysis.sat4j.computation.ComputeSolutionsSAT4J;
import de.featjar.analysis.sat4j.computation.YASAIncremental;
import de.featjar.analysis.sat4j.solver.ISelectionStrategy;
import de.featjar.analysis.sat4j.twise.CoverageStatistic;
import de.featjar.analysis.sat4j.twise.RelativeTWiseCoverageComputation;
import de.featjar.analysis.sat4j.twise.TWiseCoverageComputation;
import de.featjar.analysis.sat4j.twise.TWiseStatisticGenerator;
import de.featjar.base.FeatJAR;
import de.featjar.base.computation.IComputation;
import de.featjar.formula.assignment.BooleanAssignment;
import de.featjar.formula.assignment.BooleanClauseList;
import de.featjar.formula.assignment.BooleanSolutionList;
import de.featjar.formula.assignment.ComputeBooleanClauseList;
import de.featjar.formula.computation.ComputeCNFFormula;
import de.featjar.formula.computation.ComputeNNFFormula;
import de.featjar.formula.structure.IFormula;
import java.time.Duration;
import org.junit.jupiter.api.Test;

public class YASAIncrementalTest extends Common {

    @Test
    void formulaHas1WiseCoverage() {
        assertFullCoverageWithAllAlgorithms(or(literal("x"), literal(false, "y"), literal(false, "z")), 1);
    }

    @Test
    void gplHas1WiseCoverage() {
        assertFullCoverageWithAllAlgorithms(loadFormula("GPL/model.xml"), 1);
    }

    @Test
    void formulaHas2WiseCoverage() {
        assertFullCoverageWithAllAlgorithms(or(literal("x"), literal(false, "y"), literal(false, "z")), 2);
    }

    @Test
    void gplHas2WiseCoverage() {
        assertFullCoverageWithAllAlgorithms(loadFormula("GPL/model.xml"), 2);
    }

    @Test
    void formulaHas3WiseCoverage() {
        assertFullCoverageWithAllAlgorithms(or(literal("x"), literal(false, "y"), literal(false, "z")), 3);
    }

    @Test
    void gplHas3WiseCoverage() {
        assertFullCoverageWithAllAlgorithms(loadFormula("GPL/model.xml"), 3);
    }

    @Test
    void modelWithFreeVariablesHas1WiseCoverage() {
        assertFullCoverageWithAllAlgorithms(loadFormula("testFeatureModels/model_with_free_variables.dimacs"), 1);
    }

    @Test
    void modelWithFreeVariablesHas2WiseCoverage() {
        assertFullCoverageWithAllAlgorithms(loadFormula("testFeatureModels/model_with_free_variables.dimacs"), 2);
    }

    @Test
    void modelWithFreeVariablesHas3WiseCoverage() {
        assertFullCoverageWithAllAlgorithms(loadFormula("testFeatureModels/model_with_free_variables.dimacs"), 3);
    }

    @Test
    public void testBothCoverageRandom() {
        bothRandom(loadFormula("models_stability_light/busybox_monthlySnapshot/2007-05-20_17-12-43/clean.dimacs"));
    }

    @Test
    public void benchmark() {
        benchmarkCompareSample("models_stability_light/busybox_monthlySnapshot/2007-05-20_17-12-43/clean.dimacs", 2);
    }

    @Test
    void gplRunsUntilTimeout() {
        testTimeout(loadFormula("GPL/model.xml"), 10);
    }

    private void testTimeout(IFormula formula, int timeoutSeconds) {
        IComputation<BooleanClauseList> clauses = getClauses(formula);
        BooleanSolutionList sample = clauses.map(YASAIncremental::new)
                .set(YASAIncremental.T, 3)
                .set(YASAIncremental.ITERATIONS, Integer.MAX_VALUE)
                .computeResult(Duration.ofSeconds(timeoutSeconds))
                .orElseThrow();
        FeatJAR.log().info("Sample Size: %d", sample.size());

        long time = System.currentTimeMillis();
        CoverageStatistic statistic1 = computeCoverageNew(3, clauses, sample);
        assertEquals(1.0, statistic1.coverage());
        FeatJAR.log().info((System.currentTimeMillis() - time) / 1000.0);
    }

    private void benchmarkCompareSample(String modelPath, int t) {
        IFormula formula = loadFormula(modelPath);
        IComputation<BooleanClauseList> clauses = getClauses(formula);

        FeatJAR.log().info("Comparing random sample (10) for %s with t = %d", modelPath, t);
        benchmarkCompare(clauses, computeRandomSample(clauses, 10), t);
        FeatJAR.log().info("Comparing random sample (100) for %s with t = %d", modelPath, t);
        benchmarkCompare(clauses, computeRandomSample(clauses, 100), t);
        FeatJAR.log().info("Comparing  %d-wise for %s with t = %d", t, modelPath, t);
        benchmarkCompare(clauses, computeSample(t, clauses), t);
    }

    private void benchmarkCompare(IComputation<BooleanClauseList> clauses, BooleanSolutionList sample, int t) {
        long time;
        time = System.currentTimeMillis();
        computeCoverageNew(t, clauses, sample);
        FeatJAR.log().info((System.currentTimeMillis() - time) / 1000.0);

        time = System.currentTimeMillis();
        computeCoverageOld(t, clauses, sample);
        FeatJAR.log().info((System.currentTimeMillis() - time) / 1000.0);
    }

    void onlyNew(IFormula formula) {
        IComputation<BooleanClauseList> clauses = getClauses(formula);
        BooleanSolutionList sample = computeSample(2, clauses);
        computeCoverageNew(2, clauses, sample);
    }

    private void bothRandom(IFormula formula) {
        IComputation<BooleanClauseList> clauses = getClauses(formula);
        BooleanSolutionList sample = computeRandomSample(clauses, 10);
        CoverageStatistic statistic1 = computeCoverageNew(2, clauses, sample);
        CoverageStatistic statistic3 = computeCoverageOld(2, clauses, sample);

        FeatJAR.log().debug("total     %d | %d", statistic1.total(), statistic3.total());
        FeatJAR.log().debug("covered   %d | %d", statistic1.covered(), statistic3.covered());
        FeatJAR.log().debug("uncovered %d | %d", statistic1.uncovered(), statistic3.uncovered());
        FeatJAR.log().debug("invalid   %d | %d", statistic1.invalid(), statistic3.invalid());
        assertEquals(statistic1.total(), statistic3.total());
        assertEquals(statistic1.covered(), statistic3.covered());
        assertEquals(statistic1.uncovered(), statistic3.uncovered());
        assertEquals(statistic1.invalid(), statistic3.invalid());
    }

    void onlyNewRandom(IFormula formula) {
        IComputation<BooleanClauseList> clauses = getClauses(formula);
        BooleanSolutionList sample = computeRandomSample(clauses, 10);
        computeCoverageNew(2, clauses, sample);
    }

    private BooleanSolutionList computeRandomSample(IComputation<BooleanClauseList> clauses, int size) {
        BooleanSolutionList sample = clauses.map(ComputeSolutionsSAT4J::new)
                .set(ComputeSolutionsSAT4J.SELECTION_STRATEGY, ISelectionStrategy.Strategy.FAST_RANDOM)
                .set(ComputeSolutionsSAT4J.LIMIT, size)
                .set(ComputeSolutionsSAT4J.RANDOM_SEED, 1L)
                .compute();
        return sample;
    }

    public void assertFullCoverageWithAllAlgorithms(IFormula formula, int t) {
        IComputation<BooleanClauseList> clauses = getClauses(formula);
        BooleanSolutionList sample = computeSample(t, clauses);

        CoverageStatistic statistic1 = computeCoverageNew(t, clauses, sample);
        CoverageStatistic statistic2 = computeCoverageRel(t, clauses, sample);
        CoverageStatistic statistic3 = computeCoverageOld(t, clauses, sample);
        CoverageStatistic statistic4 = computeCoverageRel2(t, clauses, sample);

        FeatJAR.log().info("total     %d | %d | %d", statistic1.total(), statistic2.total(), statistic3.total());
        FeatJAR.log().info("covered   %d | %d | %d", statistic1.covered(), statistic2.covered(), statistic3.covered());
        FeatJAR.log()
                .info("uncovered %d | %d | %d", statistic1.uncovered(), statistic2.uncovered(), statistic3.uncovered());
        FeatJAR.log().info("invalid   %d | %d | %d", statistic1.invalid(), statistic2.invalid(), statistic3.invalid());

        assertEquals(1.0, statistic1.coverage());
        assertEquals(1.0, statistic2.coverage());
        assertEquals(1.0, statistic3.coverage());
        assertEquals(1.0, statistic4.coverage());

        assertEquals(statistic1.covered(), statistic2.covered());
        assertEquals(statistic1.uncovered(), statistic2.uncovered());
        assertEquals(statistic1.invalid(), statistic2.invalid());
        assertEquals(statistic1.covered(), statistic3.covered());
        assertEquals(statistic1.uncovered(), statistic3.uncovered());
        assertEquals(statistic1.invalid(), statistic3.invalid());
        assertEquals(statistic1.covered(), statistic4.covered());
        assertEquals(statistic1.uncovered(), statistic4.uncovered());
        assertEquals(statistic1.invalid(), statistic4.invalid());
    }

    private BooleanSolutionList computeSample(int t, IComputation<BooleanClauseList> clauses) {
        BooleanSolutionList sample = clauses.map(YASAIncremental::new)
                .setDependencyComputation(YASAIncremental.T, async(t))
                .compute();
        FeatJAR.log().info("Sample Size: %d", sample.size());
        return sample;
    }

    private CoverageStatistic computeCoverageOld(
            int t, IComputation<BooleanClauseList> clauses, BooleanSolutionList sample) {
        CoverageStatistic statistic = clauses.map(TWiseStatisticGenerator::new)
                .set(TWiseStatisticGenerator.SAMPLE, sample)
                .set(TWiseStatisticGenerator.CORE, new BooleanAssignment())
                .set(TWiseStatisticGenerator.T, t)
                .compute();
        FeatJAR.log().info("Computed Coverage (TWiseStatisticGenerator)");
        return statistic;
    }

    private CoverageStatistic computeCoverageRel(
            int t, IComputation<BooleanClauseList> clauses, BooleanSolutionList sample) {
        CoverageStatistic statistic = clauses.map(ComputeSolutionsSAT4J::new)
                .map(RelativeTWiseCoverageComputation::new)
                .set(RelativeTWiseCoverageComputation.SAMPLE, sample)
                .set(RelativeTWiseCoverageComputation.T, t)
                .compute();
        FeatJAR.log().info("Computed Coverage (RelativeTWiseCoverageComputation)");
        return statistic;
    }

    private CoverageStatistic computeCoverageRel2(
            int t, IComputation<BooleanClauseList> clauses, BooleanSolutionList sample) {
        CoverageStatistic statistic = clauses.map(ComputeSolutionsSAT4J::new)
                .map(RelativeTWiseCoverageComputation::new)
                .set(RelativeTWiseCoverageComputation.SAMPLE, sample)
                .set(RelativeTWiseCoverageComputation.T, t)
                .compute();
        FeatJAR.log().info("Computed Coverage (RelativeTWiseCoverageComputation2)");
        return statistic;
    }

    private CoverageStatistic computeCoverageNew(
            int t, IComputation<BooleanClauseList> clauses, BooleanSolutionList sample) {
        CoverageStatistic statistic = clauses.map(TWiseCoverageComputation::new)
                .set(TWiseCoverageComputation.SAMPLE, sample)
                .set(TWiseCoverageComputation.T, t)
                .compute();
        FeatJAR.log().info("Computed Coverage (TWiseCoverageComputation)");
        return statistic;
    }

    private IComputation<BooleanClauseList> getClauses(IFormula formula) {
        return async(formula)
                .map(ComputeNNFFormula::new)
                .map(ComputeCNFFormula::new)
                .map(ComputeBooleanClauseList::new);
    }
}
