package de.featjar.formula.analysis.sat4j;

import de.featjar.base.data.Computation;
import de.featjar.formula.clauses.ToCNF;
import de.featjar.formula.structure.Expressions;
import org.junit.jupiter.api.Test;

import static de.featjar.formula.structure.Expressions.*;
import static org.junit.jupiter.api.Assertions.*;

public class HasSolutionAnalysisTest {
    @Test
    void hasSolution() {
        assertTrue(Computation.of(and(literal("x"), not(literal("y"))))
                .then(ToCNF.class)
                .then(HasSolutionAnalysis.class)
                .computeResult()
                .get());
        assertFalse(Computation.of(and(literal("x"), not(literal("x"))))
                .then(ToCNF.class)
                .then(HasSolutionAnalysis.class)
                .computeResult()
                .get());
    }
}
