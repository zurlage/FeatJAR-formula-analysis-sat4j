package de.featjar.formula.analysis.sat4j;

import de.featjar.base.computation.Computable;
import de.featjar.formula.analysis.bool.ComputeBooleanRepresentation;
import de.featjar.formula.structure.formula.Formula;
import de.featjar.formula.transformer.TransformCNFFormula;
import de.featjar.formula.transformer.TransformNNFFormula;
import org.junit.jupiter.api.Test;

import static de.featjar.formula.structure.Expressions.*;
import static org.junit.jupiter.api.Assertions.*;

public class AnalyzeHasSolutionSAT4JTest {
    public boolean hasSolution(Formula formula) {
        return Computable.of(formula)
                .map(TransformNNFFormula::new)
                .map(TransformCNFFormula::new)
                .map(ComputeBooleanRepresentation::new)
                .map(AnalyzeHasSolutionSAT4J::new)
                .getResult()
                .get();
    }

    //TODO: all tests below only work when the formula is wrapped in and(...) as an auxiliary root. fix this, it is a big potential bug source
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
