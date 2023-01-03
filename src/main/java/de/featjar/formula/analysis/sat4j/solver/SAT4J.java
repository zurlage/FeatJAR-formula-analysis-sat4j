package de.featjar.formula.analysis.sat4j.solver;

import de.featjar.formula.analysis.*;

public class SAT4J implements ISolver<Integer> {
    @Override
    public ISolutionAnalysis<?, ?, ?> getSolutionAnalysis() {
        return null; //new SAT4JGetSolutionAnalysis();
    }

    @Override
    public ISolutionCountAnalysis<?, ?> countSolutionsAnalysis() {
        return null; //new SAT4JCountSolutionsAnalysis();
    }

    @Override
    public ISolutionsAnalysis<?, ?, ?> getSolutionsAnalysis() {
        return null; //new SAT4JGetSolutionsAnalysis();
    }
}
