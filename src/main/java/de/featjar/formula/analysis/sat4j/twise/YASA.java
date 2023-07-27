/*
 * Copyright (C) 2023 Sebastian Krieter
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
package de.featjar.formula.analysis.sat4j.twise;

import de.featjar.base.FeatJAR;
import de.featjar.base.computation.Computations;
import de.featjar.base.computation.Dependency;
import de.featjar.base.computation.DependencyList;
import de.featjar.base.computation.IComputation;
import de.featjar.base.computation.IRandomDependency;
import de.featjar.base.computation.Progress;
import de.featjar.base.data.ExpandableIntegerList;
import de.featjar.base.data.Result;
import de.featjar.formula.analysis.RuntimeContradictionException;
import de.featjar.formula.analysis.RuntimeTimeoutException;
import de.featjar.formula.analysis.bool.BooleanAssignment;
import de.featjar.formula.analysis.bool.BooleanClause;
import de.featjar.formula.analysis.bool.BooleanClauseList;
import de.featjar.formula.analysis.bool.BooleanSolution;
import de.featjar.formula.analysis.bool.BooleanSolutionList;
import de.featjar.formula.analysis.combinations.BinomialCalculator;
import de.featjar.formula.analysis.combinations.LexicographicIterator;
import de.featjar.formula.analysis.mig.solver.MIGBuilder;
import de.featjar.formula.analysis.mig.solver.ModalImplicationGraph;
import de.featjar.formula.analysis.mig.solver.ModalImplicationGraph.Visitor;
import de.featjar.formula.analysis.sat4j.ASAT4JAnalysis;
import de.featjar.formula.analysis.sat4j.solver.ISelectionStrategy;
import de.featjar.formula.analysis.sat4j.solver.SAT4JSolutionSolver;
import de.featjar.formula.analysis.sat4j.solver.SAT4JSolver;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * YASA sampling algorithm. Generates configurations for a given propositional
 * formula such that t-wise feature coverage is achieved.
 *
 * @author Sebastian Krieter
 */
public class YASA extends ASAT4JAnalysis<BooleanSolutionList> implements IRandomDependency {

    public static final Dependency<Integer> T = Dependency.newDependency(Integer.class);
    public static final Dependency<BooleanAssignment> VARIABLES_OF_INTEREST =
            Dependency.newDependency(BooleanAssignment.class);

    public YASA(IComputation<BooleanClauseList> booleanClauseList) {
        super(booleanClauseList, Computations.of(2), Computations.of(new BooleanAssignment()));
    }

