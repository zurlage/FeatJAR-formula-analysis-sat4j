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
package de.featjar.formula.analysis.sat4j.todo.twise;

import de.featjar.formula.analysis.todo.mig.solver.visitor.DefaultVisitor;
import de.featjar.formula.analysis.todo.mig.solver.visitor.Traverser;
import de.featjar.formula.analysis.todo.mig.solver.visitor.Visitor;
import de.featjar.formula.analysis.sat4j.solver.ISelectionStrategy;
import de.featjar.formula.analysis.sat4j.solver.SAT4JSolutionSolver;

import java.util.Arrays;
import org.sat4j.core.VecInt;

/**
 * Represent a solution within a covering array.
 *
 * @author Sebastian Krieter
 */
public class TWiseConfiguration extends SortedIntegerList {


    public static final byte SELECTION_IMPOSSIBLE = 1;
    public static final byte SELECTION_SELECTED = 2;

    public static int SOLUTION_COUNT_THRESHOLD = 10;

    protected VecInt solutionLiterals;

    protected int countLiterals, rank = 0;

    protected final int numberOfVariableLiterals;
    protected final TWiseConfigurationUtil util;
    protected Traverser traverser;
    protected Visitor<?> visitor;

    protected VecInt solverSolutionIndex = new VecInt();

    private class DPVisitor extends DefaultVisitor {

        private int[] unknownValues = null;

        @Override
        public VisitResult visitStrong(int curLiteral) {
            addLiteral(curLiteral);
            if (unknownValues != null) {
                util.getSolver().getAssignment().add(curLiteral);
                unknownValues[Math.abs(curLiteral) - 1] = 0;
            }
            return VisitResult.Continue;
        }

        @Override
        public final VisitResult visitWeak(final int curLiteral) {
            if (unknownValues == null) {
                final SAT4JSolutionSolver solver = util.getSolver();
                setUpSolver(solver);
                solver.setSelectionStrategy(ISelectionStrategy.original());
                switch (solver.hasSolution()) {
                    case FALSE:
                        return VisitResult.Cancel;
                    case TIMEOUT:
                        throw new RuntimeException();
                    case TRUE:
                        unknownValues = solver.getInternalSolution();
                        util.addSolverSolution(Arrays.copyOf(unknownValues, unknownValues.length));
                        solver.shuffleOrder(util.random);
                        break;
                    default:
                        throw new RuntimeException();
                }
                if (unknownValues != null) {
                    solver.setSelectionStrategy(ISelectionStrategy.inverse(unknownValues));
                    solver.hasSolution();
                    final int[] model2 = solver.getInternalSolution();
                    util.addSolverSolution(Arrays.copyOf(model2, model2.length));

                    SortedIntegerList.resetConflicts(unknownValues, model2);

                    final int[] literals = TWiseConfiguration.this.integers;
                    for (int k = 0; k < literals.length; k++) {
                        final int var = literals[k];
                        if ((var != 0) && (unknownValues[k] != 0)) {
                            unknownValues[k] = 0;
                        }
                    }
                } else {
                    throw new RuntimeException();
                }
            }
            return sat(unknownValues, curLiteral) ? VisitResult.Select : VisitResult.Continue;
        }

        private final boolean sat(final int[] unknownValues, final int curLiteral) {
            final int i = Math.abs(curLiteral) - 1;
            if (unknownValues[i] == curLiteral) {
                final SAT4JSolutionSolver solver = util.getSolver();
                solver.getAssignment().add(-curLiteral);
                switch (solver.hasSolution()) {
                    case FALSE:
                        solver.getAssignment().replaceLast(curLiteral);
                        unknownValues[i] = 0;
                        return true;
                    case TIMEOUT:
                        solver.getAssignment().remove();
                        unknownValues[i] = 0;
                        break;
                    case TRUE:
                        solver.getAssignment().remove();
                        final int[] solution2 = solver.getInternalSolution();
                        util.addSolverSolution(Arrays.copyOf(solution2, solution2.length));
                        SortedIntegerList.resetConflicts(unknownValues, solution2);
                        solver.shuffleOrder(util.random);
                        break;
                }
            }
            return false;
        }
    }

