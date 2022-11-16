package de.featjar.formula.analysis.sat4j;

import de.featjar.base.data.Computation;
import de.featjar.formula.analysis.bool.ToLiteralClauseList;
import de.featjar.formula.structure.formula.Formula;
import org.junit.jupiter.api.Test;

import static de.featjar.formula.structure.Expressions.*;

public class HasSolutionAnalysisTest {
    @Test
    void hasSolution() {
        assertTrue(Computation.of((Formula) and(literal("x"), not(literal("y"))))
                .then(ToLiteralClauseList::new)
                .then(c -> new HasSolutionAnalysis(c, new Assignment(), 1000, 1000)) // todo: timeout 0 is not interpreted correctly
                .getResult()
                .get());
        assertFalse(Computation.of((Formula) and(literal("x"), not(literal("x"))))
                .then(ToLiteralClauseList::new)
                .then(c -> new HasSolutionAnalysis(c, new Assignment(), 1000, 1000))
                .getResult()
                .get());
    }
}
