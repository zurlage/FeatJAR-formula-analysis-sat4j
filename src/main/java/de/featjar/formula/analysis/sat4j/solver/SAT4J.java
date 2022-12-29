package de.featjar.formula.analysis.sat4j.solver;

import de.featjar.formula.analysis.*;

public class SAT4J implements ISolver<Integer> {
    @Override
    public IHasSolutionAnalysis<?, ?> hasSolutionAnalysis() {
        return null; //new SAT4JHasSolutionAnalysis();
    }

    @Override
    public IGetSolutionAnalysis<?, ?, ?> getSolutionAnalysis() {
        return null; //new SAT4JGetSolutionAnalysis();
    }

    @Override
    public ICountSolutionsAnalysis<?, ?> countSolutionsAnalysis() {
        return null; //new SAT4JCountSolutionsAnalysis();
    }

    @Override
    public IGetSolutionsAnalysis<?, ?, ?> getSolutionsAnalysis() {
        return null; //new SAT4JGetSolutionsAnalysis();
    }
}
