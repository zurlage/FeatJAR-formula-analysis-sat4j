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

import de.featjar.formula.analysis.sat.clause.SATClause;
import de.featjar.formula.analysis.sat.clause.ClauseList;
import de.featjar.formula.analysis.solver.SolverContradictionException;
import de.featjar.formula.analysis.solver.SolverFormula;
import de.featjar.formula.analysis.sat.clause.ToCNF;
import de.featjar.formula.structure.formula.Formula;
import org.sat4j.core.VecInt;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IConstr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Modifiable formula for a {@link Sat4JSolutionSolver}.
 *
 * @author Sebastian Krieter
 */
public class Sat4JFormula extends SolverFormula<IConstr> {

    private final Sat4JSolver<?> solver;

    public Sat4JFormula(Sat4JSolver<?> solver) {
        this.solver = solver;
    }

    protected Sat4JFormula(Sat4JSolver<?> solver, Sat4JFormula oldFormula) {
        super(oldFormula);
        this.solver = solver;
    }

    @Override
    public List<IConstr> push(Formula formula) throws SolverContradictionException {
        return push(ToCNF.convert(formula).get().getClauseList());
    }

    public List<IConstr> push(ClauseList clauses) {
        final ArrayList<IConstr> constrs = new ArrayList<>();
        for (final SATClause sortedIntegerList : clauses.getAll()) {
            try {
                if ((sortedIntegerList.size() == 1) && (sortedIntegerList.getIntegers()[0] == 0)) {
                    throw new ContradictionException();
                }
                final IConstr constr =
                        solver.solver.addClause(new VecInt(Arrays.copyOf(sortedIntegerList.getIntegers(), sortedIntegerList.size())));
                constrs.add(constr);
            } catch (final ContradictionException e) {
                for (final IConstr constr : constrs) {
                    solver.solver.removeConstr(constr);
                }
                throw new SolverContradictionException(e);
            }
        }
        if (solver.SATSolutionHistory != null) {
            solver.SATSolutionHistory.clear();
            solver.lastModel = null;
        }
        this.solverFormulas.addAll(constrs);
        return constrs;
    }

    public IConstr push(SATClause sortedIntegerList) throws SolverContradictionException {
        try {
            if ((sortedIntegerList.size() == 1) && (sortedIntegerList.getIntegers()[0] == 0)) {
                throw new ContradictionException();
            }
            final IConstr constr = solver.solver.addClause(
                    new VecInt(Arrays.copyOfRange(sortedIntegerList.getIntegers(), 0, sortedIntegerList.size())));
            solverFormulas.add(constr);
            if (solver.SATSolutionHistory != null) {
                solver.SATSolutionHistory.clear();
                solver.lastModel = null;
            }
            return constr;
        } catch (final ContradictionException e) {
            throw new SolverContradictionException(e);
        }
    }

    @Override
    public IConstr pop() {
        final IConstr lastConstraint = super.pop();
        solver.solver.removeConstr(lastConstraint);
        return lastConstraint;
    }

    @Override
    public void remove(IConstr clause) {
        if (clause != null) {
            solver.solver.removeConstr(clause);
            super.remove(clause);
        }
    }

    @Override
    public void pop(int count) {
        if (count > solverFormulas.size()) {
            count = solverFormulas.size();
        }
        for (int i = 0; i < count; i++) {
            final IConstr lastConstraint = remove(solverFormulas.size() - 1);
            if (lastConstraint != null) {
                solver.solver.removeSubsumedConstr(lastConstraint);
            }
        }
        solver.solver.clearLearntClauses();
    }
}
