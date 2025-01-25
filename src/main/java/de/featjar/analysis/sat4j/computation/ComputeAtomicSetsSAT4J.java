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

import de.featjar.analysis.RuntimeTimeoutException;
import de.featjar.analysis.sat4j.solver.ISelectionStrategy;
import de.featjar.analysis.sat4j.solver.SAT4JSolutionSolver;
import de.featjar.analysis.sat4j.twise.SampleBitIndex;
import de.featjar.base.computation.Computations;
import de.featjar.base.computation.Dependency;
import de.featjar.base.computation.IComputation;
import de.featjar.base.computation.Progress;
import de.featjar.base.data.ExpandableIntegerList;
import de.featjar.base.data.Result;
import de.featjar.formula.VariableMap;
import de.featjar.formula.assignment.BooleanAssignment;
import de.featjar.formula.assignment.BooleanAssignmentList;
import de.featjar.formula.assignment.BooleanSolution;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
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

    private List<BitSet> solutions;
    private SampleBitIndex solutionIndex;
    private int variableCount;

    private Random random;

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
        random = new Random(RANDOM_SEED.get(dependencyList));
        VariableMap variableMap = BOOLEAN_CLAUSE_LIST.get(dependencyList).getVariableMap();

        boolean omitCore = OMIT_CORE.get(dependencyList);
        boolean omitSingles = OMIT_SINGLE_SETS.get(dependencyList);

        final BooleanAssignmentList atomicSets = new BooleanAssignmentList(variableMap);
        variableCount = variableMap.getVariableCount();
        solutions = new ArrayList<>();
        solutionIndex = new SampleBitIndex(variableCount);

        BooleanAssignment variables = VARIABLES_OF_INTEREST.get(dependencyList);
        final boolean[] decided = new boolean[variableCount];
        if (!variables.isEmpty()) {
            BooleanAssignment ignoredVariables = variableMap.getVariables().removeAll(variables);
            for (int var : ignoredVariables.get()) {
                decided[var - 1] = true;
            }
            progress.setTotalSteps(2 * variables.size() + 2);
        } else {
            progress.setTotalSteps(2 * variableCount + 2);
        }
        checkCancel();

        solver.setSelectionStrategy(ISelectionStrategy.positive());
        Result<BooleanSolution> findSolution = solver.findSolution();
        if (findSolution.isEmpty()) {
            return findSolution.merge(Result.empty());
        }

        final int[] firstSolution = findSolution.get().get();
        addSolution(firstSolution);
        solver.setSelectionStrategy(ISelectionStrategy.inverse(firstSolution));
        progress.incrementCurrentStep();
        checkCancel();

        final int[] secondSolution = solver.findSolution().get().get();
        addSolution(secondSolution);
        final int[] undecided = Arrays.copyOf(firstSolution, firstSolution.length);
        BooleanSolution.removeConflictsInplace(undecided, secondSolution);
        progress.incrementCurrentStep();
        checkCancel();

        solver.setSelectionStrategy(ISelectionStrategy.random(random));

        ExpandableIntegerList core = new ExpandableIntegerList();
        for (int i = 0; i < variableCount; i++) {
            progress.incrementCurrentStep();
            checkCancel();
            if (!decided[i]) {
                int potentialCoreLiteral = undecided[i];
                if (potentialCoreLiteral != 0) {
                    solver.getAssignment().add(-potentialCoreLiteral);
                    Result<Boolean> hasSolution = solver.hasSolution();
                    if (hasSolution.isEmpty()) {
                        solver.getAssignment().remove();
                        throw new RuntimeTimeoutException();
                    } else if (hasSolution.valueEquals(Boolean.FALSE)) {
                        decided[i] = true;
                        core.add(potentialCoreLiteral);
                        solver.getAssignment().replaceLast(potentialCoreLiteral);
                    } else if (hasSolution.valueEquals(Boolean.TRUE)) {
                        solver.getAssignment().remove();
                        int[] internalSolution = solver.getInternalSolution();
                        addSolution(internalSolution);
                        BooleanSolution.removeConflictsInplace(undecided, internalSolution);
                        solver.shuffleOrder(random);
                    }
                }
            }
        }
        if (!omitCore) {
            atomicSets.add(new BooleanAssignment(core.toArray()));
        }

        for (int vi = 0; vi < variableCount; vi++) {
            progress.incrementCurrentStep();
            checkCancel();
            if (!decided[vi]) {
                decided[vi] = true;
                int v = vi + 1;

                ExpandableIntegerList atomicSet = new ExpandableIntegerList();
                atomicSet.add(v);

                BitSet commonPositiveLiterals = new BitSet(variableCount);
                BitSet commonNegativeLiterals = new BitSet(variableCount);
                commonPositiveLiterals.flip(0, variableCount);
                commonNegativeLiterals.flip(0, variableCount);
                findCommenLiterals(v, commonPositiveLiterals, commonNegativeLiterals);
                findCommenLiterals(-v, commonNegativeLiterals, commonPositiveLiterals);

                int ui = commonPositiveLiterals.nextSetBit(vi + 1);
                while (ui >= 0) {
                    if (!decided[ui]) {
                        int u = ui + 1;
                        if (unsat(solver, -v, u) && unsat(solver, v, -u)) {
                            atomicSet.add(u);
                            decided[ui] = true;
                        }
                    }
                    ui = commonPositiveLiterals.nextSetBit(ui + 1);
                }

                ui = commonNegativeLiterals.nextSetBit(vi + 1);
                while (ui >= 0) {
                    if (!decided[ui]) {
                        int u = -(ui + 1);
                        if (unsat(solver, -v, u) && unsat(solver, v, -u)) {
                            atomicSet.add(u);
                            decided[ui] = true;
                        }
                    }
                    ui = commonNegativeLiterals.nextSetBit(ui + 1);
                }
                if (!omitSingles || atomicSet.size() > 1) {
                    atomicSets.add(new BooleanAssignment(atomicSet.toArray()));
                }
            }
        }

        solutions = null;
        solutionIndex = null;
        random = null;
        return Result.of(atomicSets);
    }

    private boolean unsat(SAT4JSolutionSolver solver, final int v, int u) {
        solver.getAssignment().add(v);
        solver.getAssignment().add(u);
        try {
            Result<Boolean> hasSolution = solver.hasSolution();
            if (hasSolution.isEmpty()) {
                return false;
            } else if (hasSolution.valueEquals(Boolean.TRUE)) {
                int[] internalSolution = solver.getInternalSolution();
                addSolution(internalSolution);
                solver.shuffleOrder(random);
                return false;
            }
            return true;
        } finally {
            solver.getAssignment().remove();
            solver.getAssignment().remove();
        }
    }

    private void findCommenLiterals(final int v, BitSet commonPositiveLiterals, BitSet commonNegativeLiterals) {
        BitSet positiveBitSet = solutionIndex.getBitSet(v);
        int nextSetBit = positiveBitSet.nextSetBit(0);
        while (nextSetBit >= 0) {
            BitSet bitSet = solutions.get(nextSetBit);
            commonPositiveLiterals.and(bitSet);
            commonNegativeLiterals.andNot(bitSet);
            nextSetBit = positiveBitSet.nextSetBit(nextSetBit + 1);
        }
    }

    private void addSolution(final int[] firstSolution) {
        solutionIndex.addConfiguration(firstSolution);
        BitSet bitSet = new BitSet(variableCount);
        solutions.add(bitSet);
        for (int i = 0; i < variableCount; i++) {
            bitSet.set(i, firstSolution[i] > 0);
        }
    }
}
