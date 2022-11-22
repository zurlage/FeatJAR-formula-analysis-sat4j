package de.featjar.formula.analysis.sat4j;

import de.featjar.base.data.Computation;
import de.featjar.formula.analysis.Assignment;
import de.featjar.formula.analysis.bool.ToLiteralClauseList;
import de.featjar.formula.structure.formula.Formula;
import de.featjar.formula.transformer.ToCNF;
import de.featjar.formula.transformer.ToNNF;
import org.junit.jupiter.api.Test;

import static de.featjar.formula.structure.Expressions.*;
import static org.junit.jupiter.api.Assertions.*;

public class SAT4JHasSolutionAnalysisTest {
    public boolean hasSolution(Formula formula) {
        return Computation.of(formula)
                .then(ToNNF::new)
                .then(ToCNF::new)
                .then(ToLiteralClauseList::new)
                .then(SAT4JHasSolutionAnalysis::new)
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
