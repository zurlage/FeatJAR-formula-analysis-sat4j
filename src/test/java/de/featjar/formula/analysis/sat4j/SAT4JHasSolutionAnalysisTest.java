package de.featjar.formula.analysis.sat4j;

import de.featjar.base.data.Computation;
import de.featjar.formula.analysis.Assignment;
import de.featjar.formula.analysis.bool.ToLiteralClauseList;
import de.featjar.formula.structure.formula.Formula;
import org.junit.jupiter.api.Test;

import static de.featjar.formula.structure.Expressions.*;
import static org.junit.jupiter.api.Assertions.*;

public class SAT4JHasSolutionAnalysisTest {
    @Test
    void hasSolution() {
        assertTrue(Computation.of((Formula) and(literal("x"), literal(false, "y")))
                .then(ToLiteralClauseList::new)
                .then(SAT4JHasSolutionAnalysis::new)
                .getResult()
                .get());
        assertFalse(Computation.of((Formula) and(literal("x"), literal(false, "x")))
                .then(ToLiteralClauseList::new)
                .then(SAT4JHasSolutionAnalysis::new)
                .getResult()
                .get());
    }
}
