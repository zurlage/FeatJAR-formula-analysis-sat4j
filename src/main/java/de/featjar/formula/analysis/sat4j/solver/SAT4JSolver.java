/*
 * Copyright (C) 2022 Sebastian Krieter
 *
 * This file is part of formula-analysis-sat4j.
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
package de.featjar.formula.analysis.sat4j.solver;

import de.featjar.base.data.Result;
import de.featjar.formula.analysis.bool.BooleanAssignment;
import de.featjar.formula.analysis.bool.BooleanClauseList;
import de.featjar.formula.analysis.bool.BooleanSolution;
import org.sat4j.core.VecInt;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;

import java.util.Optional;

/**
 * Base class for solvers using Sat4J.
 *
 * @author Sebastian Krieter
 * @author Elias Kuiter
 */
public abstract class SAT4JSolver {
    protected final ISolver internalSolver;
    protected final SAT4JClauseList clauseList;
    protected final SAT4JAssignment assignment = new SAT4JAssignment();
    protected SolutionHistory solutionHistory = new SolutionHistory.RememberUpTo(1000);
    private Long timeout;
    protected boolean globalTimeout;

    public SAT4JSolver(BooleanClauseList clauseList) {
        internalSolver = newInternalSolver();
        internalSolver.setDBSimplificationAllowed(true);
        internalSolver.setKeepSolverHot(true);
        internalSolver.setVerbose(false);
        this.clauseList = new SAT4JClauseList(this, clauseList);

        final int size = clauseList.getVariableMap().getVariableCount();
        try {
            if (!clauseList.isEmpty()) {
                internalSolver.setExpectedNumberOfClauses(clauseList.size() + 1);
            }
            // todo: what does this do?
            if (size > 0) {
                final VecInt pseudoClause = new VecInt(size + 1);
                for (int i = 1; i <= size; i++) {
                    pseudoClause.push(i);
                }
                pseudoClause.push(-1);
                internalSolver.addClause(pseudoClause);
            }
        } catch (final Exception e) {
            throw new RuntimeException(e); // todo: contradiction exception?
        }
    }

    protected abstract ISolver newInternalSolver();

    public SAT4JClauseList getClauseList() {
        return clauseList;
    }

    public SAT4JAssignment getAssignment() {
        return assignment;
    }

    public SolutionHistory getSolutionHistory() {
        return solutionHistory;
    }

    public void setSolutionHistory(SolutionHistory solutionHistory) {
        this.solutionHistory = solutionHistory;
    }

    public Optional<Long> getTimeout() {
        return Optional.ofNullable(timeout);
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
        internalSolver.setTimeoutMs(timeout);
    }

    public boolean isGlobalTimeout() {
        return globalTimeout;
    }

    public void setGlobalTimeout(boolean globalTimeout) {
        this.globalTimeout = globalTimeout;
    }

    public Result<BooleanSolution> findSolution() {
        return hasSolution().equals(Result.of(true)) ? Result.ofOptional(solutionHistory.getLastSolution()) : Result.empty();
    }

    protected Result<Boolean> hasSolution(VecInt integers) {
        for (final BooleanSolution solution : solutionHistory) {
            if (solution.containsAll(integers.toArray())) {
                solutionHistory.setLastSolution(solution);
                return Result.of(true);
            }
        }

        try {
            if (internalSolver.isSatisfiable(integers, globalTimeout)) {
                BooleanSolution solution = new BooleanSolution(internalSolver.model());
                solutionHistory.addNewSolution(solution);
                return Result.of(true);
            } else {
                solutionHistory.setLastSolution(null);
                return Result.of(false);
            }
        } catch (final TimeoutException e) {
            solutionHistory.setLastSolution(null);
            return Result.empty();
        }
    }

    public Result<Boolean> hasSolution() {
        return hasSolution(assignment.getIntegers());
    }

    /**
     * Does only consider the given {@code assignment} and <b>not</b> the global
     * assignment variable of the solver.
     */
    public Result<Boolean> hasSolution(BooleanAssignment assignment) {
        return hasSolution(new VecInt(assignment.getIntegers()));
    }
}
