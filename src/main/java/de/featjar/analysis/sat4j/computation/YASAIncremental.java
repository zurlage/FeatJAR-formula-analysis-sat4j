/*
 * Copyright (C) 2025 FeatJAR-Development-Team
 *
 * This file is part of FeatJAR-FeatJAR-formula-analysis-sat4j.
 *
 * FeatJAR-formula-analysis-sat4j is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3.0 of the License,
 * or (at your option) any later version.
 *
 * FeatJAR-formula-analysis-sat4j is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FeatJAR-formula-analysis-sat4j. If not, see <https://www.gnu.org/licenses/>.
 *
 * See <https://github.com/FeatureIDE/FeatJAR-formula-analysis-sat4j> for further information.
 */
package de.featjar.analysis.sat4j.computation;

import de.featjar.analysis.RuntimeContradictionException;
import de.featjar.analysis.RuntimeTimeoutException;
import de.featjar.analysis.sat4j.solver.ISelectionStrategy;
import de.featjar.analysis.sat4j.solver.ModalImplicationGraph;
import de.featjar.analysis.sat4j.solver.ModalImplicationGraph.Visitor;
import de.featjar.analysis.sat4j.solver.SAT4JSolutionSolver;
import de.featjar.analysis.sat4j.solver.SAT4JSolver;
import de.featjar.base.computation.Computations;
import de.featjar.base.computation.Dependency;
import de.featjar.base.computation.IComputation;
import de.featjar.base.computation.Progress;
import de.featjar.base.data.BinomialCalculator;
import de.featjar.base.data.ExpandableIntegerList;
import de.featjar.base.data.LexicographicIterator;
import de.featjar.base.data.Result;
import de.featjar.formula.VariableMap;
import de.featjar.formula.assignment.ABooleanAssignmentList;
import de.featjar.formula.assignment.BooleanAssignment;
import de.featjar.formula.assignment.BooleanAssignmentList;
import de.featjar.formula.assignment.BooleanClause;
import de.featjar.formula.assignment.BooleanClauseList;
import de.featjar.formula.assignment.BooleanSolution;
import de.featjar.formula.assignment.BooleanSolutionList;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * YASA sampling algorithm. Generates configurations for a given propositional
 * formula such that t-wise feature coverage is achieved.
 *
 * @author Sebastian Krieter
 */
public class YASAIncremental extends ASAT4JAnalysis<BooleanSolutionList> {

    public static final Dependency<BooleanAssignment> LITERALS = Dependency.newDependency(BooleanAssignment.class);
    public static final Dependency<Integer> T = Dependency.newDependency(Integer.class);
    public static final Dependency<Integer> CONFIGURATION_LIMIT = Dependency.newDependency(Integer.class);
    public static final Dependency<Integer> ITERATIONS = Dependency.newDependency(Integer.class);
    public static final Dependency<Integer> INTERNAL_SOLUTION_LIMIT = Dependency.newDependency(Integer.class);

    public static final Dependency<ModalImplicationGraph> MIG = Dependency.newDependency(ModalImplicationGraph.class);

    @SuppressWarnings("rawtypes")
    public static final Dependency<ABooleanAssignmentList> INITIAL_SAMPLE =
            Dependency.newDependency(ABooleanAssignmentList.class);

    public static final Dependency<Boolean> ALLOW_CHANGE_TO_INITIAL_SAMPLE = Dependency.newDependency(Boolean.class);
    public static final Dependency<Boolean> INITIAL_SAMPLE_COUNTS_TOWARDS_CONFIGURATION_LIMIT =
            Dependency.newDependency(Boolean.class);
    public static final Dependency<Boolean> REDUCE_FINAL_SAMPLE = Dependency.newDependency(Boolean.class);

    public YASAIncremental(IComputation<BooleanClauseList> booleanClauseList) {
        super(
                booleanClauseList,
                Computations.of(new BooleanAssignment()),
                Computations.of(2),
                Computations.of(Integer.MAX_VALUE),
                Computations.of(2),
                Computations.of(100_000),
                new MIGBuilder(booleanClauseList),
                Computations.of(new BooleanAssignmentList(null)),
                Computations.of(Boolean.TRUE),
                Computations.of(Boolean.TRUE),
                Computations.of(Boolean.TRUE));
    }

