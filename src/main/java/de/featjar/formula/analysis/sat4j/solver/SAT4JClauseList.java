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

import de.featjar.formula.analysis.AssignmentList;
import de.featjar.formula.analysis.bool.BooleanClause;
import de.featjar.formula.analysis.bool.BooleanClauseList;
import org.sat4j.core.VecInt;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IConstr;

import java.util.*;

/**
 * ...
 * This class breaks the Liskov principle, as it only allows appending clauses at the end (i.e., implementing
 * an assumption stack) and does not allow for meaningful cloning due to being tied to a solver instance.
 *
 * @author Sebastian Krieter
 * @author Elias Kuiter
 */
public class SAT4JClauseList extends BooleanClauseList {
    protected final SAT4JSolver solver;
    protected final LinkedList<IConstr> addedConstraints = new LinkedList<>();

    public SAT4JClauseList(SAT4JSolver solver) {
        this.solver = solver;
    }

    public SAT4JClauseList(SAT4JSolver solver, int size) {
        super(size);
        this.solver = solver;
    }

    public SAT4JClauseList(SAT4JSolver solver, Collection<? extends BooleanClause> clauses) {
        this.solver = solver;
        assignments.forEach(this::addConstraint);
    }

    public SAT4JClauseList(SAT4JSolver solver, BooleanClauseList other) {
        super(other);
        this.solver = solver;
        assignments.forEach(this::addConstraint);
    }

    @Override
    protected SAT4JClauseList newAssignmentList(List<BooleanClause> clauses) {
        throw new UnsupportedOperationException(); // cannot clone this as it is tied to one solver instance
    }

    @Override
    public void add(int index, BooleanClause clause) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<BooleanClause> remove(int index) {
        throw new UnsupportedOperationException();
    }

    protected void addConstraint(BooleanClause clause) {
        try {
            addedConstraints.add(solver.internalSolver.addClause(
                    new VecInt(Arrays.copyOf(clause.getIntegers(), clause.size()))));
        } catch (final ContradictionException e) {
            throw new RuntimeException(e); // todo: dedicated exception? return result?
        }
    }

    @Override
    public void add(BooleanClause clause) {
        addConstraint(clause);
        super.add(clause);
        solver.getSolutionHistory().clear();
    }

    @Override
    public void addAll(Collection<BooleanClause> clauses) {
        final ArrayList<IConstr> constraints = new ArrayList<>();
        for (final BooleanClause clause : clauses) {
            try {
                constraints.add(solver.internalSolver.addClause(
                        new VecInt(Arrays.copyOf(clause.getIntegers(), clause.size()))));
            } catch (final ContradictionException e) {
                for (final IConstr constraint : constraints) {
                    solver.internalSolver.removeConstr(constraint);
                }
                throw new RuntimeException(e);
            }
        }
        solver.getSolutionHistory().clear();
        addedConstraints.addAll(constraints);
        super.addAll(clauses);
    }

    @Override
    public void addAll(AssignmentList<BooleanClause> clauseList) {
        addAll(clauseList.getAll());
    }

    @Override
    public Optional<BooleanClause> remove() {
        solver.getSolutionHistory().clear();
        if (addedConstraints.size() > 0) {
            final IConstr lastConstraint = addedConstraints.pop();
            solver.internalSolver.removeConstr(lastConstraint);
        }
        return super.remove();
    }

    @Override
    public void clear() {
        solver.getSolutionHistory().clear();
        while (addedConstraints.size() > 0)
            remove();
        super.clear();
    }
}
