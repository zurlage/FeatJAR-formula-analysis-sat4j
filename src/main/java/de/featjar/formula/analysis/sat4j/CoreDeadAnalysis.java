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
import de.featjar.formula.analysis.sat4j.solver.SStrategy;
import de.featjar.formula.analysis.sat4j.solver.Sat4JSolutionSolver;
import de.featjar.formula.analysis.solver.SolverContradictionException;
import de.featjar.formula.assignment.VariableAssignment;
import de.featjar.formula.clauses.CNF;
import de.featjar.formula.clauses.LiteralList;
import de.featjar.base.task.Monitor;
import java.util.Arrays;
import org.sat4j.core.VecInt;
import org.sat4j.specs.IteratorInt;

/**
 * Finds core and dead features.
 *
 * @author Sebastian Krieter
 */
public class CoreDeadAnalysis extends VariableAnalysis<LiteralList> {
    protected CoreDeadAnalysis(Computation<CNF> inputComputation, LiteralList variables) { // todo: pass names, not LiteralList, or even VariableAssignment
        super(inputComputation, variables);
    }

    protected CoreDeadAnalysis(Computation<CNF> inputComputation, LiteralList variables, VariableAssignment assumptions, long timeoutInMs, long randomSeed) {
        super(inputComputation, variables, assumptions, timeoutInMs, randomSeed);
    }
    
    @Override
    public FutureResult<LiteralList> compute() {
        return initializeSolver().thenCompute((this::analyze1));
    }

    // currently unused (divide & conquer)
    public LiteralList analyze2(Sat4JSolutionSolver solver, Monitor monitor) throws Exception {
        final int initialAssignmentLength = solver.getAssumptions().size();
        solver.setSelectionStrategy(SStrategy.positive());
        int[] model1 = solver.findSolution().get().getLiterals();

        if (model1 != null) {
            solver.setSelectionStrategy(SStrategy.negative());
            final int[] model2 = solver.findSolution().get().getLiterals();

            if (variables != null) {
                final int[] model3 = new int[model1.length];
                for (int i = 0; i < variables.getLiterals().length; i++) {
                    final int index = variables.getLiterals()[i] - 1;
                    if (index >= 0) {
                        model3[index] = model1[index];
                    }
                }
                model1 = model3;
            }

            for (int i = 0; i < initialAssignmentLength; i++) {
                model1[Math.abs(solver.getAssumptions().peek(i)) - 1] = 0;
            }

            LiteralList.resetConflicts(model1, model2);
            solver.setSelectionStrategy(SStrategy.inverse(model1));

            vars = new VecInt(model1.length);
            split(solver, model1, 0, model1.length);
        }
        return new LiteralList(solver.getAssumptions()
                .asArray(initialAssignmentLength, solver.getAssumptions().size()));
    }

    VecInt vars;

    private void split(Sat4JSolutionSolver solver, int[] model, int start, int end) {
        vars.clear();
        for (int j = start; j < end; j++) {
            final int var = model[j];
            if (var != 0) {
                vars.push(-var);
            }
        }
        switch (vars.size()) {
            case 0:
                return;
            case 1:
                test(solver, model, 0);
                break;
            case 2:
                test(solver, model, 0);
                test(solver, model, 1);
                break;
            default:
                try {
                    solver.getSolverFormula().push(new LiteralList(Arrays.copyOf(vars.toArray(), vars.size())));
                    Result<Boolean> hasSolution = solver.hasSolution();
                    if (Result.of(false).equals(hasSolution)) {
                        foundVariables(solver, model, vars);
                    } else if (Result.empty().equals(hasSolution)) {
                        //reportTimeout();
                    } else if (Result.of(true).equals(hasSolution)) {
                        LiteralList.resetConflicts(model, solver.getInternalSolution());
                        solver.shuffleOrder(random);

                        final int halfLength = (end - start) / 2;
                        if (halfLength > 0) {
                            split(solver, model, start + halfLength, end);
                            split(solver, model, start, start + halfLength);
                        }
                    }
                    solver.getSolverFormula().pop();
                } catch (final SolverContradictionException e) {
                    foundVariables(solver, model, vars);
                }
                break;
        }
    }

    private void test(Sat4JSolutionSolver solver, int[] model, int i) {
        final int var = vars.get(i);
        solver.getAssumptions().push(var);
        Result<Boolean> hasSolution = solver.hasSolution();
        if (Result.of(false).equals(hasSolution)) {
            solver.getAssumptions().replaceLast(-var);
            model[Math.abs(var) - 1] = 0;
        } else if (Result.empty().equals(hasSolution)) {
            solver.getAssumptions().pop();
            //reportTimeout();
        } else if (Result.of(true).equals(hasSolution)) {
            solver.getAssumptions().pop();
            LiteralList.resetConflicts(model, solver.getInternalSolution());
            solver.shuffleOrder(random);
        }
    }

    private void foundVariables(Sat4JSolutionSolver solver, int[] model, VecInt vars) {
        for (final IteratorInt iterator = vars.iterator(); iterator.hasNext(); ) {
            final int var = iterator.next();
            solver.getAssumptions().push(-var);
            model[Math.abs(var) - 1] = 0;
        }
    }

    public LiteralList analyze1(Sat4JSolutionSolver solver, Monitor monitor) {
        final int initialAssignmentLength = solver.getAssumptions().size();
        solver.setSelectionStrategy(SStrategy.positive());
        int[] model1 = solver.findSolution().get().getLiterals();

        if (model1 != null) {
            solver.setSelectionStrategy(SStrategy.inverse(model1));
            final int[] model2 = solver.findSolution().get().getLiterals();

            if (variables != null) {
                final int[] model3 = new int[model1.length];
                for (int i = 0; i < variables.getLiterals().length; i++) {
                    final int index = variables.getLiterals()[i] - 1;
                    if (index >= 0) {
                        model3[index] = model1[index];
                    }
                }
                model1 = model3;
            }

            for (int i = 0; i < initialAssignmentLength; i++) {
                model1[Math.abs(solver.getAssumptions().peek(i)) - 1] = 0;
            }

            LiteralList.resetConflicts(model1, model2);

            for (int i = 0; i < model1.length; i++) {
                final int varX = model1[i];
                if (varX != 0) {
                    solver.getAssumptions().push(-varX);
                    Result<Boolean> hasSolution = solver.hasSolution();
                    if (Result.of(false).equals(hasSolution)) {
                        solver.getAssumptions().replaceLast(varX);
                    } else if (Result.empty().equals(hasSolution)) {
                        solver.getAssumptions().pop();
                        //reportTimeout();
                    } else if (Result.of(true).equals(hasSolution)) {
                        solver.getAssumptions().pop();
                        LiteralList.resetConflicts(model1, solver.getInternalSolution());
                        solver.shuffleOrder(random);
                    }
                }
            }
        }

        return new LiteralList(solver.getAssumptions()
                .asArray(initialAssignmentLength, solver.getAssumptions().size()));
    }
}