    protected YASAIncremental(YASAIncremental other) {
        super(other);
    }

    /**
     * Converts a set of single literals into a grouped expression list.
     *
     * @param literalSet the literal set
     * @return a grouped expression list (can be used as an input for the
     *         configuration generator).
     */
    public static List<List<BooleanClause>> convertLiterals(BooleanAssignment literalSet) {
        final List<List<BooleanClause>> arrayList = new ArrayList<>(literalSet.size());
        for (final Integer literal : literalSet.get()) {
            final List<BooleanClause> clauseList = new ArrayList<>(1);
            clauseList.add(new BooleanClause(literal));
            arrayList.add(clauseList);
        }
        return arrayList;
    }

    private class PartialConfiguration extends BooleanSolution {
        private static final long serialVersionUID = 1464084516529934929L;

        private final int id;
        private final boolean allowChange;

        private Visitor visitor;
        private ArrayList<BooleanSolution> solverSolutions;

        public PartialConfiguration(PartialConfiguration config) {
            super(config);
            id = config.id;
            allowChange = config.allowChange;
            visitor = config.visitor.getVisitorProvider().new Visitor(config.visitor, elements);
            solverSolutions = config.solverSolutions != null ? new ArrayList<>(config.solverSolutions) : null;
        }

        public PartialConfiguration(int id, boolean allowChange, ModalImplicationGraph mig, int... newliterals) {
            super(new int[n], false);
            this.id = id;
            this.allowChange = allowChange;
            visitor = mig.getVisitor(this.elements);
            solverSolutions = new ArrayList<>();
            visitor.propagate(newliterals);
        }

        public void initSolutionList() {
            solutionLoop:
            for (BooleanSolution solution : randomSample) {
                final int[] solverSolutionLiterals = solution.get();
                for (int j = 0; j < visitor.getAddedLiteralCount(); j++) {
                    final int l = visitor.getAddedLiterals()[j];
                    if (solverSolutionLiterals[Math.abs(l) - 1] != l) {
                        continue solutionLoop;
                    }
                }
                solverSolutions.add(solution);
            }
        }

        public void updateSolutionList(int lastIndex) {
            if (!isComplete()) {
                for (int i = lastIndex; i < visitor.getAddedLiteralCount(); i++) {
                    final int newLiteral = visitor.getAddedLiterals()[i];
                    final int k = Math.abs(newLiteral) - 1;
                    for (int j = solverSolutions.size() - 1; j >= 0; j--) {
                        final int[] solverSolutionLiterals =
                                solverSolutions.get(j).get();
                        if (solverSolutionLiterals[k] != newLiteral) {
                            final int last = solverSolutions.size() - 1;
                            Collections.swap(solverSolutions, j, last);
                            solverSolutions.remove(last);
                        }
                    }
                }
            }
        }

        public int setLiteral(int... literals) {
            final int oldModelCount = visitor.getAddedLiteralCount();
            visitor.propagate(literals);
            return oldModelCount;
        }

        public void clear() {
            solverSolutions = null;
        }

        public boolean isComplete() {
            return visitor.getAddedLiteralCount() == numberOfVariableLiterals;
        }

        public int countLiterals() {
            return visitor.getAddedLiteralCount();
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return super.equals(obj);
        }
    }

    private int n, tmax, t, maxSampleSize, iterations, numberOfVariableLiterals, internalConfigurationLimit;
    private boolean allowChangeToInitialSample, initialSampleCountsTowardsConfigurationLimit, reduceFinalSample;

    private SAT4JSolutionSolver solver;
    private VariableMap variableMap;
    private ModalImplicationGraph mig;
    private Random random;
    private List<List<BooleanClause>> presenceConditions;

    private ABooleanAssignmentList<?> initialSample;
    private ArrayDeque<BooleanSolution> randomSample;
    private List<PartialConfiguration> bestSample;
    private List<PartialConfiguration> currentSample;

