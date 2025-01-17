/*
 * Copyright (C) 2025 FeatJAR-Development-Team
 *
 * This file is part of FeatJAR-formula-analysis-sat4j.
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
package de.featjar.analysis.sat4j.computation;

import de.featjar.analysis.sat4j.solver.ISelectionStrategy;
import de.featjar.analysis.sat4j.solver.SAT4JAssignment;
import de.featjar.analysis.sat4j.solver.SAT4JSolutionSolver;
import de.featjar.base.computation.Computations;
import de.featjar.base.computation.Dependency;
import de.featjar.base.computation.IComputation;
import de.featjar.base.computation.Progress;
import de.featjar.base.data.Result;
import de.featjar.formula.VariableMap;
import de.featjar.formula.assignment.BooleanAssignment;
import de.featjar.formula.assignment.BooleanAssignmentList;
import de.featjar.formula.assignment.BooleanSolution;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Finds atomic sets.
 *
 * @author Sebastian Krieter
 */
public class ComputeAtomicSetsSAT4J extends ASAT4JAnalysis.Solution<BooleanAssignmentList> {

    public static final Dependency<BooleanAssignment> VARIABLES_OF_INTEREST =
            Dependency.newDependency(BooleanAssignment.class);
    public static final Dependency<Boolean> OMIT_SINGLE_SETS = Dependency.newDependency(Boolean.class);
    public static final Dependency<Boolean> OMIT_CORE = Dependency.newDependency(Boolean.class);

    public ComputeAtomicSetsSAT4J(IComputation<BooleanAssignmentList> clauseList) {
        super(
                clauseList,
                Computations.of(new BooleanAssignment()),
                Computations.of(Boolean.FALSE),
                Computations.of(Boolean.FALSE));
    }

    protected ComputeAtomicSetsSAT4J(ComputeAtomicSetsSAT4J other) {
        super(other);
    }

    @Override
    public Result<BooleanAssignmentList> compute(List<Object> dependencyList, Progress progress) {
        SAT4JSolutionSolver solver = initializeSolver(dependencyList);
        Random random = new Random(RANDOM_SEED.get(dependencyList));
        VariableMap variableMap = BOOLEAN_CLAUSE_LIST.get(dependencyList).getVariableMap();

        final BooleanAssignment ignoredVariables;
        BooleanAssignment variables = VARIABLES_OF_INTEREST.get(dependencyList);
        if (variables.isEmpty()) {
            ignoredVariables = new BooleanAssignment();
        } else {
            ignoredVariables = variableMap.getVariables().removeAll(variables);
        }
        boolean omitCore = OMIT_CORE.get(dependencyList);
        boolean omitSingles = OMIT_SINGLE_SETS.get(dependencyList);

        final BooleanAssignmentList atomicSets = new BooleanAssignmentList(variableMap);

        solver.setSelectionStrategy(ISelectionStrategy.positive());
        Result<BooleanSolution> findSolution = solver.findSolution();
        if (findSolution.isEmpty()) {
            return findSolution.merge(Result.empty());
        }

        final int[] model1 = findSolution.get().get();
        if (model1 != null) {
            // initial atomic set consists of core and dead features
            solver.setSelectionStrategy(ISelectionStrategy.inverse(model1));
            final int[] model2 = findSolution.get().get();

            final byte[] done = new byte[model1.length];

            final int[] model1Copy = Arrays.copyOf(model1, model1.length);
            for (int var : ignoredVariables.get()) {
                model1Copy[var - 1] = 0;
                done[var - 1] = 2;
            }

            BooleanSolution.removeConflictsInplace(model1Copy, model2);
            for (int i = 0; i < model1Copy.length; i++) {
                final int varX = model1Copy[i];

                if (varX != 0) {
                    solver.getAssignment().add(-varX);
                    Result<Boolean> hasSolution = solver.hasSolution();
                    if (hasSolution.isEmpty()) {
                        solver.getAssignment().remove();
                    } else if (hasSolution.valueEquals(Boolean.FALSE)) {
                        done[i] = 2;
                        solver.getAssignment().replaceLast(varX);
                    } else if (hasSolution.valueEquals(Boolean.TRUE)) {
                        solver.getAssignment().remove();
                        BooleanSolution.removeConflictsInplace(model1Copy, solver.getInternalSolution());
                        solver.shuffleOrder(random);
                    }
                }
            }
            final int fixedSize = solver.getAssignment().size();
            if (!omitCore) {
                atomicSets.add(new BooleanAssignment(solver.getAssignment().copy(0, fixedSize)));
            }

            solver.setSelectionStrategy(ISelectionStrategy.random(random));

            for (int i = 0; i < model1.length; i++) {
                if (done[i] == 0) {
                    done[i] = 2;

                    int[] xModel0 = Arrays.copyOf(model1, model1.length);

                    final int mx0 = xModel0[i];
                    solver.getAssignment().add(mx0);

                    for (int j = i + 1; j < xModel0.length; j++) {
                        final int my0 = xModel0[j];
                        if ((my0 != 0) && (done[j] == 0)) {
                            solver.getAssignment().add(-my0);

                            Result<Boolean> hasSolution = solver.hasSolution();

                            if (hasSolution.isEmpty()) {
                            } else if (hasSolution.valueEquals(Boolean.FALSE)) {
                                done[j] = 1;
                            } else if (hasSolution.valueEquals(Boolean.TRUE)) {
                                BooleanSolution.removeConflictsInplace(xModel0, solver.getInternalSolution());
                                solver.shuffleOrder(random);
                            }
                            solver.getAssignment().remove();
                        }
                    }

                    solver.getAssignment().remove();
                    solver.getAssignment().add(-mx0);

                    Result<Boolean> hasSolution = solver.hasSolution();
                    if (hasSolution.isEmpty()) {
                        return findSolution.merge(Result.empty());
                    } else if (hasSolution.valueEquals(Boolean.FALSE)) {
                        for (int j = i + 1; j < xModel0.length; j++) {
                            done[j] = 0;
                        }
                    } else if (hasSolution.valueEquals(Boolean.TRUE)) {
                        xModel0 = solver.getInternalSolution();
                    }

                    for (int j = i + 1; j < xModel0.length; j++) {
                        if (done[j] == 1) {
                            final int my0 = xModel0[j];
                            if (my0 != 0) {
                                solver.getAssignment().add(-my0);

                                hasSolution = solver.hasSolution();
                                if (hasSolution.valueEquals(Boolean.FALSE)) {
                                    done[j] = 2;
                                    solver.getAssignment().replaceLast(my0);
                                } else if (hasSolution.isEmpty()) {
                                    return findSolution.merge(Result.empty());
                                } else if (hasSolution.valueEquals(Boolean.TRUE)) {
                                    done[j] = 0;
                                    BooleanSolution.removeConflictsInplace(xModel0, solver.getInternalSolution());
                                    solver.shuffleOrder(random);
                                    solver.getAssignment().remove();
                                }
                            } else {
                                done[j] = 0;
                            }
                        }
                    }
                    SAT4JAssignment assignment = solver.getAssignment();
                    int assignmentSize = solver.getAssignment().size();
                    if (!omitSingles || assignmentSize - fixedSize > 1) {
                        atomicSets.add(new BooleanAssignment(assignment.copy(fixedSize, assignmentSize)));
                    }
                    solver.getAssignment().clear(fixedSize);
                }
            }
        }
        return Result.of(atomicSets);
    }
}
