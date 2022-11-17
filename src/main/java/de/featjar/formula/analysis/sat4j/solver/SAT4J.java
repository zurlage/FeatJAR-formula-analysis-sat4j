package de.featjar.formula.analysis.sat4j.solver;

import de.featjar.formula.analysis.CountSolutionsAnalysis;
import de.featjar.formula.analysis.GetSolutionAnalysis;
import de.featjar.formula.analysis.GetSolutionsAnalysis;
import de.featjar.formula.analysis.Solver;
import de.featjar.formula.analysis.sat4j.SAT4JCountSolutionsAnalysis;
import de.featjar.formula.analysis.sat4j.SAT4JGetSolutionAnalysis;
import de.featjar.formula.analysis.sat4j.SAT4JGetSolutionsAnalysis;
import de.featjar.formula.analysis.sat4j.SAT4JHasSolutionAnalysis;

public class SAT4J implements Solver<Integer> {
    @Override
    public SAT4JHasSolutionAnalysis hasSolutionAnalysis() {
        return null; //new SAT4JHasSolutionAnalysis();
    }

    @Override
    public GetSolutionAnalysis<?, ?, ?> getSolutionAnalysis() {
        return null; //new SAT4JGetSolutionAnalysis();
    }

    @Override
    public CountSolutionsAnalysis<?, ?> countSolutionsAnalysis() {
        return null; //new SAT4JCountSolutionsAnalysis();
    }

    @Override
    public GetSolutionsAnalysis<?, ?, ?> getSolutionsAnalysis() {
        return null; //new SAT4JGetSolutionsAnalysis();
    }
}