    private ArrayList<PartialConfiguration> candidateConfiguration;
    private ArrayList<ExpandableIntegerList> currentSampleIndices;
    private ExpandableIntegerList[] selectedSampleIndices;
    private BitSet[] bestSampleIndices;
    private PartialConfiguration newConfiguration;
    private int curSolutionId;
    private boolean overLimit;

    @Override
    public Result<BooleanSolutionList> compute(List<Object> dependencyList, Progress progress) {
        tmax = T.get(dependencyList);
        if (tmax < 1) {
            throw new IllegalArgumentException("Value for t must be grater than 0. Value was " + tmax);
        }

        iterations = ITERATIONS.get(dependencyList);
        if (iterations == 0) {
            throw new IllegalArgumentException("Iterations must not equal 0.");
        }
        if (iterations < 0) {
            iterations = Integer.MAX_VALUE;
        }

        internalConfigurationLimit = INTERNAL_SOLUTION_LIMIT.get(dependencyList);
        if (internalConfigurationLimit < 0) {
            throw new IllegalArgumentException(
                    "Internal solution limit must be greater than 0. Value was " + internalConfigurationLimit);
        }

        maxSampleSize = CONFIGURATION_LIMIT.get(dependencyList);
        if (maxSampleSize < 0) {
            throw new IllegalArgumentException(
                    "Configuration limit must be greater than 0. Value was " + maxSampleSize);
        }

        initialSample = INITIAL_SAMPLE.get(dependencyList);

        random = new Random(RANDOM_SEED.get(dependencyList));

        allowChangeToInitialSample = ALLOW_CHANGE_TO_INITIAL_SAMPLE.get(dependencyList);
        initialSampleCountsTowardsConfigurationLimit =
                INITIAL_SAMPLE_COUNTS_TOWARDS_CONFIGURATION_LIMIT.get(dependencyList);
        reduceFinalSample = REDUCE_FINAL_SAMPLE.get(dependencyList);

        randomSample = new ArrayDeque<>(internalConfigurationLimit);
        variableMap = BOOLEAN_CLAUSE_LIST.get(dependencyList).getVariableMap();
        solver = initializeSolver(dependencyList);
        mig = MIG.get(dependencyList);
        n = mig.size();

        if (initialSampleCountsTowardsConfigurationLimit) {
            maxSampleSize = Math.max(maxSampleSize, maxSampleSize + initialSample.size());
        }

        BooleanAssignment variables = LITERALS.get(dependencyList);
        BooleanAssignment core = new BooleanAssignment(mig.getCore());

        solver.setSelectionStrategy(ISelectionStrategy.random(random));

        List<List<BooleanClause>> nodes;
        if (variables.isEmpty()) {
            nodes = convertLiterals(new BooleanAssignment(
                    IntStream.range(-n, n + 1).filter(i -> i != 0).toArray()));
        } else {
            nodes = convertLiterals(variables);
        }
        numberOfVariableLiterals = nodes.size() - core.countNegatives() - core.countPositives();
        tmax = Math.min(tmax, Math.max(numberOfVariableLiterals, 1));

        presenceConditions = new ArrayList<>();
        expressionLoop:
        for (final List<BooleanClause> clauses : nodes) {
            final List<BooleanClause> newClauses = new ArrayList<>(clauses.size());
            for (final BooleanClause clause : clauses) {
                // If clause can be satisfied
                if (!core.containsAnyNegated(clause)) {
                    // If clause is already satisfied
                    if (core.containsAll(clause)) {
                        continue expressionLoop;
                    } else {
                        newClauses.add(new BooleanClause(clause));
                    }
                }
            }
            if (!newClauses.isEmpty()) {
                Collections.sort(newClauses, (o1, o2) -> o1.size() - o2.size());
                presenceConditions.add(newClauses);
            }
        }

        int totalSteps = 0;
        for (int j = 1; j < tmax; j++) {
            totalSteps += (int) (BinomialCalculator.computeBinomial(presenceConditions.size(), j));
        }
        totalSteps += iterations * (int) (BinomialCalculator.computeBinomial(presenceConditions.size(), tmax));
        progress.setTotalSteps(totalSteps);

        buildCombinations(progress);

        if (!overLimit && iterations > 1) {
            rebuildCombinations(progress);
        }

        return finalizeResult();
    }