    public TWiseConfiguration(TWiseConfigurationUtil util) {
        super(new int[util.getCnf().getVariableMap().getVariableCount()], Order.INDEX, false);
        countLiterals = 0;
        this.util = util;
        if (util.hasSolver()) {
            for (final int var : util.getDeadCoreFeatures().getIntegers()) {
                integers[Math.abs(var) - 1] = var;
                countLiterals++;
            }
            numberOfVariableLiterals = integers.length - countLiterals;
            solutionLiterals = new VecInt(numberOfVariableLiterals);
            countLiterals = 0;
            if (util.hasMig()) {
                traverser = new Traverser(util.getMig());
                traverser.setModel(integers);
                visitor = new DefaultVisitor() {
                    @Override
                    public VisitResult visitStrong(int curLiteral) {
                        addLiteral(curLiteral);
                        return super.visitStrong(curLiteral);
                    }
                };
            } else {
                traverser = null;
                visitor = null;
            }
        } else {
            traverser = null;
            visitor = null;
            numberOfVariableLiterals = integers.length - countLiterals;
            solutionLiterals = new VecInt(numberOfVariableLiterals);
        }
    }

    public TWiseConfiguration(TWiseConfiguration other) {
        super(other);
        util = other.util;

        numberOfVariableLiterals = other.numberOfVariableLiterals;
        solverSolutionIndex = other.solverSolutionIndex;
        countLiterals = other.countLiterals;
        rank = other.rank;

        if (util.hasSolver()) {
            if (other.solutionLiterals != null) {
                solutionLiterals = new VecInt(numberOfVariableLiterals);
                other.solutionLiterals.copyTo(solutionLiterals);
            }
            if (util.hasMig()) {
                traverser = new Traverser(util.getMig());
                traverser.setModel(integers);

                visitor = new DefaultVisitor() {

                    @Override
                    public VisitResult visitStrong(int curLiteral) {
                        addLiteral(curLiteral);
                        return super.visitStrong(curLiteral);
                    }
                };
            } else {
                traverser = null;
                visitor = null;
            }
        } else {
            traverser = null;
            visitor = null;
        }
    }

    private void addLiteral(int curLiteral) {
        newLiteral(curLiteral);
    }

    private void newLiteral(int curLiteral) {
        countLiterals++;
        solutionLiterals.push(curLiteral);
        final int k = Math.abs(curLiteral) - 1;

        for (int i = 0; i < solverSolutionIndex.size(); i++) {
            if (util.getSolverSolution(solverSolutionIndex.get(i)).getIntegers()[k] == -curLiteral) {
                solverSolutionIndex.delete(i--);
            }
        }
    }

    public void setLiteral(int literal) {
        if (traverser != null) {
            traverser.setVisitor(visitor);
            traverser.traverseStrong(integers);
        } else {
            final int i = Math.abs(literal) - 1;
            if (integers[i] == 0) {
                integers[i] = literal;
                newLiteral(literal);
            }
        }
    }

    public void setLiteral(int... literals) {
        if (traverser != null) {
            traverser.setVisitor(visitor);
            traverser.traverseStrong(literals);
        } else {
            for (final int literal : literals) {
                final int i = Math.abs(literal) - 1;
                if (this.integers[i] == 0) {
                    this.integers[i] = literal;
                    newLiteral(literal);
                }
            }
        }
    }

