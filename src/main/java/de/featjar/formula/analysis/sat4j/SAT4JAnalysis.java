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

import de.featjar.base.Feat;
import de.featjar.base.data.Computation;
import de.featjar.base.data.FutureResult;
import de.featjar.formula.analysis.Analysis;
import de.featjar.formula.analysis.bool.BooleanAssignment;
import de.featjar.formula.analysis.bool.BooleanClauseList;
import de.featjar.formula.analysis.sat4j.solver.SAT4JExplanationSolver;
import de.featjar.formula.analysis.sat4j.solver.SAT4JSolutionSolver;
import de.featjar.formula.analysis.sat4j.solver.SAT4JSolver;
import de.featjar.formula.analysis.value.ValueAssignment;
import de.featjar.formula.analysis.value.ValueClauseList;

import java.util.Optional;

@SuppressWarnings("unchecked")
public abstract class SAT4JAnalysis<T extends SAT4JAnalysis<T, U>, U> implements
        Analysis<BooleanClauseList, U>,
        Analysis.WithAssumedAssignment<BooleanAssignment>,
        Analysis.WithAssumedClauseList<BooleanClauseList>,
        Analysis.WithTimeout {
    protected Computation<BooleanClauseList> clauseListComputation;
    protected Computation<BooleanAssignment> assumedAssignmentComputation = Computation.of(new BooleanAssignment());
    protected Computation<BooleanClauseList> assumedClauseListComputation = Computation.of(new BooleanClauseList());
    protected Long timeout;

    public SAT4JAnalysis(Computation<BooleanClauseList> clauseListComputation) {
        this.clauseListComputation = clauseListComputation;
    }

    @Override
    public Computation<BooleanClauseList> getInput() {
        return clauseListComputation;
    }

    @Override
    public T setInput(Computation<BooleanClauseList> clauseListComputation) {
        this.clauseListComputation = clauseListComputation;
        return (T) this;
    }

    @Override
    public Computation<BooleanAssignment> getAssumedAssignment() {
        return assumedAssignmentComputation;
    }

    @Override
    public T setAssumedAssignment(Computation<BooleanAssignment> assumedAssignmentComputation) {
        this.assumedAssignmentComputation = assumedAssignmentComputation;
        return (T) this;
    }

    public T setAssumedValueAssignment(Computation<ValueAssignment> valueAssignmentComputation) {
        return setAssumedAssignment(Computation.allOf(getInput(), valueAssignmentComputation)
                .mapResult(pair -> {
                    BooleanClauseList clauseList = pair.getKey();
                    ValueAssignment valueAssignment = pair.getValue();
                    // todo: this ignores warnings besides logging. is this a good idea?
                    return valueAssignment.toBoolean(clauseList.getVariableMap()).getAndLogProblems();
                }));
    }

    @Override
    public Computation<BooleanClauseList> getAssumedClauseList() {
        return assumedClauseListComputation;
    }

    @Override
    public T setAssumedClauseList(Computation<BooleanClauseList> assumedClauseListComputation) {
        this.assumedClauseListComputation = assumedClauseListComputation;
        return (T) this;
    }

    public T setAssumedValueClauseList(Computation<ValueClauseList> valueClauseListComputation) {
        return setAssumedClauseList(Computation.allOf(getInput(), valueClauseListComputation)
                .mapResult(pair -> {
                    BooleanClauseList clauseList = pair.getKey();
                    ValueClauseList valueClauseList = pair.getValue();
                    // todo: this ignores warnings besides logging. is this a good idea?
                    return valueClauseList.toBoolean(clauseList.getVariableMap()).getAndLogProblems();
                }));
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

    abstract protected SAT4JSolver newSolver(BooleanClauseList clauseList);

    public FutureResult<SAT4JSolver> initializeSolver() {
        return Computation.allOf(clauseListComputation, assumedAssignmentComputation, assumedClauseListComputation)
                .get().thenCompute((list, monitor) -> {
                    BooleanClauseList clauseList = (BooleanClauseList) list.get(0);
                    BooleanAssignment assumedAssignment = (BooleanAssignment) list.get(1);
                    BooleanClauseList assumedClauseList = (BooleanClauseList) list.get(2);
                    Feat.log().debug("initializing SAT4J");
                    Feat.log().debug(clauseList.toValue().get());
                    Feat.log().debug(assumedAssignment.toValue(clauseList.getVariableMap()).getAndLogProblems());
                    Feat.log().debug(assumedClauseList.toValue().get());
                    Feat.log().debug(clauseList);
                    Feat.log().debug(assumedAssignment);
                    Feat.log().debug(assumedClauseList);
                    SAT4JSolver solver = newSolver(clauseList);
                    solver.setTimeout(timeout);
                    solver.getClauseList().addAll(assumedClauseList);
                    solver.getAssignment().addAll(assumedAssignment);
                    return solver;
                });
    }

    static abstract class Solution<T extends SAT4JAnalysis<T, U>, U> extends SAT4JAnalysis<T, U> {
        public Solution(Computation<BooleanClauseList> clauseListComputation) {
            super(clauseListComputation);
        }

        @Override
        protected SAT4JSolutionSolver newSolver(BooleanClauseList clauseList) {
            return new SAT4JSolutionSolver(clauseList);
        }
    }

    static abstract class Explanation<T extends SAT4JAnalysis<T, U>, U> extends SAT4JAnalysis<T, U> {
        public Explanation(Computation<BooleanClauseList> clauseListComputation) {
            super(clauseListComputation);
        }

        @Override
        protected SAT4JExplanationSolver newSolver(BooleanClauseList clauseList) {
            return new SAT4JExplanationSolver(clauseList);
        }
    }
}
