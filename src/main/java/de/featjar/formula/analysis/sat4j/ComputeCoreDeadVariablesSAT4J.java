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

import de.featjar.base.computation.*;
import de.featjar.base.data.Result;
import de.featjar.base.tree.structure.ITree;
import de.featjar.formula.analysis.bool.BooleanAssignment;
import de.featjar.formula.analysis.bool.BooleanClauseList;
import de.featjar.formula.analysis.bool.BooleanSolution;
import de.featjar.formula.analysis.sat4j.solver.ISelectionStrategy;
import de.featjar.formula.analysis.sat4j.solver.SAT4JSolutionSolver;
import java.util.Random;

/**
 * Finds core and dead features.
 *
 * @author Sebastian Krieter
 */
public class ComputeCoreDeadVariablesSAT4J extends ASAT4JAnalysis.Solution<BooleanAssignment>
        implements IRandomDependency {
    protected static final Dependency<Random> RANDOM =
            newOptionalDependency(new Random(IRandomDependency.DEFAULT_RANDOM_SEED));
    protected static final Dependency<BooleanAssignment> VARIABLES_OF_INTEREST =
            newOptionalDependency(new BooleanAssignment());

    public ComputeCoreDeadVariablesSAT4J(IComputation<BooleanClauseList> booleanClauseList) {
        super(booleanClauseList, RANDOM, VARIABLES_OF_INTEREST);
    }

    @Override
    public Dependency<Random> getRandomDependency() {
        return RANDOM;
    }

    public Dependency<BooleanAssignment> getVariablesOfInterest() {
        return VARIABLES_OF_INTEREST;
    }

    @Override
    public Result<BooleanAssignment> compute(DependencyList dependencyList, Progress progress) {
        SAT4JSolutionSolver solver = initializeSolver(dependencyList);
        Random random = dependencyList.get(RANDOM);
        BooleanAssignment variablesOfInterest = dependencyList.get(VARIABLES_OF_INTEREST);
        final int initialAssignmentLength = solver.getAssignment().size();
        solver.setSelectionStrategy(ISelectionStrategy.positive());
        Result<BooleanSolution> solution = solver.findSolution();
        if (solution.isEmpty()) return Result.empty();
        int[] model1 = solution.get().getIntegers();

        if (model1 != null) {
            solver.setSelectionStrategy(ISelectionStrategy.inverse(model1));
            solution = solver.findSolution();
            if (solution.isEmpty()) return Result.empty();
            final int[] model2 = solution.get().getIntegers();

            if (!variablesOfInterest.isEmpty()) {
                final int[] model3 = new int[model1.length];
                for (int i = 0; i < variablesOfInterest.getIntegers().length; i++) {
                    final int index = variablesOfInterest.getIntegers()[i] - 1;
                    if (index >= 0) {
                        model3[index] = model1[index];
                    }
                }
                model1 = model3;
            }

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
                    } else if (Result.of(true).equals(hasSolution)) {
                        solver.getAssignment().remove();
                        model1 = BooleanSolution.resetConflicts(model1, solver.getInternalSolution());
                        solver.shuffleOrder(random);
                    }
                }
            }
        }

        return solver.createResult(solver.getAssignment().toAssignment());
    }

    @Override
    public ITree<IComputation<?>> cloneNode() {
        return new ComputeCoreDeadVariablesSAT4J(getInput());
    }
}