    public void propagation() {
        final SAT4JSolutionSolver solver = util.getSolver();
        final int orgAssignmentSize;
        if (traverser != null) {
            final DPVisitor visitor = new DPVisitor();

            final int[] literals = Arrays.copyOf(solutionLiterals.toArray(), solutionLiterals.size());
            for (int i = 0, length = literals.length; i < length; i++) {
                this.integers[Math.abs(literals[i]) - 1] = 0;
            }
            solutionLiterals.clear();
            countLiterals = 0;

            orgAssignmentSize = solver.getAssignment().size();
            traverser.setVisitor(visitor);
            traverser.traverse(literals);
        } else {
            orgAssignmentSize = setUpSolver(solver);

            solver.setSelectionStrategy(ISelectionStrategy.original());
            final int[] firstSolution = solver.findSolution().getLiterals();
            if (firstSolution != null) {
                util.addSolverSolution(Arrays.copyOf(firstSolution, firstSolution.length));
                solver.setSelectionStrategy(ISelectionStrategy.inverse(firstSolution));
                util.getSolver().hasSolution();
                final int[] secondSolution = util.getSolver().getInternalSolution();
                util.addSolverSolution(Arrays.copyOf(secondSolution, secondSolution.length));

                SortedIntegerList.resetConflicts(firstSolution, secondSolution);
                for (final int literal : integers) {
                    if (literal != 0) {
                        firstSolution[Math.abs(literal) - 1] = 0;
                    }
                }

                for (int i = 0; i < firstSolution.length; i++) {
                    final int varX = firstSolution[i];
                    if (varX != 0) {
                        solver.getAssignment().add(-varX);
                        switch (solver.hasSolution()) {
                            case FALSE:
                                solver.getAssignment().replaceLast(varX);
                                setLiteral(varX);
                                break;
                            case TIMEOUT:
                                solver.getAssignment().remove();
                                break;
                            case TRUE:
                                solver.getAssignment().remove();
                                final int[] solution = solver.getInternalSolution();
                                util.addSolverSolution(Arrays.copyOf(solution, solution.length));
                                SortedIntegerList.resetConflicts(firstSolution, solution);
                                solver.shuffleOrder(util.random);
                                break;
                        }
                    }
                }
            }
        }
        solver.getAssignment().clear(orgAssignmentSize);
        solver.setSelectionStrategy(ISelectionStrategy.random(util.random));
    }

    public void clear() {
        traverser = null;
        visitor = null;
        solutionLiterals = null;
        solverSolutionIndex = null;
    }

    public boolean isComplete() {
        return countLiterals == numberOfVariableLiterals;
    }

    public int countLiterals() {
        return countLiterals;
    }

    public void autoComplete() {
        if (!isComplete()) {
            if (util.hasSolver()) {
                if (solverSolutionIndex.isEmpty()) {
                    final SAT4JSolutionSolver solver = util.getSolver();
                    final int orgAssignmentSize = setUpSolver(solver);
                    try {
                        if (solver.hasSolution() == SATSolver.Result.TRUE) {
                            System.arraycopy(solver.getInternalSolution(), 0, integers, 0, integers.length);
                        }
                    } finally {
                        solver.getAssignment().clear(orgAssignmentSize);
                    }
                } else {
                    System.arraycopy(
                            util.getSolverSolution(solverSolutionIndex.last()).getIntegers(),
                            0,
                            integers,
                            0,
                            integers.length);
                    solverSolutionIndex.clear();
                }
            } else {
                for (int i = 0; i < integers.length; i++) {
                    if (integers[i] == 0) {
                        integers[i] = -(i + 1);
                    }
                }
            }
            countLiterals = numberOfVariableLiterals;
        }
    }

    public SortedIntegerList getCompleteSolution() {
        if (isComplete()) {
            return new SortedIntegerList(this);
        } else {
            final int[] s;
            if (util.hasSolver()) {
                if (solverSolutionIndex.isEmpty()) {
                    final SAT4JSolutionSolver solver = util.getSolver();
                    final int orgAssignmentSize = setUpSolver(solver);
                    try {
                        final SATSolver.Result<Boolean> Result<Boolean> = solver.hasSolution();
                        switch (Result<Boolean>) {
                            case FALSE:
                                throw new RuntimeException("Solution Invalid!");
                            case TIMEOUT:
                                throw new RuntimeException("SatSolver Timeout!");
                            case TRUE:
                                s = solver.getInternalSolution();
                                break;
                            default:
                                throw new RuntimeException(Result.toString());
                        }
                    } finally {
                        solver.getAssignment().clear(orgAssignmentSize);
                    }
                } else {
                    s = util.getSolverSolution(solverSolutionIndex.last()).getIntegers();
                    if (s == null) {
                        throw new RuntimeException();
                    }
                }
            } else {
                s = Arrays.copyOf(integers, integers.length);
                for (int i = 0; i < s.length; i++) {
                    if (s[i] == 0) {
                        s[i] = -(i + 1);
                    }
                }
            }
            return (s == null) ? null : new SortedIntegerList(Arrays.copyOf(s, s.length), Order.INDEX, false);
        }
    }