    @Override
    protected SAT4JSolver newSolver(BooleanClauseList clauseList) {
        return new SAT4JSolutionSolver(clauseList);
    }

    @Override
    public Result<BooleanSolutionList> getIntermediateResult() {
        return finalizeResult();
    }

    private Result<BooleanSolutionList> finalizeResult() {
        if (bestSample != null) {
            BooleanSolutionList result = new BooleanSolutionList(variableMap, bestSample.size());
            for (int j = bestSample.size() - 1; j >= 0; j--) {
                result.add(autoComplete(bestSample.get(j)));
            }
            if (reduceFinalSample) {
                bestSample = reduce(bestSample);
            }
            return Result.of(result);
        } else {
            return Result.empty();
        }
    }

    private void buildCombinations(Progress monitor) {
        initSample();
        final int[] literals = initliterals(false);

        for (int ti = 1; ti <= tmax; ti++) {
            checkCancel();
            t = ti;
            selectedSampleIndices = new ExpandableIntegerList[t];
            initRun();
            LexicographicIterator.stream(t, presenceConditions.size()).forEach(combo -> {
                checkCancel();
                monitor.incrementCurrentStep();
                int[] combinationLiterals = combo.getSelection(literals);

                if (isCovered(combinationLiterals, currentSampleIndices)) {
                    return;
                }
                if (isCombinationInvalidMIG(combinationLiterals)) {
                    return;
                }

                try {
                    if (isCombinationValidSample(combinationLiterals)) {
                        if (tryCover(combinationLiterals)) {
                            return;
                        }
                    } else {
                        if (isCombinationInvalidSAT(combinationLiterals)) {
                            return;
                        }
                    }

                    if (tryCoverWithSat(combinationLiterals)) {
                        return;
                    }
                    newConfiguration(combinationLiterals);
                } finally {
                    candidateConfiguration.clear();
                    newConfiguration = null;
                }
            });
        }
        setBestSolutionList();
    }

    private void rebuildCombinations(Progress monitor) {
        if (iterations > 1) {
            int solutionCount = bestSample.size();
            bestSampleIndices = new BitSet[2 * n + 1];
            for (int j = 1; j <= n; j++) {
                BitSet negIndices = new BitSet(solutionCount);
                BitSet posIndices = new BitSet(solutionCount);
                for (int i = 0; i < solutionCount; i++) {
                    BooleanSolution config = bestSample.get(i);
                    int l = config.get(j - 1);
                    if (l != 0) {
                        if (l < 0) {
                            negIndices.set(i);
                        } else {
                            posIndices.set(i);
                        }
                    }
                }
                bestSampleIndices[n - j] = negIndices;
                bestSampleIndices[j + n] = posIndices;
            }
        }

        for (int j = 1; j < iterations; j++) {
            checkCancel();
            final int[] literals = initliterals(true);
            initSample();
            initRun();
            LexicographicIterator.stream(tmax, presenceConditions.size()).forEach(combo -> {
                int[] combinationLiterals = combo.getSelection(literals);

                if (isCovered(combinationLiterals, currentSampleIndices)) {
                    return;
                }
                if (!isCovered(combinationLiterals, bestSampleIndices)) {
                    return;
                }
                try {
                    if (tryCoverWithoutMIG(combinationLiterals)) {
                        return;
                    }
                    if (tryCoverWithSat(combinationLiterals)) {
                        return;
                    }
                    newConfiguration(combinationLiterals);
                } finally {
                    candidateConfiguration.clear();
                    newConfiguration = null;
                }
            });
            setBestSolutionList();
        }
    }

    private void setBestSolutionList() {
        if (bestSample == null || bestSample.size() > currentSample.size()) {
            bestSample = currentSample;
        }
    }

