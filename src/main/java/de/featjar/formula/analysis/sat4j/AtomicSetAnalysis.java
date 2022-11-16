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
import de.featjar.formula.assignment.VariableAssignment;
import de.featjar.formula.clauses.CNF;
import de.featjar.formula.clauses.LiteralList;
import de.featjar.base.task.Monitor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Finds atomic sets.
 *
 * @author Sebastian Krieter
 */
public class AtomicSetAnalysis extends Sat4JAnalysis<List<LiteralList>> { // todo: AVariableAnalysis
    public AtomicSetAnalysis(Computation<CNF> inputComputation) {
        super(inputComputation);
    }

    public AtomicSetAnalysis(Computation<CNF> inputComputation, VariableAssignment assumptions, long timeoutInMs, long randomSeed) {
        super(inputComputation, assumptions, timeoutInMs, randomSeed);
    }

    @Override
    public FutureResult<List<LiteralList>> compute() {
        return initializeSolver().thenCompute(((solver, monitor) -> {
            final List<LiteralList> result = new ArrayList<>();
            //		if (variables == null) {
            //			variables = LiteralList.getVariables(solver.getVariables());
            //		}

            // for all variables not in this.variables, set done[...] to 2

            solver.setSelectionStrategy(SStrategy.positive());
            final int[] model1 = solver.findSolution().get().getLiterals();
            final List<LiteralList> solutions = solver.rememberSolutionHistory(1000);

            if (model1 != null) {
                // initial atomic set consists of core and dead features
                solver.setSelectionStrategy(SStrategy.negative());
                final int[] model2 = solver.findSolution().get().getLiterals();
                solver.setSelectionStrategy(SStrategy.positive());

                final byte[] done = new byte[model1.length];

                final int[] model1Copy = Arrays.copyOf(model1, model1.length);

                LiteralList.resetConflicts(model1Copy, model2);
                for (int i = 0; i < model1Copy.length; i++) {
                    final int varX = model1Copy[i];
                    if (varX != 0) {
                        solver.getAssumptions().push(-varX);
                        Result<Boolean> hasSolution = solver.hasSolution();
                        if (Result.of(false).equals(hasSolution)) {
                            done[i] = 2;
                            solver.getAssumptions().replaceLast(varX);
                        } else if (Result.empty().equals(hasSolution)) {
                            solver.getAssumptions().pop();
                            // return Result.empty(new TimeoutException()); // todo: optionally ignore timeout or continue?
                        } else if (Result.of(true).equals(hasSolution)) {
                            solver.getAssumptions().pop();
                            LiteralList.resetConflicts(model1Copy, solver.getInternalSolution());
                            solver.shuffleOrder(random);
                        }
                    }
                }
                final int fixedSize = solver.getAssumptions().size();
                result.add(new LiteralList(solver.getAssumptions().asArray(0, fixedSize)));

                solver.setSelectionStrategy(SStrategy.random(random));

                for (int i = 0; i < model1.length; i++) {
                    if (done[i] == 0) {
                        done[i] = 2;

                        int[] xModel0 = Arrays.copyOf(model1, model1.length);

                        final int mx0 = xModel0[i];
                        solver.getAssumptions().push(mx0);

                        inner:
                        for (int j = i + 1; j < xModel0.length; j++) {
                            final int my0 = xModel0[j];
                            if ((my0 != 0) && (done[j] == 0)) {
                                for (final LiteralList solution : solutions) {
                                    final int mxI = solution.getLiterals()[i];
                                    final int myI = solution.getLiterals()[j];
                                    if ((mx0 == mxI) != (my0 == myI)) {
                                        continue inner;
                                    }
                                }

                                solver.getAssumptions().push(-my0);

                                Result<Boolean> hasSolution = solver.hasSolution();
                                if (Result.of(false).equals(hasSolution)) {
                                    done[j] = 1;
                                } else if (Result.empty().equals(hasSolution)) {
                                    // return Result.empty(new TimeoutException()); // todo: optionally ignore timeout or continue?
                                } else if (Result.of(true).equals(hasSolution)) {
                                    LiteralList.resetConflicts(xModel0, solver.getInternalSolution());
                                    solver.shuffleOrder(random);
                                }
                                solver.getAssumptions().pop();
                            }
                        }

                        solver.getAssumptions().pop();
                        solver.getAssumptions().push(-mx0);

                        Result<Boolean> hasSolution = solver.hasSolution();
                        if (Result.of(false).equals(hasSolution)) {
                        } else if (Result.empty().equals(hasSolution)) {
                            for (int j = i + 1; j < xModel0.length; j++) {
                                done[j] = 0;
                            }
                            // return Result.empty(new TimeoutException()); // todo: optionally ignore timeout or continue?
                        } else if (Result.of(true).equals(hasSolution)) {
                            xModel0 = solver.getInternalSolution();
                        }

                        for (int j = i + 1; j < xModel0.length; j++) {
                            if (done[j] == 1) {
                                final int my0 = xModel0[j];
                                if (my0 != 0) {
                                    solver.getAssumptions().push(-my0);

                                    Result<Boolean> solution = solver.hasSolution();
                                    if (Result.of(false).equals(solution)) {
                                        done[j] = 2;
                                        solver.getAssumptions().replaceLast(my0);
                                    } else if (Result.empty().equals(solution)) {
                                        done[j] = 0;
                                        solver.getAssumptions().pop();
                                        // return Result.empty(new TimeoutException()); // todo: optionally ignore timeout or continue?
                                    } else if (Result.of(true).equals(solution)) {
                                        done[j] = 0;
                                        LiteralList.resetConflicts(xModel0, solver.getInternalSolution());
                                        solver.shuffleOrder(random);
                                        solver.getAssumptions().pop();
                                    }
                                } else {
                                    done[j] = 0;
                                }
                            }
                        }

                        result.add(new LiteralList(solver.getAssumptions()
                                .asArray(fixedSize, solver.getAssumptions().size())));
                        solver.getAssumptions().clear(fixedSize);
                    }
                }
            }
            return result;
        }));
    }
}
