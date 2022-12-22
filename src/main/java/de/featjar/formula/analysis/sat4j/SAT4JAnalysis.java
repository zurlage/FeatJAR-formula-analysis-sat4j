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
import de.featjar.base.cli.Option;
import de.featjar.base.computation.*;
import de.featjar.formula.analysis.FormulaAnalysis;
import de.featjar.formula.analysis.bool.BooleanAssignment;
import de.featjar.formula.analysis.bool.BooleanClauseList;
import de.featjar.formula.analysis.sat4j.solver.SAT4JExplanationSolver;
import de.featjar.formula.analysis.sat4j.solver.SAT4JSolutionSolver;
import de.featjar.formula.analysis.sat4j.solver.SAT4JSolver;

import java.util.List;
import java.util.function.Consumer;

import static de.featjar.base.computation.Computations.async;

public abstract class SAT4JAnalysis<T> extends Computation<T> implements
        Analysis<BooleanClauseList, T>,
        FormulaAnalysis.WithAssumedAssignment<BooleanAssignment>,
        FormulaAnalysis.WithAssumedClauseList<BooleanClauseList>,
        Computable.WithTimeout {
    protected final static Dependency<BooleanClauseList> BOOLEAN_CLAUSE_LIST = newDependency();
    protected final static Dependency<BooleanAssignment> ASSUMED_ASSIGNMENT = newDependency(new BooleanAssignment());
    protected final static Dependency<BooleanClauseList> ASSUMED_CLAUSE_LIST = newDependency(new BooleanClauseList());
    protected final static Dependency<Long> TIMEOUT = newDependency();

    public SAT4JAnalysis(Computable<BooleanClauseList> booleanClauseList) {
        dependOn(BOOLEAN_CLAUSE_LIST, ASSUMED_ASSIGNMENT, ASSUMED_CLAUSE_LIST, TIMEOUT);
        setInput(booleanClauseList);
    }

    @Override
    public Dependency<BooleanClauseList> getInputDependency() {
        return BOOLEAN_CLAUSE_LIST;
    }

    @Override
    public Dependency<Long> getTimeoutDependency() {
        return TIMEOUT;
    }

    @Override
    public Dependency<BooleanAssignment> getAssumedAssignmentDependency() {
        return ASSUMED_ASSIGNMENT;
    }

    @Override
    public Dependency<BooleanClauseList> getAssumedClauseListDependency() {
        return ASSUMED_CLAUSE_LIST;
    }

    abstract protected SAT4JSolver newSolver(BooleanClauseList clauseList);

    public Computable<SAT4JSolver> computeSolver() {
        return Computable.allOf(getChildren()).mapResult(SAT4JAnalysis.class, "computeSolver", list -> { // caches the solver
                    BooleanClauseList clauseList = (BooleanClauseList) BOOLEAN_CLAUSE_LIST.get(list);
                    BooleanAssignment assumedAssignment = (BooleanAssignment) ASSUMED_ASSIGNMENT.get(list);
                    BooleanClauseList assumedClauseList = (BooleanClauseList) ASSUMED_CLAUSE_LIST.get(list);
                    Long timeout = (Long) TIMEOUT.get(list);
                    Feat.log().debug("initializing SAT4J");
//                    Feat.log().debug(clauseList.toValue().get());
//                    Feat.log().debug("assuming " + assumedAssignment.toValue(clauseList.getVariableMap()).getAndLogProblems());
//                    Feat.log().debug("assuming " + assumedClauseList.toValue().get());
//                    Feat.log().debug(clauseList.getVariableMap());
                    Feat.log().debug(clauseList);
                    Feat.log().debug("assuming " + assumedAssignment);
                    Feat.log().debug("assuming " + assumedClauseList);
                    SAT4JSolver solver = newSolver(clauseList);
                    solver.getClauseList().addAll(assumedClauseList);
                    solver.getAssignment().addAll(assumedAssignment); // todo: the assumed assignment is mutable, so the solver should not be cached?
                    solver.setTimeout(timeout);
                    return solver;
                });
    }

    static abstract class Solution<T> extends SAT4JAnalysis<T> {
        public Solution(Computable<BooleanClauseList> booleanClauseList) {
            super(booleanClauseList);
        }

        @Override
        protected SAT4JSolutionSolver newSolver(BooleanClauseList clauseList) {
            return new SAT4JSolutionSolver(clauseList);
        }
    }

    static abstract class Explanation<T> extends SAT4JAnalysis<T> {
        public Explanation(Computable<BooleanClauseList> booleanClauseList) {
            super(booleanClauseList);
        }

        @Override
        protected SAT4JExplanationSolver newSolver(BooleanClauseList clauseList) {
            return new SAT4JExplanationSolver(clauseList);
        }
    }
}