    private void initSample() {
        curSolutionId = 0;
        overLimit = false;
        currentSample = new ArrayList<>();
        final int indexSize = 2 * n;
        currentSampleIndices = new ArrayList<>(indexSize);
        for (int i = 0; i < indexSize; i++) {
            currentSampleIndices.add(new ExpandableIntegerList());
        }
        for (BooleanAssignment config : initialSample) {
            if (currentSample.size() < maxSampleSize) {
                PartialConfiguration initialConfiguration =
                        new PartialConfiguration(curSolutionId++, allowChangeToInitialSample, mig, config.get());
                if (allowChangeToInitialSample) {
                    initialConfiguration.initSolutionList();
                }
                if (initialConfiguration.isComplete()) {
                    initialConfiguration.clear();
                }
                currentSample.add(initialConfiguration);
                for (int i = 0; i < initialConfiguration.visitor.getAddedLiteralCount(); i++) {
                    ExpandableIntegerList indexList = currentSampleIndices.get(ModalImplicationGraph.getVertexIndex(
                            initialConfiguration.visitor.getAddedLiterals()[i]));
                    indexList.add(initialConfiguration.id);
                }
            } else {
                overLimit = true;
            }
        }
    }

    private void initRun() {
        newConfiguration = null;
        candidateConfiguration = new ArrayList<>();
        Collections.sort(currentSample, (a, b) -> b.countLiterals() - a.countLiterals());
    }

    private int[] initliterals(boolean shuffle) {
        if (shuffle) {
            final Map<Integer, List<List<BooleanClause>>> groupedPCs =
                    presenceConditions.stream().collect(Collectors.groupingBy(List::size));
            for (final List<List<BooleanClause>> pcList : groupedPCs.values()) {
                Collections.shuffle(pcList, random);
            }
            final List<Map.Entry<Integer, List<List<BooleanClause>>>> shuffledPCs =
                    new ArrayList<>(groupedPCs.entrySet());
            Collections.sort(shuffledPCs, (a, b) -> a.getKey() - b.getKey());
            presenceConditions.clear();
            for (final Map.Entry<Integer, List<List<BooleanClause>>> entry : shuffledPCs) {
                presenceConditions.addAll(entry.getValue());
            }
        }
        final int[] literals = new int[presenceConditions.size()];
        for (int i1 = 0; i1 < literals.length; i1++) {
            literals[i1] = presenceConditions.get(i1).get(0).get()[0];
        }
        return literals;
    }

    private boolean isCovered(int[] literals, ArrayList<ExpandableIntegerList> indexedSolutions) {
        if (t < 2) {
            return !indexedSolutions
                    .get(ModalImplicationGraph.getVertexIndex(literals[0]))
                    .isEmpty();
        }
        for (int i = 0; i < t; i++) {
            final ExpandableIntegerList indexedSolution =
                    indexedSolutions.get(ModalImplicationGraph.getVertexIndex(literals[i]));
            if (indexedSolution.size() == 0) {
                return false;
            }
            selectedSampleIndices[i] = indexedSolution;
        }
        Arrays.sort(selectedSampleIndices, (a, b) -> a.size() - b.size());
        final int[] ix = new int[t - 1];

        final ExpandableIntegerList i0 = selectedSampleIndices[0];
        final int[] ia0 = i0.toArray();
        loop:
        for (int i = 0; i < i0.size(); i++) {
            int id0 = ia0[i];
            for (int j = 1; j < t; j++) {
                final ExpandableIntegerList i1 = selectedSampleIndices[j];
                int binarySearch = Arrays.binarySearch(i1.toArray(), ix[j - 1], i1.size(), id0);
                if (binarySearch < 0) {
                    ix[j - 1] = -binarySearch - 1;
                    continue loop;
                } else {
                    ix[j - 1] = binarySearch;
                }
            }
            return true;
        }
        return false;
    }

