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
package de.featjar.formula.analysis.sat4j;

import de.featjar.base.data.Computation;
import de.featjar.base.data.FutureResult;
import de.featjar.formula.analysis.Analysis;
import de.featjar.formula.analysis.bool.BooleanAssignment;
import de.featjar.formula.analysis.bool.BooleanClauseList;
import de.featjar.formula.analysis.sat4j.solver.SAT4JExplanationSolver;
import de.featjar.formula.analysis.sat4j.solver.SAT4JSolutionSolver;
import de.featjar.formula.analysis.sat4j.solver.SAT4JSolver;

import java.util.Optional;

@SuppressWarnings("unchecked")
public abstract class SAT4JAnalysis<T extends SAT4JAnalysis<T, U>, U> implements
        Analysis<BooleanClauseList, U>,
        Analysis.WithTimeout,
        Analysis.WithAssumedAssignment<BooleanAssignment>,
        Analysis.WithAssumedClauseList<BooleanClauseList> {
    protected Computation<BooleanClauseList> clauseListComputation;
    protected Long timeout;
    protected BooleanAssignment assumedAssignment;
    protected BooleanClauseList assumedClauseList;

    @Override
    public Computation<BooleanClauseList> getInputComputation() {
        return clauseListComputation;
    }

    @Override
    public T setInputComputation(Computation<BooleanClauseList> clauseListComputation) {
        this.clauseListComputation = clauseListComputation;
        return (T) this;
    }

    @Override
    public Optional<Long> getTimeout() {
        return Optional.ofNullable(timeout);
    }

    @Override
    public T setTimeout(Long timeout) {
        this.timeout = timeout;
        return (T) this;
    }

    @Override
    public BooleanAssignment getAssumedAssignment() {
        return assumedAssignment;
    }

    @Override
    public T setAssumedAssignment(BooleanAssignment assumedAssignment) {
        this.assumedAssignment = assumedAssignment;
        return (T) this;
    }

    @Override
    public BooleanClauseList getAssumedClauseList() {
        return assumedClauseList;
    }

    @Override
    public T setAssumedClauseList(BooleanClauseList assumedClauseList) {
        this.assumedClauseList = assumedClauseList;
        return (T) this;
    }

    abstract protected SAT4JSolver newSolver(BooleanClauseList clauseList);

    public FutureResult<SAT4JSolver> initializeSolver() {
        return clauseListComputation.get().thenCompute((clauseList, monitor) -> {
            SAT4JSolver solver = newSolver(clauseList);
            solver.setTimeout(timeout);
            solver.getClauseList().addAll(assumedClauseList);
            solver.getAssignment().addAll(assumedAssignment);
            return solver;
        });
    }

    static abstract class Solution<T extends SAT4JAnalysis<T, U>, U> extends SAT4JAnalysis<T, U> {
        @Override
        protected SAT4JSolutionSolver newSolver(BooleanClauseList clauseList) {
            return new SAT4JSolutionSolver(clauseList);
        }
    }

    static abstract class Explanation<T extends SAT4JAnalysis<T, U>, U> extends SAT4JAnalysis<T, U> {
        @Override
        protected SAT4JExplanationSolver newSolver(BooleanClauseList clauseList) {
            return new SAT4JExplanationSolver(clauseList);
        }
    }
}
