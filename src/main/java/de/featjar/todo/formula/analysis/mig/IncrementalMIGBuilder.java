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
package de.featjar.todo.formula.analysis.mig;

import de.featjar.formula.analysis.RuntimeContradictionException;
import de.featjar.formula.analysis.bool.BooleanAssignmentList;
import de.featjar.formula.analysis.bool.BooleanClause;
import de.featjar.formula.analysis.mig.solver.MIGBuilder;
import de.featjar.formula.analysis.mig.solver.ModalImplicationGraph;
import de.featjar.formula.analysis.mig.solver.ModalImplicationGraph.BuildStatus;
import de.featjar.formula.analysis.sat4j.solver.ISelectionStrategy;
import de.featjar.formula.analysis.sat4j.solver.SAT4JSolutionSolver;
import de.featjar.formula.analysis.sat4j.solver.SAT4JSolver;
import de.featjar.formula.structure.map.TermMap;
import de.featjar.base.computation.Progress;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class IncrementalMIGBuilder extends MIGBuilder {

    private enum Changes {
        UNCHANGED,
        ADDED,
        REMOVED,
        REPLACED
    }

    private final ModalImplicationGraph oldModalImplicationGraph;

    private boolean add = false;

    private Changes changes;
    private HashSet<BooleanClause> addedBooleanClauses;
    private TermMap variables;

    public IncrementalMIGBuilder(ModalImplicationGraph oldModalImplicationGraph) {
        this.oldModalImplicationGraph = oldModalImplicationGraph;
    }

    @Override
    public ModalImplicationGraph execute(BooleanAssignmentList cnf, Progress progress) {
        Objects.requireNonNull(cnf);
        Objects.requireNonNull(oldModalImplicationGraph);

        collect(cnf);
        progress.incrementCurrentStep();

        if (!satCheck(cnf)) {
            throw new RuntimeContradictionException("CNF is not satisfiable!");
        }
        progress.incrementCurrentStep();
        core(cnf);
        progress.incrementCurrentStep();

        cleanClauses();
        progress.incrementCurrentStep();

        if (detectStrong) {
            checkOldStrong();

            if (add) {
                addClauses(cnf, false);

                bfsStrong();

                final BooleanClause affectedVariables = new BooleanClause(
                        addedBooleanClauses.stream() //
                                .map(c ->
                                        c.adapt(variables, cnf.getVariableMap()).get()) //
                                .flatMapToInt(c -> IntStream.of(c.getIntegers())) //
                                .map(Math::abs) //
                                .distinct() //
                                .toArray(), //
                        BooleanClause.Order.NATURAL);
                bfsWeak(affectedVariables);
            }
            modalImplicationGraph.setStrongStatus(ModalImplicationGraph.BuildStatus.Incremental);
        } else {
            modalImplicationGraph.setStrongStatus(ModalImplicationGraph.BuildStatus.None);
        }

        add(cnf, checkRedundancy, addedBooleanClauses);

        bfsStrong();
        progress.incrementCurrentStep();

        finish();
        progress.incrementCurrentStep();
        return modalImplicationGraph;
    }

    public static double getChangeRatio(BooleanAssignmentList cnf1, BooleanAssignmentList cnf2) {
        final Set<String> allVariables = new HashSet<>(cnf2.getVariableMap().getVariableNames());
        allVariables.addAll(cnf1.getVariableMap().getVariableNames());
        final TermMap variables = new TermMap(allVariables);

        final HashSet<BooleanClause> adaptedNewBooleanClauses = cnf1.getClauseList().stream()
                .map(c -> c.adapt(cnf1.getVariableMap(), variables).get()) //
                .peek(c -> c.setOrder(BooleanClause.Order.NATURAL))
                .collect(Collectors.toCollection(HashSet::new));

        final HashSet<BooleanClause> adaptedOldBooleanClauses = cnf2.getClauseList().stream() //
                .map(c -> c.adapt(cnf2.getVariableMap(), variables).get()) //
                .peek(c -> c.setOrder(BooleanClause.Order.NATURAL)) //
                .collect(Collectors.toCollection(HashSet::new));

        final HashSet<BooleanClause> addedBooleanClauses = adaptedNewBooleanClauses.stream() //
                .filter(c -> !adaptedOldBooleanClauses.contains(c)) //
                .collect(Collectors.toCollection(HashSet::new));
        final HashSet<BooleanClause> removedBooleanClauses = adaptedOldBooleanClauses.stream() //
                .filter(c -> !adaptedNewBooleanClauses.contains(c)) //
                .collect(Collectors.toCollection(HashSet::new));

        final HashSet<BooleanClause> allBooleanClauses = new HashSet<>(adaptedNewBooleanClauses);
        allBooleanClauses.addAll(adaptedOldBooleanClauses);
        return (addedBooleanClauses.size() + removedBooleanClauses.size()) / (double) allBooleanClauses.size();
    }

    private void collect(BooleanAssignmentList cnf) {
        init(cnf);

        final BooleanAssignmentList oldCnf = oldModalImplicationGraph.getCnf();
        final Set<String> allVariables = new HashSet<>(oldCnf.getVariableMap().getVariableNames());
        allVariables.addAll(cnf.getVariableMap().getVariableNames());
        variables = new TermMap(allVariables);

        final HashSet<BooleanClause> adaptedNewBooleanClauses = cnf.getClauseList().stream()
                .map(c -> c.adapt(cnf.getVariableMap(), variables).get()) //
                .peek(c -> c.setOrder(BooleanClause.Order.NATURAL))
                .collect(Collectors.toCollection(HashSet::new));

        final HashSet<BooleanClause> adaptedOldBooleanClauses = oldCnf.getClauseList().stream() //
                .map(c -> c.adapt(oldCnf.getVariableMap(), variables).get()) //
                .peek(c -> c.setOrder(BooleanClause.Order.NATURAL)) //
                .collect(Collectors.toCollection(HashSet::new));

        addedBooleanClauses = adaptedNewBooleanClauses.stream() //
                .filter(c -> !adaptedOldBooleanClauses.contains(c)) //
                .collect(Collectors.toCollection(HashSet::new));
        final HashSet<BooleanClause> removedBooleanClauses = adaptedOldBooleanClauses.stream() //
                .filter(c -> !adaptedNewBooleanClauses.contains(c)) //
                .collect(Collectors.toCollection(HashSet::new));

        changes = addedBooleanClauses.isEmpty()
                ? removedBooleanClauses.isEmpty() ? Changes.UNCHANGED : Changes.REMOVED
                : removedBooleanClauses.isEmpty() ? Changes.ADDED : Changes.REPLACED;

        final HashSet<BooleanClause> allBooleanClauses = new HashSet<>(adaptedNewBooleanClauses);
        allBooleanClauses.addAll(adaptedOldBooleanClauses);
        //		changeRatio = (addedClauses.size() + removedClauses.size()) / (double) allClauses.size();
    }

    private void core(BooleanAssignmentList cnf) {
        final int[] coreDead = oldModalImplicationGraph.getVertices().stream() //
                .filter(Vertex::isCore) //
                .mapToInt(Vertex::getVar) //
                .map(l -> Deprecated.adapt(l, oldModalImplicationGraph.getCnf().getVariableMap(), cnf.getVariableMap())) //
                .filter(l -> l != 0) //
                .peek(l -> {
                    modalImplicationGraph.getVertex(l).setStatus(Vertex.Status.Core);
                    modalImplicationGraph.getVertex(-l).setStatus(Vertex.Status.Dead);
                })
                .toArray();
        switch (changes) {
            case ADDED:
                for (final int literal : coreDead) {
                    solver.getAssignment().add(literal);
                    fixedFeatures[Math.abs(literal) - 1] = 0;
                }
                findCoreFeatures();
                break;
            case REMOVED:
                checkOldCoreLiterals(coreDead);
                break;
            case REPLACED:
                checkOldCoreLiterals(coreDead);
                for (final int literal : coreDead) {
                    fixedFeatures[Math.abs(literal) - 1] = 0;
                }
                findCoreFeatures();
                break;
            case UNCHANGED:
                break;
            default:
                throw new IllegalStateException(String.valueOf(changes));
        }
    }

    private long add(BooleanAssignmentList cnf, boolean checkRedundancy, Collection<BooleanClause> addedBooleanClauses) {
        Stream<BooleanClause> cnfStream = cleanedClausesList.stream();
        if (checkRedundancy) {
            final Set<BooleanClause> oldMigBooleanClauses = oldModalImplicationGraph.getVertices().stream()
                    .flatMap(v -> v.getComplexClauses().stream())
                    .collect(Collectors.toCollection(HashSet::new));
            final HashSet<BooleanClause> redundantBooleanClauses = oldModalImplicationGraph.getCnf().getClauseList().stream()
                    .map(c -> cleanClause(c, oldModalImplicationGraph)) //
                    .filter(Objects::nonNull) //
                    .filter(c -> c.size() > 2) //
                    .filter(c -> !oldMigBooleanClauses.contains(c)) //
                    .map(c ->
                            c.adapt(oldModalImplicationGraph.getCnf().getVariableMap(), variables).get()) //
                    .peek(c -> c.setOrder(BooleanClause.Order.NATURAL)) //
                    .collect(Collectors.toCollection(HashSet::new));

            cnfStream = cnfStream //
                    .map(c -> c.adapt(cnf.getVariableMap(), variables).get()) //
                    .peek(c -> c.setOrder(BooleanClause.Order.NATURAL));

            switch (changes) {
                case ADDED: {
                    if (add) {
                        final SAT4JSolutionSolver redundancySolver = new SAT4JSolutionSolver(new BooleanAssignmentList(variables));
                        final int[] affectedVariables = addedBooleanClauses.stream()
                                .flatMapToInt(c -> IntStream.of(c.getIntegers()))
                                .map(Math::abs)
                                .distinct()
                                .toArray();
                        cnfStream = cnfStream
                                .sorted(lengthComparator)
                                .distinct()
                                .filter(c -> {
                                    if (c.size() < 3) {
                                        return true;
                                    }
                                    if (redundantBooleanClauses.contains(c)) {
                                        return false;
                                    }
                                    if (add && c.containsAny(affectedVariables)) {
                                        return !isRedundant(redundancySolver, c);
                                    }
                                    return true;
                                })
                                .peek(redundancySolver.getFormula()::push);
                    } else {
                        cnfStream = cnfStream
                                .sorted(lengthComparator)
                                .distinct()
                                .filter(c -> (c.size() < 3) || !redundantBooleanClauses.contains(c));
                    }
                    modalImplicationGraph.setRedundancyStatus(ModalImplicationGraph.BuildStatus.Incremental);
                    break;
                }
                case REMOVED: {
                    final SAT4JSolutionSolver redundancySolver = new SAT4JSolutionSolver(new BooleanAssignmentList(variables));
                    cnfStream = cnfStream
                            .sorted(lengthComparator)
                            .distinct()
                            .filter(c -> {
                                if (c.size() < 3) {
                                    return true;
                                }
                                if (redundantBooleanClauses.contains(c)) {
                                    return !isRedundant(redundancySolver, c);
                                }
                                return true;
                            })
                            .peek(redundancySolver.getFormula()::push);
                    modalImplicationGraph.setRedundancyStatus(modalImplicationGraph.getRedundancyStatus());
                    break;
                }
                case REPLACED: {
                    if (add) {
                        final SAT4JSolutionSolver redundancySolver = new SAT4JSolutionSolver(new BooleanAssignmentList(variables));
                        final int[] affectedVariables = addedBooleanClauses.stream()
                                .flatMapToInt(c -> IntStream.of(c.getIntegers()))
                                .map(Math::abs)
                                .distinct()
                                .toArray();
                        cnfStream = cnfStream
                                .sorted(lengthComparator)
                                .distinct()
                                .filter(c -> {
                                    if (c.size() < 3) {
                                        return true;
                                    }
                                    if (redundantBooleanClauses.contains(c)) {
                                        return !isRedundant(redundancySolver, c);
                                    } else {
                                        if (c.containsAny(affectedVariables)) {
                                            return !isRedundant(redundancySolver, c);
                                        }
                                        return true;
                                    }
                                })
                                .peek(redundancySolver.getFormula()::push);
                    } else {
                        final SAT4JSolutionSolver redundancySolver = new SAT4JSolutionSolver(new BooleanAssignmentList(variables));
                        cnfStream = cnfStream
                                .sorted(lengthComparator)
                                .distinct()
                                .filter(c -> (c.size() < 3)
                                        || !redundantBooleanClauses.contains(c)
                                        || !isRedundant(redundancySolver, c))
                                .peek(redundancySolver.getFormula()::push);
                    }
                    modalImplicationGraph.setRedundancyStatus(ModalImplicationGraph.BuildStatus.Incremental);
                    break;
                }
                case UNCHANGED: {
                    cnfStream = cnfStream.distinct().filter(c -> (c.size() < 3) || !redundantBooleanClauses.contains(c));
                    modalImplicationGraph.setRedundancyStatus(modalImplicationGraph.getRedundancyStatus());
                    break;
                }
                default:
                    throw new IllegalStateException(String.valueOf(changes));
            }
            cnfStream = cnfStream
                    .map(c -> c.adapt(variables, cnf.getVariableMap()).get())
                    .peek(c -> c.setOrder(BooleanClause.Order.NATURAL));
        } else {
            cnfStream = cnfStream.distinct();
            modalImplicationGraph.setRedundancyStatus(ModalImplicationGraph.BuildStatus.None);
        }
        return cnfStream.peek(modalImplicationGraph::addClause).count();
    }

    protected void checkOldStrong() {
        switch (changes) {
            case REMOVED:
            case REPLACED:
                loop:
                for (final BooleanClause strongEdge : oldModalImplicationGraph.getDetectedStrong()) {
                    final BooleanClause adaptBooleanClause = strongEdge
                            .adapt(
                                    oldModalImplicationGraph.getCnf().getVariableMap(),
                                    modalImplicationGraph.getCnf().getVariableMap())
                            .get();
                    if (adaptBooleanClause != null) {
                        final int[] literals = adaptBooleanClause.getIntegers();
                        final int l1 = -literals[0];
                        final int l2 = -literals[1];
                        for (final BooleanClause solution : solver.getSolutionHistory()) {
                            if (solution.containsAll(l1, l2)) {
                                continue loop;
                            }
                        }
                        solver.getAssignment().add(l1);
                        solver.getAssignment().add(l2);
                        switch (solver.hasSolution()) {
                            case FALSE:
                                cleanedClausesList.add(adaptBooleanClause);
                                modalImplicationGraph.getDetectedStrong().add(adaptBooleanClause);
                            case TIMEOUT:
                            case TRUE:
                                break;
                        }
                        solver.getAssignment().remove();
                        solver.getAssignment().remove();
                    }
                }
                break;
            case ADDED:
            case UNCHANGED:
                for (final BooleanClause strongEdge : oldModalImplicationGraph.getDetectedStrong()) {
                    final BooleanClause adaptBooleanClause = strongEdge
                            .adapt(
                                    oldModalImplicationGraph.getCnf().getVariableMap(),
                                    modalImplicationGraph.getCnf().getVariableMap())
                            .get();
                    if (adaptBooleanClause != null) {
                        cleanedClausesList.add(adaptBooleanClause);
                        modalImplicationGraph.getDetectedStrong().add(adaptBooleanClause);
                    }
                }
                break;
            default:
                throw new IllegalStateException(String.valueOf(changes));
        }
    }

    protected void checkOldCoreLiterals(int[] coreDead) {
        solver.setSelectionStrategy(ISelectionStrategy.inverse(fixedFeatures));
        for (final int literal : coreDead) {
            final int varX = fixedFeatures[Math.abs(literal) - 1];
            if (varX == 0) {
                modalImplicationGraph.getVertex(-literal).setStatus(Vertex.Status.Normal);
                modalImplicationGraph.getVertex(literal).setStatus(Vertex.Status.Normal);
            } else {
                solver.getAssignment().add(-varX);
                switch (solver.hasSolution()) {
                    case FALSE:
                        solver.getAssignment().replaceLast(varX);
                        modalImplicationGraph.getVertex(varX).setStatus(Vertex.Status.Core);
                        modalImplicationGraph.getVertex(-varX).setStatus(Vertex.Status.Dead);
                        break;
                    case TIMEOUT:
                        solver.getAssignment().remove();
                        fixedFeatures[Math.abs(literal) - 1] = 0;
                        modalImplicationGraph.getVertex(-varX).setStatus(Vertex.Status.Normal);
                        modalImplicationGraph.getVertex(varX).setStatus(Vertex.Status.Normal);
                        break;
                    case TRUE:
                        solver.getAssignment().remove();
                        modalImplicationGraph.getVertex(-varX).setStatus(Vertex.Status.Normal);
                        modalImplicationGraph.getVertex(varX).setStatus(Vertex.Status.Normal);
                        SAT4JSolver.zeroConflicts(fixedFeatures, solver.getInternalSolution());
                        solver.shuffleOrder(random);
                        break;
                }
            }
        }
    }

    public boolean isAdd() {
        return add;
    }

    public void setAdd(boolean add) {
        this.add = add;
    }
}