    protected YASA(YASA other) {
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

        private final int id;

        private Visitor visitor;

        private ArrayList<BooleanSolution> solverSolutions;

        public PartialConfiguration(PartialConfiguration config) {
            super(config);
            id = config.id;
            visitor = config.visitor.getVisitorProvider().new Visitor(config.visitor, array);
            solverSolutions = config.solverSolutions != null ? new ArrayList<>(config.solverSolutions) : null;
        }

        public PartialConfiguration(int id, ModalImplicationGraph mig, int... newliterals) {
            super(new int[mig.size()], false);
            this.id = id;
            visitor = mig.getVisitor(this.array);
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
    }

    public static final int DEFAULT_ITERATIONS = 1;
    public static final int GLOBAL_SOLUTION_LIMIT = 100_000;

    private List<List<BooleanClause>> nodes;
    private ArrayDeque<BooleanSolution> randomSample;

    private int maxSampleSize = Integer.MAX_VALUE;
    private int iterations = DEFAULT_ITERATIONS;
    private int t;

    private ArrayList<ExpandableIntegerList> indexedSolutions;
    private ArrayList<ExpandableIntegerList> indexedBestSolutions;
    private ArrayList<PartialConfiguration> solutionList;
    private ArrayList<PartialConfiguration> bestResult;
    private final ArrayDeque<PartialConfiguration> candidateConfiguration = new ArrayDeque<>();
    private ExpandableIntegerList[] selectedIndexedSolutions;

    private PartialConfiguration newConfiguration;
    private int curSolutionId;
    private long maxCombinationIndex;
    private int numberOfVariableLiterals;

    private ModalImplicationGraph mig;
    private BooleanAssignment core;
    private BooleanClauseList cnf;

    private Random random;
    private SAT4JSolutionSolver solver;

    private final List<List<BooleanClause>> presenceConditions = new ArrayList<>();

    public void setNodes(List<List<BooleanClause>> nodes) {
        this.nodes = Objects.requireNonNull(nodes);
    }

    public void setIterations(int iterations) {
        if (iterations < 1) {
            throw new IllegalArgumentException(String.valueOf(iterations));
        }
        this.iterations = iterations;
    }

    public void setMaxSampleSize(int maxSampleSize) {
        if (maxSampleSize < 0) {
            throw new IllegalArgumentException(String.valueOf(maxSampleSize));
        }
        this.maxSampleSize = maxSampleSize;
    }

    @Override
    public Result<BooleanSolutionList> compute(DependencyList dependencyList, Progress progress) {
        solver = initializeSolver(dependencyList);
        t = dependencyList.get(T);
        if (t < 1) {
            throw new IllegalArgumentException(String.valueOf(t));
        }
        random = dependencyList.get(RANDOM);
        cnf = dependencyList.get(BOOLEAN_CLAUSE_LIST);
        BooleanAssignment variables = dependencyList.get(VARIABLES_OF_INTEREST);
        //        final int initialAssignmentLength = solver.getAssignment().size();
        //        solver.setSelectionStrategy(ISelectionStrategy.positive()); // todo: fails for berkeley db
        //        Result<BooleanSolution> solution = solver.findSolution();
        //        if (solution.isEmpty()) return Result.empty();
        //        int[] model1 = solution.get().get();
        solver.setSelectionStrategy(ISelectionStrategy.random(random));
        curSolutionId = 0;
        selectedIndexedSolutions = new ExpandableIntegerList[t];
        // Logger.setPrintStackTrace(true);
        if (variables.isEmpty()) {
            nodes = convertLiterals(
                    new BooleanAssignment(IntStream.range(-cnf.getVariableCount(), cnf.getVariableCount() + 1)
                            .filter(i -> i != 0)
                            .toArray()));
        } else {
            nodes = convertLiterals(variables);
        }
        randomSample = new ArrayDeque<>(GLOBAL_SOLUTION_LIMIT);

        final MIGBuilder migBuilder = new MIGBuilder(Computations.of(cnf));
        mig = Computations.await(migBuilder);
        core = new BooleanAssignment(mig.getCore());
        numberOfVariableLiterals = nodes.size() - core.countNegatives() - core.countPositives();

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

        maxCombinationIndex = BinomialCalculator.computeBinomial(presenceConditions.size(), t);
        progress.setTotalSteps((int) (iterations * maxCombinationIndex));
        //		progress.setStatusReporter(new Supplier<>() {
        //			@Override
        //			public String get() {
        //				return String.valueOf(solutionList.size());
        //			}
        //		});

        solutionList = new ArrayList<>();
        buildCombinations(progress, 0);
        FeatJAR.log().debug(solutionList.size() + " (" + bestResult.size() + ")");
        for (int i = 1; i < iterations; i++) {
            trimConfigurations(i);
            buildCombinations(progress, i);
            FeatJAR.log().debug(solutionList.size() + " (" + bestResult.size() + ")");
        }

        bestResult.forEach(this::autoComplete);
        Collections.reverse(bestResult);
        BooleanSolutionList list = new BooleanSolutionList(cnf.getVariableCount());
        bestResult.stream().map(c -> new BooleanSolution(c.get())).forEach(list::add);
        return Result.of(list);
    }

    private void trimConfigurations(int iteration) {
        final int indexSize = 2 * mig.size();
        if (indexedBestSolutions == null) {
            indexedBestSolutions = new ArrayList<>(indexSize);
            for (int i = 0; i < indexSize; i++) {
                indexedBestSolutions.add(new ExpandableIntegerList());
            }
            for (PartialConfiguration solution : bestResult) {
                addIndexBestSolutions(solution);
            }
            for (ExpandableIntegerList indexList : indexedBestSolutions) {
                indexList.sort();
            }
        }

        indexedSolutions = new ArrayList<>(indexSize);
        for (int i = 0; i < indexSize; i++) {
            indexedSolutions.add(new ExpandableIntegerList());
        }

        final long[] normConfigValues = getConfigScores(solutionList);

        long[] normConfigValuesSorted = Arrays.copyOf(normConfigValues, normConfigValues.length);
        Arrays.sort(normConfigValuesSorted);
        final int meanSearch = Arrays.binarySearch(normConfigValuesSorted, (long)
                LongStream.of(normConfigValues).average().getAsDouble());
        final int meanIndex = meanSearch >= 0 ? meanSearch : -meanSearch - 1;
        final long reference = normConfigValuesSorted[
                (int) (normConfigValues.length
                        - ((normConfigValues.length - meanIndex) * ((double) iteration / iterations)))];

        ArrayList<PartialConfiguration> newSolutionList = new ArrayList<>(solutionList.size());
        int index = 0;
        for (PartialConfiguration solution : solutionList) {
            if (normConfigValues[index++] >= reference) {
                addIndexSolutions(solution);
                newSolutionList.add(new PartialConfiguration(solution));
            }
        }
        solutionList = newSolutionList;

        for (ExpandableIntegerList indexList : indexedSolutions) {
            indexList.sort();
        }
    }

    private long[] getConfigScores(List<PartialConfiguration> sample) {
        final int configLength = sample.size();

        final int n = cnf.getVariableCount();
        final int t2 = (n < t) ? n : t;
        final int n2 = n - t2;
        final int pow = (int) Math.pow(2, t2);

        final long[][] configScores = new long[pow][configLength];

        int[] sampleIndex0 = IntStream.range(0, configLength).toArray();
        IntStream.range(0, pow) //
                .parallel() //
                .forEach(maskIndex -> {
                    long[] configScore = configScores[maskIndex];
                    boolean[] mask = new boolean[t2];
                    for (int j = 0; j < t2; j++) {
                        mask[j] = (maskIndex >> j & 1) == 0;
                    }

                    int[][] sampleIndex = new int[t2 + 1][];
                    sampleIndex[0] = sampleIndex0;
                    for (int i = 1; i < sampleIndex.length; i++) {
                        sampleIndex[i] = new int[configLength];
                    }

                    int[] literals = new int[t2];
                    int liSample = 0;

                    final int[] c = new int[t2];
                    for (int i = 0; i < t2; i++) {
                        c[i] = i;
                    }
                    int i = 0;

                    combinationLoop:
                    while (true) {
                        liSample = Math.min(liSample, i);

                        for (int k = 0; k < t2; k++) {
                            int literal = mask[k] ? (c[k] + 1) : -(c[k] + 1);
                            if (core.containsAnyVariable(literal)) {
                                i = k;
                                for (; i >= 0; i--) {
                                    final int ci = ++c[i];
                                    if (ci < (n2 + i)) {
                                        break;
                                    }
                                }
                                if (i == -1) {
                                    break combinationLoop;
                                }
                                for (int j = i + 1; j < t2; j++) {
                                    c[j] = c[j - 1] + 1;
                                }
                                continue combinationLoop;
                            }
                            literals[k] = literal;
                        }

                        for (int k = liSample; k < t2; k++) {
                            final int index = c[k];
                            final int literalValue = literals[k];
                            int[] sampleIndex1 = sampleIndex[k];
                            int[] sampleIndex2 = sampleIndex[k + 1];

                            int sindex2 = 0;
                            for (int sindex1 : sampleIndex1) {
                                if (sindex1 == -1 || sindex1 >= configLength) {
                                    break;
                                }
                                int[] config = sample.get(sindex1).get();
                                if (config[index] == literalValue) {
                                    sampleIndex2[sindex2++] = sindex1;
                                }
                            }
                            if (sindex2 < sampleIndex2.length) {
                                sampleIndex2[sindex2] = -1;
                            }
                        }
                        liSample = i;

                        final int[] sampleIndexK = sampleIndex[t2];

                        int count = 0;
                        for (int l = 0; l < sampleIndexK.length; l++) {
                            int j = sampleIndexK[l];
                            if (j < 0) {
                                count = l;
                                break;
                            }
                        }

                        final double s = count == 1 ? 1 : 0;
                        for (int l = 0; l < count; l++) {
                            configScore[sampleIndexK[l]] += s;
                        }

                        i = t2 - 1;
                        for (; i >= 0; i--) {
                            final int ci = ++c[i];
                            if (ci < (n2 + i)) {
                                break;
                            }
                        }

                        if (i == -1) {
                            break;
                        }
                        for (int j = i + 1; j < t2; j++) {
                            c[j] = c[j - 1] + 1;
                        }
                    }
                });

        int confIndex = 0;
        final long[] configScore = configScores[0];
        for (int j = 1; j < pow; j++) {
            final long[] configScoreJ = configScores[j];
            for (int k = 1; k < configLength; k++) {
                configScore[k] += configScoreJ[k];
            }
        }
        for (final BooleanSolution configuration : sample) {
            int count = 0;
            for (final int literal : configuration.get()) {
                if (literal != 0) {
                    count++;
                }
            }
            final double factor = Math.pow((2.0 - (((double) count) / configuration.size())), t);
            configScore[confIndex] = (long) Math.round(configScore[confIndex] * factor);
            confIndex++;
        }

        return configScore;
    }

    private void buildCombinations(Progress monitor, int phase) {
        // TODO Variation Point: Combination order
        shuffleSort();

        final int[] literals = new int[presenceConditions.size()];
        for (int i1 = 0; i1 < literals.length; i1++) {
            literals[i1] = presenceConditions.get(i1).get(0).get()[0];
        }

        final int[] combinationLiterals = new int[t];

        if (phase == 0) {
            final int indexSize = 2 * mig.size();
            indexedSolutions = new ArrayList<>(indexSize);
            for (int i2 = 0; i2 < indexSize; i2++) {
                indexedSolutions.add(new ExpandableIntegerList());
            }
            LexicographicIterator.stream(t, presenceConditions.size()).forEach(combo -> {
                int[] next = combo.elementIndices;
                monitor.incrementCurrentStep();
                for (int i = 0; i < next.length; i++) {
                    combinationLiterals[i] = literals[next[i]];
                }

                if (isCovered(combinationLiterals, indexedSolutions)) {
                    return;
                }
                if (isCombinationInvalidMIG(combinationLiterals)) {
                    return;
                }

                if (isCombinationValidSample(combinationLiterals)) {
                    if (firstCover(combinationLiterals)) {
                        return;
                    }
                } else {
                    if (isCombinationInvalidSAT(combinationLiterals)) {
                        return;
                    }
                    addToCandidateList(combinationLiterals);
                }
                // if (firstCover(combinationLiterals)) {
                // continue;
                // }
                // if (!isCombinationValidSample(combinationLiterals) &&
                // isCombinationInvalidSAT(combinationLiterals)) {
                // continue;
                // }

                if (coverSat(combinationLiterals)) {
                    return;
                }
                newConfiguration(combinationLiterals);
            });
            bestResult = solutionList;
        } else {
            //			long remainingCombinations = maxCombinationIndex;
            LexicographicIterator.stream(t, presenceConditions.size()).forEach(combo -> {
                int[] next = combo.elementIndices;
                monitor.incrementCurrentStep();
                //				remainingCombinations--;
                for (int i = 0; i < next.length; i++) {
                    combinationLiterals[i] = literals[next[i]];
                }

                if (isCovered(combinationLiterals, indexedSolutions)) {
                    return;
                }
                if (!isCovered(combinationLiterals, indexedBestSolutions)) {
                    return;
                }
                if (firstCover(combinationLiterals)) {
                    return;
                }
                if (coverSat(combinationLiterals)) {
                    return;
                }
                //				if (solutionList.size() == bestResult.size()) {
                //					monitor.addCurrentSteps((int) remainingCombinations);
                //					return;
                //				}
                newConfiguration(combinationLiterals);
            });
            if (bestResult.size() > solutionList.size()) {
                bestResult = solutionList;
            }
        }
    }

    private void shuffleSort() {
        final Map<Integer, List<List<BooleanClause>>> groupedPCs =
                presenceConditions.stream().collect(Collectors.groupingBy(List::size));
        for (final List<List<BooleanClause>> pcList : groupedPCs.values()) {
            Collections.shuffle(pcList, random);
        }
        final List<Map.Entry<Integer, List<List<BooleanClause>>>> shuffledPCs = new ArrayList<>(groupedPCs.entrySet());
        Collections.sort(shuffledPCs, (a, b) -> a.getKey() - b.getKey());
        presenceConditions.clear();
        for (final Map.Entry<Integer, List<List<BooleanClause>>> entry : shuffledPCs) {
            presenceConditions.addAll(entry.getValue());
        }
    }

    private void addIndexBestSolutions(PartialConfiguration solution) {
        final int[] literals = solution.get();
        for (int i = 0; i < literals.length; i++) {
            final int literal = literals[i];
            if (literal != 0) {
                final int vertexIndex = ModalImplicationGraph.getVertexIndex(literal);
                ExpandableIntegerList indexList = indexedBestSolutions.get(vertexIndex);
                indexList.add(solution.id);
            }
        }
    }

    private void addIndexSolutions(PartialConfiguration solution) {
        for (int literal : solution.get()) {
            if (literal != 0) {
                indexedSolutions
                        .get(ModalImplicationGraph.getVertexIndex(literal))
                        .add(solution.id);
            }
        }
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
            selectedIndexedSolutions[i] = indexedSolution;
        }
        Arrays.sort(selectedIndexedSolutions, (a, b) -> a.size() - b.size());
        final int[] ix = new int[t - 1];

        final ExpandableIntegerList i0 = selectedIndexedSolutions[0];
        final int[] ia0 = i0.toArray();
        loop:
        for (int i = 0; i < i0.size(); i++) {
            int id0 = ia0[i];
            for (int j = 1; j < t; j++) {
                final ExpandableIntegerList i1 = selectedIndexedSolutions[j];
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

    private void select(PartialConfiguration solution, int[] literals) {
        final int lastIndex = solution.setLiteral(literals);
        for (int i = lastIndex; i < solution.visitor.getAddedLiteralCount(); i++) {
            ExpandableIntegerList indexList = indexedSolutions.get(
                    ModalImplicationGraph.getVertexIndex(solution.visitor.getAddedLiterals()[i]));
            final int idIndex = Arrays.binarySearch(indexList.toArray(), 0, indexList.size(), solution.id);
            if (idIndex < 0) {
                indexList.add(solution.id, -(idIndex + 1));
            }
        }
        solution.updateSolutionList(lastIndex);
    }

    private boolean firstCover(int[] literals) {
        candidateConfiguration.clear();
        if (newConfiguration != null) {
            configLoop:
            for (final PartialConfiguration configuration : solutionList) {
                if (!configuration.isComplete()) {
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
        } else {
            configLoop:
            for (final PartialConfiguration configuration : solutionList) {
                if (!configuration.isComplete()) {
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
        }
        return false;
    }

    private void addToCandidateList(int[] literals) {
        candidateConfiguration.clear();
        if (newConfiguration != null) {
            configLoop:
            for (final PartialConfiguration configuration : solutionList) {
                if (!configuration.isComplete()) {
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
            for (final PartialConfiguration configuration : solutionList) {
                if (!configuration.isComplete()) {
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
        Collections.sort(solutionList, (a, b) -> b.countLiterals() - a.countLiterals());
    }

    private boolean isCombinationInvalidMIG(int[] literals) {
        if (newConfiguration != null) {
            newConfiguration.visitor.reset();
            try {
                newConfiguration.visitor.propagate(literals);
            } catch (RuntimeContradictionException e) {
                newConfiguration.visitor.reset();
                return true;
            }
        } else {
            try {
                newConfiguration = new PartialConfiguration(curSolutionId++, mig, literals);
            } catch (RuntimeContradictionException e) {
                return true;
            }
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
                    final BooleanSolution e = addSolverSolution();

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

    private boolean coverSat(int[] literals) {
        for (PartialConfiguration configuration : candidateConfiguration) {
            if (trySelectSat(configuration, literals)) {
                change(configuration);
                return true;
            }
        }
        return false;
    }

    private void newConfiguration(int[] literals) {
        if (solutionList.size() < maxSampleSize) {
            if (newConfiguration == null) {
                newConfiguration = new PartialConfiguration(curSolutionId++, mig, literals);
            }
            newConfiguration.initSolutionList();
            solutionList.add(newConfiguration);
            change(newConfiguration);
            for (int i = 0; i < newConfiguration.visitor.getAddedLiteralCount(); i++) {
                ExpandableIntegerList indexList = indexedSolutions.get(ModalImplicationGraph.getVertexIndex(
                        newConfiguration.visitor.getAddedLiterals()[i]));
                indexList.add(newConfiguration.id);
            }
        }
        newConfiguration = null;
    }

    private void autoComplete(PartialConfiguration configuration) {
        if (!configuration.isComplete()) {
            if (configuration.solverSolutions.size() > 0) {
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
                        ExpandableIntegerList indexList = indexedSolutions.get(ModalImplicationGraph.getVertexIndex(
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
        if (randomSample.size() == GLOBAL_SOLUTION_LIMIT) {
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

    @Override
    protected SAT4JSolver newSolver(BooleanClauseList clauseList) {
        return new SAT4JSolutionSolver(clauseList);
    }
}