    private BitSet combinedIndex(final int size, int[] literals, BitSet[] bitSets) {
        BitSet first = bitSets[literals[0] + size];
        BitSet bitSet = new BitSet(first.size());
        bitSet.xor(first);
        for (int k = 1; k < literals.length; k++) {
            bitSet.and(bitSets[literals[k] + size]);
        }
        return bitSet;
    }

    private boolean isCovered(int[] literals, BitSet[] indexedSolutions) {
        if (t == 1) {
            return !indexedSolutions[literals[0] + n].isEmpty();
        }

        return !combinedIndex(n, literals, indexedSolutions).isEmpty();
    }

    private void select(PartialConfiguration solution, int[] literals) {
        final int lastIndex = solution.setLiteral(literals);
        for (int i = lastIndex; i < solution.visitor.getAddedLiteralCount(); i++) {
            ExpandableIntegerList indexList = currentSampleIndices.get(
                    ModalImplicationGraph.getVertexIndex(solution.visitor.getAddedLiterals()[i]));
            final int idIndex = Arrays.binarySearch(indexList.toArray(), 0, indexList.size(), solution.id);
            if (idIndex < 0) {
                indexList.add(solution.id, -(idIndex + 1));
            }
        }
        solution.updateSolutionList(lastIndex);
    }

    private boolean tryCover(int[] literals) {
        return newConfiguration == null ? tryCoverWithoutMIG(literals) : tryCoverWithMIG(literals);
    }

    private boolean tryCoverWithoutMIG(int[] literals) {
        configLoop:
        for (final PartialConfiguration configuration : currentSample) {
            if (configuration.allowChange && !configuration.isComplete()) {
                final int[] literals2 = configuration.get();
                for (int i = 0; i < literals.length; i++) {
                    final int l = literals[i];
                    if (literals2[Math.abs(l) - 1] == -l) {
                        continue configLoop;
                    }
                }
                if (isSelectionPossibleSol(configuration, literals)) {
                    select(configuration, literals);
                    change(configuration);
                    return true;
                }
                candidateConfiguration.add(configuration);
            }
        }
        return false;
    }

    private boolean tryCoverWithMIG(int[] literals) {
        configLoop:
        for (final PartialConfiguration configuration : currentSample) {
            if (configuration.allowChange && !configuration.isComplete()) {
                final int[] literals2 = configuration.get();
                for (int i = 0; i < newConfiguration.visitor.getAddedLiteralCount(); i++) {
                    final int l = newConfiguration.visitor.getAddedLiterals()[i];
                    if (literals2[Math.abs(l) - 1] == -l) {
                        continue configLoop;
                    }
                }
                if (isSelectionPossibleSol(configuration, literals)) {
                    select(configuration, literals);
                    change(configuration);
                    return true;
                }
                candidateConfiguration.add(configuration);
            }
        }
        return false;
    }

    private void addToCandidateList(int[] literals) {
        if (newConfiguration != null) {
            configLoop:
            for (final PartialConfiguration configuration : currentSample) {
                if (configuration.allowChange && !configuration.isComplete()) {
                    final int[] literals2 = configuration.get();
                    for (int i = 0; i < newConfiguration.visitor.getAddedLiteralCount(); i++) {
                        final int l = newConfiguration.visitor.getAddedLiterals()[i];
                        if (literals2[Math.abs(l) - 1] == -l) {
                            continue configLoop;
                        }
                    }
                    candidateConfiguration.add(configuration);
                }
            }
        } else {
            configLoop:
            for (final PartialConfiguration configuration : currentSample) {
                if (configuration.allowChange && !configuration.isComplete()) {
                    final int[] literals2 = configuration.get();
                    for (int i = 0; i < literals.length; i++) {
                        final int l = literals[i];
                        if (literals2[Math.abs(l) - 1] == -l) {
                            continue configLoop;
                        }
                    }
                    candidateConfiguration.add(configuration);
                }
            }
        }
    }

    private void change(final PartialConfiguration configuration) {
        if (configuration.isComplete()) {
            configuration.clear();
        }
        Collections.sort(currentSample, (a, b) -> b.countLiterals() - a.countLiterals());
    }

