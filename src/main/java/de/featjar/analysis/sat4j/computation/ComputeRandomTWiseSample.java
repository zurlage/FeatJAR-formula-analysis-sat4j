/*
 * Copyright (C) 2024 FeatJAR-Development-Team
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

import de.featjar.analysis.RuntimeContradictionException;
import de.featjar.analysis.RuntimeTimeoutException;
import de.featjar.analysis.sat4j.solver.MIGVisitorByte;
import de.featjar.analysis.sat4j.solver.ModalImplicationGraph;
import de.featjar.analysis.sat4j.twise.SampleBitIndex;
import de.featjar.base.computation.Dependency;
import de.featjar.base.computation.IComputation;
import de.featjar.base.computation.Progress;
import de.featjar.base.data.Result;
import de.featjar.formula.assignment.BooleanAssignment;
import de.featjar.formula.assignment.BooleanAssignmentList;
import de.featjar.formula.assignment.BooleanSolution;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

/**
 * YASA sampling algorithm. Generates configurations for a given propositional
 * formula such that t-wise feature coverage is achieved.
 *
 * @author Sebastian Krieter
 */
public class ComputeRandomTWiseSample extends ATWiseSampleComputation {

    public static final Dependency<ModalImplicationGraph> MIG = Dependency.newDependency(ModalImplicationGraph.class);

    public ComputeRandomTWiseSample(IComputation<BooleanAssignmentList> clauseList) {
        super(clauseList, new MIGBuilder(clauseList));
    }

    private int maxT, variableCount;

    private ICombinationSpecification variables;
    private ModalImplicationGraph mig;

    private SampleBitIndex sampleIndex;

    @Override
    public Result<BooleanAssignmentList> computeSample(List<Object> dependencyList, Progress progress) {
        mig = MIG.get(dependencyList);

        if (variables instanceof NoneCombinationSpecification) {
            variables = new SingleCombinationSpecification(
                    new BooleanAssignment(new BooleanAssignment(IntStream.range(-variableCount, variableCount + 1)
                                    .filter(i -> i != 0)
                                    .toArray())
                            .removeAllVariables(
                                    Arrays.stream(mig.getCore()).map(Math::abs).toArray())),
                    maxT);
        }

        progress.setTotalSteps(variables.getTotalSteps());

        buildCombinations(progress);

        return finalizeResult();
    }

    @Override
    public Result<BooleanAssignmentList> getIntermediateResult() {
        return finalizeResult();
    }

    private Result<BooleanAssignmentList> finalizeResult() {
        BooleanAssignmentList result = new BooleanAssignmentList(variableMap, sampleIndex.size());
        int initialSize = initialSample.size();
        for (int j = 0; j < initialSize; j++) {
            result.add(new BooleanSolution(sampleIndex.getConfiguration(j), false));
        }
        for (int j = sampleIndex.size() - 1; j >= initialSize; j--) {
            result.add(new BooleanSolution(Arrays.stream(sampleIndex.getConfiguration(j))
                    .filter(l -> l != 0)
                    .toArray()));
        }
        return Result.of(result);
    }

    private void buildCombinations(Progress monitor) {
        sampleIndex = new SampleBitIndex(variableCount);
        for (BooleanAssignment config : initialSample) {
            sampleIndex.addConfiguration(config);
        }

        variables.shuffle(random);
        variables.stream().forEach(combinationLiterals -> {
            checkCancel();
            monitor.incrementCurrentStep();

            if (sampleIndex.test(combinationLiterals)) {
                return;
            }
            if (isCombinationInvalidMIG(combinationLiterals)) {
                return;
            }
            newRandomConfiguration(combinationLiterals);
        });
    }

    private boolean isCombinationInvalidMIG(int[] literals) {
        try {
            MIGVisitorByte visitor = new MIGVisitorByte(mig);
            visitor.propagate(literals);
        } catch (RuntimeContradictionException e) {
            return true;
        }
        return false;
    }

    private void newRandomConfiguration(final int[] fixedLiterals) {
        int orgAssignmentSize = solver.getAssignment().size();
        try {
            solver.getAssignment().addAll(fixedLiterals);
            Result<Boolean> hasSolution = solver.hasSolution();
            if (hasSolution.isPresent()) {
                if (hasSolution.get()) {
                    sampleIndex.addConfiguration(solver.getInternalSolution());
                    solver.shuffleOrder(random);
                }
            } else {
                throw new RuntimeTimeoutException();
            }
        } finally {
            solver.getAssignment().clear(orgAssignmentSize);
        }
    }
}