    public void generateRandomSolutions(int count) {
        final SAT4JSolutionSolver solver = util.getSolver();
        solver.setSelectionStrategy(ISelectionStrategy.random(util.random));
        final int orgAssignmentSize = setUpSolver(solver);
        try {
            for (int i = 0; i < count; i++) {
                solver.hasSolution();
                final int[] randomSolution = solver.getInternalSolution();
                util.addSolverSolution(Arrays.copyOf(randomSolution, randomSolution.length));
                solver.shuffleOrder(util.random);
            }
        } finally {
            solver.getAssignment().clear(orgAssignmentSize);
        }
    }

    public boolean isValid() {
        final SAT4JSolutionSolver solver = util.getSolver();
        final ISelectionStrategy selectionStrategy = solver.getSelectionStrategy();
        final int orgAssignmentSize = setUpSolver(solver);
        solver.setSelectionStrategy(ISelectionStrategy.original());
        try {
            return solver.hasSolution() == SATSolver.Result.TRUE;
        } finally {
            solver.getAssignment().clear(orgAssignmentSize);
            solver.setSelectionStrategy(selectionStrategy);
        }
    }

    public int setUpSolver(final SAT4JSolutionSolver solver) {
        final int orgAssignmentSize = solver.getAssignment().size();
        if (isComplete()) {
            for (int i = 0; i < integers.length; i++) {
                solver.getAssignment().add(integers[i]);
            }
        } else {
            final int[] array = solutionLiterals.toArray();
            for (int i = 0, length = solutionLiterals.size(); i < length; i++) {
                solver.getAssignment().add(array[i]);
            }
        }
        return orgAssignmentSize;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public void updateSolverSolutions() {
        if (util.hasSolver() && (solutionLiterals != null)) {
            solverSolutionIndex.clear();
            final int[] array = solutionLiterals.toArray();
            final SortedIntegerList[] solverSolutions = util.getSolverSolutions();
            solutionLoop:
            for (int i = 0; i < solverSolutions.length; i++) {
                final SortedIntegerList solverSolution = solverSolutions[i];
                if (solverSolution == null) {
                    break;
                }
                final int[] solverSolutionLiterals = solverSolution.getIntegers();
                for (int j = 0, length = solutionLiterals.size(); j < length; j++) {
                    final int k = Math.abs(array[j]) - 1;
                    if (solverSolutionLiterals[k] == -integers[k]) {
                        continue solutionLoop;
                    }
                }
                solverSolutionIndex.push(i);
            }
        }
    }

    public void updateSolverSolutions(int[] solverSolution, int index) {
        if (solverSolutionIndex != null) {
            for (int i = 0; i < solverSolutionIndex.size(); i++) {
                if (solverSolutionIndex.get(i) == index) {
                    solverSolutionIndex.delete(i);
                    break;
                }
            }
            final int[] array = solutionLiterals.toArray();
            for (int i = 0, length = solutionLiterals.size(); i < length; i++) {
                final int k = Math.abs(array[i]) - 1;
                if (solverSolution[k] == -integers[k]) {
                    return;
                }
            }
            solverSolutionIndex.push(index);
        }
    }

    public VecInt getSolverSolutionIndex() {
        return solverSolutionIndex;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(integers);
    }

    @Override
    public TWiseConfiguration clone() {
        return new TWiseConfiguration(this);
    }
}