    private boolean isCombinationInvalidMIG(int[] literals) {
        try {
            newConfiguration = new PartialConfiguration(curSolutionId++, true, mig, literals);
        } catch (RuntimeContradictionException e) {
            return true;
        }
        return false;
    }

    private boolean isCombinationValidSample(int[] literals) {
        for (final BooleanSolution s : randomSample) {
            if (!s.containsAnyNegated(literals)) {
                return true;
            }
        }
        return false;
    }

    private boolean isCombinationInvalidSAT(int[] literals) {
        final int orgAssignmentLength = solver.getAssignment().size();
        try {
            if (newConfiguration != null) {
                for (int i = 0; i < newConfiguration.visitor.getAddedLiteralCount(); i++) {
                    solver.getAssignment().add(newConfiguration.visitor.getAddedLiterals()[i]);
                }
            } else {
                for (int i = 0; i < literals.length; i++) {
                    solver.getAssignment().add(literals[i]);
                }
            }
            Result<Boolean> hasSolution = solver.hasSolution();
            if (hasSolution.isPresent()) {
                if (hasSolution.get()) {
                    BooleanSolution e = addSolverSolution();

                    addToCandidateList(literals);
                    PartialConfiguration compatibleConfiguration = null;
                    for (PartialConfiguration c : candidateConfiguration) {
                        if (!c.containsAnyNegated(e)) {
                            if (compatibleConfiguration == null) {
                                compatibleConfiguration = c;
                            } else {
                                c.solverSolutions.add(e);
                            }
                        }
                    }
                    if (compatibleConfiguration != null) {
                        select(compatibleConfiguration, literals);
                        compatibleConfiguration.solverSolutions.add(e);
                        change(compatibleConfiguration);
                        return true;
                    }
                    return false;
                } else {
                    return true;
                }
            } else {
                return true;
            }
        } finally {
            solver.getAssignment().clear(orgAssignmentLength);
        }
    }

    private boolean tryCoverWithSat(int[] literals) {
        for (PartialConfiguration configuration : candidateConfiguration) {
            if (trySelectSat(configuration, literals)) {
                change(configuration);
                return true;
            }
        }
        return false;
    }

    private void newConfiguration(int[] literals) {
        if (currentSample.size() < maxSampleSize) {
            if (newConfiguration == null) {
                newConfiguration = new PartialConfiguration(curSolutionId++, true, mig, literals);
            }
            newConfiguration.initSolutionList();
            currentSample.add(newConfiguration);
            change(newConfiguration);
            for (int i = 0; i < newConfiguration.visitor.getAddedLiteralCount(); i++) {
                ExpandableIntegerList indexList = currentSampleIndices.get(ModalImplicationGraph.getVertexIndex(
                        newConfiguration.visitor.getAddedLiterals()[i]));
                indexList.add(newConfiguration.id);
            }
        } else {
            overLimit = true;
        }
    }

    private BooleanSolution autoComplete(PartialConfiguration configuration) {
        if (configuration.allowChange && !configuration.isComplete()) {
            if (configuration.solverSolutions != null && configuration.solverSolutions.size() > 0) {
                final int[] configuration2 =
                        configuration.solverSolutions.get(0).get();
                System.arraycopy(configuration2, 0, configuration.get(), 0, configuration.size());
                configuration.clear();
            } else {
                final int orgAssignmentSize = setUpSolver(configuration);
                try {
                    Result<Boolean> hasSolution = solver.hasSolution();
                    if (hasSolution.isPresent()) {
                        if (hasSolution.get()) {
                            final int[] internalSolution = solver.getInternalSolution();
                            System.arraycopy(internalSolution, 0, configuration.get(), 0, configuration.size());
                            configuration.clear();
                        } else {
                            throw new RuntimeContradictionException();
                        }
                    } else {
                        throw new RuntimeTimeoutException();
                    }
                } finally {
                    solver.getAssignment().clear(orgAssignmentSize);
                }
            }
        }
        return new BooleanSolution(configuration.get(), false);
    }

