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

import de.featjar.base.computation.IComputation;
import de.featjar.base.computation.Dependency;
import de.featjar.base.computation.IRandomDependency;
import de.featjar.base.data.Result;
import de.featjar.base.computation.Progress;
import de.featjar.base.tree.structure.ITree;
import de.featjar.formula.analysis.bool.BooleanAssignment;
import de.featjar.formula.analysis.bool.BooleanClauseList;
import de.featjar.formula.analysis.bool.BooleanSolution;
import de.featjar.formula.analysis.sat4j.solver.SAT4JSolutionSolver;
import de.featjar.formula.analysis.sat4j.solver.ISelectionStrategy;
import org.sat4j.core.VecInt;
import org.sat4j.specs.IteratorInt;

import java.util.List;
import java.util.Random;

/**
 * Finds core and dead features.
 *
 * @author Sebastian Krieter
 */

public class ComputeCoreDeadVariablesSAT4J extends ASAT4JAnalysis.Solution<BooleanAssignment>
    implements IRandomDependency {
    protected final static Dependency<Random> RANDOM = newOptionalDependency(new Random(IRandomDependency.DEFAULT_RANDOM_SEED));

    public ComputeCoreDeadVariablesSAT4J(IComputation<BooleanClauseList> booleanClauseList) {
        super(booleanClauseList, RANDOM);
    }

    @Override
    public Dependency<Random> getRandomDependency() {
        return RANDOM;
    }

    @Override
    public Result<BooleanAssignment> computeResult(List<?> results, Progress progress) {
        return analyze(initializeSolver(results), RANDOM.get(results), progress);
    }

    private void foundVariables(SAT4JSolutionSolver solver, int[] model, VecInt vars) {
        for (final IteratorInt iterator = vars.iterator(); iterator.hasNext(); ) {
            final int var = iterator.next();
            solver.getAssignment().add(-var);
            model[Math.abs(var) - 1] = 0;
        }
    }

    public Result<BooleanAssignment> analyze(SAT4JSolutionSolver solver, Random random, Progress progress) {
        final int initialAssignmentLength = solver.getAssignment().size();
        solver.setSelectionStrategy(ISelectionStrategy.positive());
        Result<BooleanSolution> solution = solver.findSolution();
        if (solution.isEmpty())
            return Result.empty();
        int[] model1 = solution.get().getIntegers();

        if (model1 != null) {
            solver.setSelectionStrategy(ISelectionStrategy.inverse(model1));
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
    public ITree<IComputation<?>> cloneNode() {
        return new ComputeCoreDeadVariablesSAT4J(getInput());
    }
}
