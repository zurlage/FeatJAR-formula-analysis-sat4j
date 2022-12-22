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

import de.featjar.base.computation.Computable;
import de.featjar.base.computation.Dependency;
import de.featjar.base.computation.FutureResult;
import de.featjar.base.data.Pair;
import de.featjar.base.data.Result;
import de.featjar.base.task.Monitor;
import de.featjar.base.tree.structure.Traversable;
import de.featjar.formula.analysis.bool.BooleanAssignment;
import de.featjar.formula.analysis.bool.BooleanClauseList;
import de.featjar.formula.analysis.bool.BooleanSolution;
import de.featjar.formula.analysis.sat4j.solver.SAT4JSolutionSolver;
import de.featjar.formula.analysis.sat4j.solver.SAT4JSolver;
import de.featjar.formula.analysis.sat4j.solver.SelectionStrategy;
import org.sat4j.core.VecInt;
import org.sat4j.specs.IteratorInt;

import java.util.Random;

/**
 * Finds core and dead features.
 * TODO: currently probably broken
 *
 * @author Sebastian Krieter
 */

public class AnalyzeCoreDeadVariablesSAT4J extends SAT4JAnalysis.Solution<BooleanAssignment>
    implements Computable.WithRandom {
    protected final static Dependency<Random> RANDOM = newDependency();

    public AnalyzeCoreDeadVariablesSAT4J(Computable<BooleanClauseList> booleanClauseList) {
        super(booleanClauseList);
    }

    @Override
    public Dependency<Random> getRandomDependency() {
        return RANDOM;
    }

    @Override
    public FutureResult<BooleanAssignment> compute() {
        return Computable.of(computeSolver(), getRandom()).get().thenComputeResult(this::analyze);
    }

    // currently unused (divide & conquer)
//    public SortedIntegerList analyze2(SAT4JSolutionSolver solver, Monitor monitor) throws Exception {
//        final int initialAssignmentLength = solver.getAssignment().size();
//        solver.setSelectionStrategy(SelectionStrategy.positive());
//        int[] model1 = solver.findSolution().get().getIntegers();
//
//        if (model1 != null) {
//            solver.setSelectionStrategy(SelectionStrategy.negative());
//            final int[] model2 = solver.findSolution().get().getIntegers();
//
//            if (variables != null) {
//                final int[] model3 = new int[model1.length];
//                for (int i = 0; i < variables.getIntegers().length; i++) {
//                    final int index = variables.getIntegers()[i] - 1;
//                    if (index >= 0) {
//                        model3[index] = model1[index];
//                    }
//                }
//                model1 = model3;
//            }
//
//            for (int i = 0; i < initialAssignmentLength; i++) {
//                model1[Math.abs(solver.getAssignment().peek(i)) - 1] = 0;
//            }
//
//            SortedIntegerList.resetConflicts(model1, model2);
//            solver.setSelectionStrategy(SelectionStrategy.inverse(model1));
//
//            vars = new VecInt(model1.length);
//            split(solver, model1, 0, model1.length);
//        }
//        return new SortedIntegerList(solver.getAssignment()
//                .asArray(initialAssignmentLength, solver.getAssignment().size()));
//    }

    //VecInt vars;

//    private void split(SAT4JSolutionSolver solver, int[] model, int start, int end) {
//        vars.clear();
//        for (int j = start; j < end; j++) {
//            final int var = model[j];
//            if (var != 0) {
//                vars.push(-var);
//            }
//        }
//        switch (vars.size()) {
//            case 0:
//                return;
//            case 1:
//                test(solver, model, 0);
//                break;
//            case 2:
//                test(solver, model, 0);
//                test(solver, model, 1);
//                break;
//            default:
//                try {
//                    solver.getClauseList().add(new BooleanClause(Arrays.copyOf(vars.toArray(), vars.size())));
//                    Result<Boolean> hasSolution = solver.hasSolution();
//                    if (Result.of(false).equals(hasSolution)) {
//                        foundVariables(solver, model, vars);
//                    } else if (Result.empty().equals(hasSolution)) {
//                        //reportTimeout();
//                    } else if (Result.of(true).equals(hasSolution)) {
//                        SortedIntegerList.resetConflicts(model, solver.getInternalSolution());
//                        solver.shuffleOrder(random);
//
//                        final int halfLength = (end - start) / 2;
//                        if (halfLength > 0) {
//                            split(solver, model, start + halfLength, end);
//                            split(solver, model, start, start + halfLength);
//                        }
//                    }
//                    solver.getSolverFormula().pop();
//                } catch (final SolverContradictionException e) {
//                    foundVariables(solver, model, vars);
//                }
//                break;
//        }
//    }

//    private void test(SAT4JSolutionSolver solver, int[] model, int i) {
//        final int var = vars.get(i);
//        solver.getAssignment().add(var);
//        Result<Boolean> hasSolution = solver.hasSolution();
//        if (Result.of(false).equals(hasSolution)) {
//            solver.getAssignment().replaceLast(-var);
//            model[Math.abs(var) - 1] = 0;
//        } else if (Result.empty().equals(hasSolution)) {
//            solver.getAssignment().remove();
//            //reportTimeout();
//        } else if (Result.of(true).equals(hasSolution)) {
//            solver.getAssignment().remove();
//            SortedIntegerList.resetConflicts(model, solver.getInternalSolution());
//            solver.shuffleOrder(random);
//        }
//    }

    private void foundVariables(SAT4JSolutionSolver solver, int[] model, VecInt vars) {
        for (final IteratorInt iterator = vars.iterator(); iterator.hasNext(); ) {
            final int var = iterator.next();
            solver.getAssignment().add(-var);
            model[Math.abs(var) - 1] = 0;
        }
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public Result<BooleanAssignment> analyze(Pair<SAT4JSolver, Random> pair, Monitor monitor) {
        SAT4JSolutionSolver solver = (SAT4JSolutionSolver) pair.getKey();
        Random random = pair.getValue();
        final int initialAssignmentLength = solver.getAssignment().size();
        solver.setSelectionStrategy(SelectionStrategy.positive());
        Result<BooleanSolution> solution = solver.findSolution();
        if (solution.isEmpty())
            return Result.empty();
        int[] model1 = solution.get().getIntegers();

        if (model1 != null) {
            solver.setSelectionStrategy(SelectionStrategy.inverse(model1));
            solution = solver.findSolution();
            if (solution.isEmpty())
                return Result.empty();
            final int[] model2 = solution.get().getIntegers();

            // TODO: what does this do??
//            if (variables != null) {
//                final int[] model3 = new int[model1.length];
//                for (int i = 0; i < variables.getIntegers().length; i++) {
//                    final int index = variables.getIntegers()[i] - 1;
//                    if (index >= 0) {
//                        model3[index] = model1[index];
//                    }
//                }
//                model1 = model3;
//            }

            for (int i = 0; i < initialAssignmentLength; i++) {
                model1[Math.abs(solver.getAssignment().peek(i)) - 1] = 0;
            }

            model1 = BooleanSolution.resetConflicts(model1, model2);

            for (int i = 0; i < model1.length; i++) {
                final int varX = model1[i];
                if (varX != 0) {
                    solver.getAssignment().add(-varX);
                    Result<Boolean> hasSolution = solver.hasSolution();
                    if (Result.of(false).equals(hasSolution)) {
                        solver.getAssignment().replaceLast(varX);
                    } else if (Result.empty().equals(hasSolution)) {
                        solver.getAssignment().remove();
                        //reportTimeout();
                    } else if (Result.of(true).equals(hasSolution)) {
                        solver.getAssignment().remove();
                        model1 = BooleanSolution.resetConflicts(model1, solver.getSolutionHistory().getLastSolution().get().getIntegers());
                        solver.shuffleOrder(random);
                    }
                }
            }
        }

        return Result.of(solver.getAssignment().toAssignment());
    }

    @Override
    public Traversable<Computable<?>> cloneNode() {
        return new AnalyzeCoreDeadVariablesSAT4J(getInput());
    }
}
