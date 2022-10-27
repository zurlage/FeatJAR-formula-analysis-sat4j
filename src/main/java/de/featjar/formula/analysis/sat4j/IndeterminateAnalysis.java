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
import de.featjar.base.data.Result;
import de.featjar.formula.analysis.sat4j.solver.Sat4JSolutionSolver;
import de.featjar.formula.analysis.solver.SolverContradictionException;
import de.featjar.formula.assignment.VariableAssignment;
import de.featjar.formula.clauses.CNF;
import de.featjar.formula.clauses.LiteralList;

import java.util.Arrays;
import java.util.List;
import org.sat4j.core.VecInt;

/**
 * Finds indeterminate features.
 *
 * @author Sebastian Krieter
 */
public class IndeterminateAnalysis extends VariableAnalysis<LiteralList> { // todo: variable-analysis does not work
    // reliably (false positives) (use old
    // analysis first?)

    protected IndeterminateAnalysis(Computation<CNF> inputComputation, LiteralList variables) {
        super(inputComputation, variables);
    }

    protected IndeterminateAnalysis(Computation<CNF> inputComputation, LiteralList variables, VariableAssignment assumptions, long timeoutInMs, long randomSeed) {
        super(inputComputation, variables, assumptions, timeoutInMs, randomSeed);
    }

    @Override
    public FutureResult<LiteralList> compute() {
        return initializeSolver().thenCompute(((solver, monitor) -> {
            if (variables == null) {
                variables = LiteralList.getVariables(solver.getCNF().getVariableMap());
            }
            monitor.setTotalSteps(variables.getLiterals().length);

            final VecInt resultList = new VecInt();
            variableLoop:
            for (final int variable : variables.getLiterals()) {
                final Sat4JSolutionSolver modSolver = new Sat4JSolutionSolver(solver.getCNF()); // todo: before, this was passed the variable map?
                final List<LiteralList> clauses = solver.getCNF().getClauses();
                for (final LiteralList clause : clauses) {
                    final LiteralList newClause = clause.removeVariables(variable);
                    if (newClause != null) {
                        try {
                            modSolver.getSolverFormula().push(newClause);
                        } catch (final SolverContradictionException e) {
                            monitor.addStep();
                            continue variableLoop;
                        }
                    } else {
                        monitor.addStep();
                        continue variableLoop;
                    }
                }

                final Result<Boolean> hasSolution = modSolver.hasSolution();
                if (Result.of(false).equals(hasSolution)) {
                } else if (Result.empty().equals(hasSolution)) {
                    //reportTimeout();
                } else if (Result.of(true).equals(hasSolution)) {
                    resultList.push(variable);
                } else {
                    throw new AssertionError(hasSolution);
                }
                monitor.addStep();
            }
            return new LiteralList(Arrays.copyOf(resultList.toArray(), resultList.size()));
        }));
    }
}
