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
import static de.featjar.base.computation.Computations.await;
import static de.featjar.formula.structure.Expressions.*;
import static org.junit.jupiter.api.Assertions.*;

import de.featjar.base.computation.Computations;
import de.featjar.base.computation.ComputePresence;
import de.featjar.formula.analysis.bool.BooleanClauseList;
import de.featjar.formula.analysis.bool.BooleanRepresentationComputation;
import de.featjar.formula.analysis.bool.BooleanSolution;
import de.featjar.formula.structure.formula.IFormula;
import de.featjar.formula.transformer.ComputeCNFFormula;
import de.featjar.formula.transformer.ComputeNNFFormula;
import org.junit.jupiter.api.Test;

public class AnalyzeHasSolutionSAT4JTest {
    public boolean hasSolution(IFormula formula) {
        return await(async(formula)
                .map(ComputeNNFFormula::new)
                .map(ComputeCNFFormula::new)
                .set(ComputeCNFFormula.IS_PLAISTED_GREENBAUM, Boolean.TRUE)
                .map(BooleanRepresentationComputation::new)
                .map(Computations::getKey)
                .cast(BooleanClauseList.class)
                .map(ComputeSolutionSAT4J::new)
                .map(ComputePresence<BooleanSolution>::new));
    }

    // TODO: all tests below only work when the formula is wrapped in and(...) as an auxiliary root. fix this, it is a
    // big potential bug source
    @Test
    void satisfiableFormulaInCNFHasSolution() {
        assertTrue(hasSolution(and(literal("x"), literal(false, "y"))));
    }

    @Test
    void unsatisfiableFormulaInCNFHasNoSolution() {
        assertFalse(hasSolution(and(literal("x"), literal(false, "x"))));
    }

    @Test
    void satisfiableArbitraryFormulaHasSolution() {
        assertTrue(hasSolution(biImplies(literal("a"), literal("b"))));
    }

    @Test
    void unsatisfiableArbitraryFormulaHasNoSolution() {
        assertFalse(hasSolution(and(biImplies(literal("a"), not(literal("a"))))));
    }
}