    private boolean isSelectionPossibleSol(PartialConfiguration configuration, int[] literals) {
        for (BooleanSolution configuration2 : configuration.solverSolutions) {
            if (!configuration2.containsAnyNegated(literals)) {
                return true;
            }
        }
        return false;
    }

    private boolean trySelectSat(PartialConfiguration configuration, final int[] literals) {
        final int oldModelCount = configuration.visitor.getAddedLiteralCount();
        try {
            configuration.visitor.propagate(literals);
        } catch (RuntimeException e) {
            configuration.visitor.reset(oldModelCount);
            return false;
        }

        final int orgAssignmentSize = setUpSolver(configuration);
        try {
            if (newConfiguration != null) {
                for (int i = 0; i < newConfiguration.visitor.getAddedLiteralCount(); i++) {
                    int l = newConfiguration.visitor.getAddedLiterals()[i];
                    if (configuration.get()[Math.abs(l) - 1] == 0) {
                        solver.getAssignment().add(l);
                    }
                }
            } else {
                for (int i = 0; i < literals.length; i++) {
                    int l = literals[i];
                    if (configuration.get()[Math.abs(l) - 1] == 0) {
                        solver.getAssignment().add(l);
                    }
                }
            }
            Result<Boolean> hasSolution = solver.hasSolution();
            if (hasSolution.isPresent()) {
                if (hasSolution.get()) {
                    final BooleanSolution e = addSolverSolution();
                    for (int i = oldModelCount; i < configuration.visitor.getAddedLiteralCount(); i++) {
                        ExpandableIntegerList indexList = currentSampleIndices.get(ModalImplicationGraph.getVertexIndex(
                                configuration.visitor.getAddedLiterals()[i]));
                        final int idIndex =
                                Arrays.binarySearch(indexList.toArray(), 0, indexList.size(), configuration.id);
                        if (idIndex < 0) {
                            indexList.add(configuration.id, -(idIndex + 1));
                        }
                    }
                    configuration.updateSolutionList(oldModelCount);
                    configuration.solverSolutions.add(e);
                    return true;
                } else {
                    configuration.visitor.reset(oldModelCount);
                }
            } else {
                configuration.visitor.reset(oldModelCount);
            }
        } finally {
            solver.getAssignment().clear(orgAssignmentSize);
        }
        return false;
    }

    private BooleanSolution addSolverSolution() {
        if (randomSample.size() == internalConfigurationLimit) {
            randomSample.removeFirst();
        }
        final int[] solution = solver.getInternalSolution();
        final BooleanSolution e = new BooleanSolution(Arrays.copyOf(solution, solution.length), false);
        randomSample.add(e);
        solver.shuffleOrder(random);
        return e;
    }

    private int setUpSolver(PartialConfiguration configuration) {
        final int orgAssignmentSize = solver.getAssignment().size();
        for (int i = 0; i < configuration.visitor.getAddedLiteralCount(); i++) {
            solver.getAssignment().add(configuration.visitor.getAddedLiterals()[i]);
        }
        return orgAssignmentSize;
    }

    private List<PartialConfiguration> reduce(List<PartialConfiguration> solutionList) {
        if (solutionList.isEmpty()) {
            return solutionList;
        }
        final int n = solutionList.get(0).size();
        int t2 = (n < t) ? n : t;
        int nonUniqueIndex = solutionList.size();

        for (int i = 0; i < nonUniqueIndex; i++) {
            BooleanSolution config = solutionList.get(i);
            int finalNonUniqueIndex = nonUniqueIndex;

            boolean hasUnique = LexicographicIterator.parallelStream(t2, n).anyMatch(combo -> {
                int[] literals = combo.getSelection(config.get());
                for (int j = 0; j < finalNonUniqueIndex; j++) {
                    PartialConfiguration config2 = solutionList.get(j);
                    if (config != config2 && config2.containsAll(literals)) {
                        return false;
                    }
                }
                return true;
            });
            if (!hasUnique) {
                Collections.swap(solutionList, i--, --nonUniqueIndex);
            }
        }
        return solutionList.subList(0, nonUniqueIndex);
    }
}
