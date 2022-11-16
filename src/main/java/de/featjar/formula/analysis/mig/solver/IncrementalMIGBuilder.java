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
package de.featjar.formula.analysis.mig.solver;

import de.featjar.formula.analysis.sat4j.solver.SStrategy;
import de.featjar.formula.analysis.sat4j.solver.Sat4JSolutionSolver;
import de.featjar.formula.structure.map.TermMap;
import de.featjar.base.task.Monitor;
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
    private HashSet<SortedIntegerList> addedSortedIntegerLists;
    private TermMap variables;

    public IncrementalMIGBuilder(ModalImplicationGraph oldModalImplicationGraph) {
        this.oldModalImplicationGraph = oldModalImplicationGraph;
    }

    @Override
    public ModalImplicationGraph execute(CNF cnf, Monitor monitor) {
        Objects.requireNonNull(cnf);
        Objects.requireNonNull(oldModalImplicationGraph);

        collect(cnf);
        monitor.addStep();

        if (!satCheck(cnf)) {
            throw new SolverContradictionException("CNF is not satisfiable!");
        }
        monitor.addStep();
        core(cnf, monitor);
        monitor.addStep();

        cleanClauses();
        monitor.addStep();

        if (detectStrong) {
            checkOldStrong();

            if (add) {
                addClauses(cnf, false, monitor.createChildMonitor(10));

                bfsStrong(monitor.createChildMonitor(10));

                final SortedIntegerList affectedVariables = new SortedIntegerList(
                        addedSortedIntegerLists.stream() //
                                .map(c ->
                                        c.adapt(variables, cnf.getVariableMap()).get()) //
                                .flatMapToInt(c -> IntStream.of(c.getIntegers())) //
                                .map(Math::abs) //
                                .distinct() //
                                .toArray(), //
                        SortedIntegerList.Order.NATURAL);
                bfsWeak(affectedVariables, monitor.createChildMonitor(1000));
            }
            modalImplicationGraph.setStrongStatus(ModalImplicationGraph.BuildStatus.Incremental);
        } else {
            modalImplicationGraph.setStrongStatus(ModalImplicationGraph.BuildStatus.None);
        }

        add(cnf, checkRedundancy, addedSortedIntegerLists);

        bfsStrong(monitor);
        monitor.addStep();

        finish();
        monitor.addStep();
        return modalImplicationGraph;
    }

    public static double getChangeRatio(CNF cnf1, CNF cnf2) {
        final Set<String> allVariables = new HashSet<>(cnf2.getVariableMap().getVariableNames());
        allVariables.addAll(cnf1.getVariableMap().getVariableNames());
        final TermMap variables = new TermMap(allVariables);

        final HashSet<SortedIntegerList> adaptedNewSortedIntegerLists = cnf1.getClauseList().stream()
                .map(c -> c.adapt(cnf1.getVariableMap(), variables).get()) //
                .peek(c -> c.setOrder(SortedIntegerList.Order.NATURAL))
                .collect(Collectors.toCollection(HashSet::new));

        final HashSet<SortedIntegerList> adaptedOldSortedIntegerLists = cnf2.getClauseList().stream() //
                .map(c -> c.adapt(cnf2.getVariableMap(), variables).get()) //
                .peek(c -> c.setOrder(SortedIntegerList.Order.NATURAL)) //
                .collect(Collectors.toCollection(HashSet::new));

        final HashSet<SortedIntegerList> addedSortedIntegerLists = adaptedNewSortedIntegerLists.stream() //
                .filter(c -> !adaptedOldSortedIntegerLists.contains(c)) //
                .collect(Collectors.toCollection(HashSet::new));
        final HashSet<SortedIntegerList> removedSortedIntegerLists = adaptedOldSortedIntegerLists.stream() //
                .filter(c -> !adaptedNewSortedIntegerLists.contains(c)) //
                .collect(Collectors.toCollection(HashSet::new));

        final HashSet<SortedIntegerList> allSortedIntegerLists = new HashSet<>(adaptedNewSortedIntegerLists);
        allSortedIntegerLists.addAll(adaptedOldSortedIntegerLists);
        return (addedSortedIntegerLists.size() + removedSortedIntegerLists.size()) / (double) allSortedIntegerLists.size();
    }

    private void collect(CNF cnf) {
        init(cnf);

        final CNF oldCnf = oldModalImplicationGraph.getCnf();
        final Set<String> allVariables = new HashSet<>(oldCnf.getVariableMap().getVariableNames());
        allVariables.addAll(cnf.getVariableMap().getVariableNames());
        variables = new TermMap(allVariables);

        final HashSet<SortedIntegerList> adaptedNewSortedIntegerLists = cnf.getClauseList().stream()
                .map(c -> c.adapt(cnf.getVariableMap(), variables).get()) //
                .peek(c -> c.setOrder(SortedIntegerList.Order.NATURAL))
                .collect(Collectors.toCollection(HashSet::new));

        final HashSet<SortedIntegerList> adaptedOldSortedIntegerLists = oldCnf.getClauseList().stream() //
                .map(c -> c.adapt(oldCnf.getVariableMap(), variables).get()) //
                .peek(c -> c.setOrder(SortedIntegerList.Order.NATURAL)) //
                .collect(Collectors.toCollection(HashSet::new));

        addedSortedIntegerLists = adaptedNewSortedIntegerLists.stream() //
                .filter(c -> !adaptedOldSortedIntegerLists.contains(c)) //
                .collect(Collectors.toCollection(HashSet::new));
        final HashSet<SortedIntegerList> removedSortedIntegerLists = adaptedOldSortedIntegerLists.stream() //
                .filter(c -> !adaptedNewSortedIntegerLists.contains(c)) //
                .collect(Collectors.toCollection(HashSet::new));

        changes = addedSortedIntegerLists.isEmpty()
                ? removedSortedIntegerLists.isEmpty() ? Changes.UNCHANGED : Changes.REMOVED
                : removedSortedIntegerLists.isEmpty() ? Changes.ADDED : Changes.REPLACED;

        final HashSet<SortedIntegerList> allSortedIntegerLists = new HashSet<>(adaptedNewSortedIntegerLists);
        allSortedIntegerLists.addAll(adaptedOldSortedIntegerLists);
        //		changeRatio = (addedClauses.size() + removedClauses.size()) / (double) allClauses.size();
    }

    private void core(CNF cnf, Monitor monitor) {
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
                    solver.getAssumptionList().push(literal);
                    fixedFeatures[Math.abs(literal) - 1] = 0;
                }
                findCoreFeatures(monitor);
                break;
            case REMOVED:
                checkOldCoreLiterals(coreDead);
                break;
            case REPLACED:
                checkOldCoreLiterals(coreDead);
                for (final int literal : coreDead) {
                    fixedFeatures[Math.abs(literal) - 1] = 0;
                }
                findCoreFeatures(monitor);
                break;
            case UNCHANGED:
                break;
            default:
                throw new IllegalStateException(String.valueOf(changes));
        }
    }

    private long add(CNF cnf, boolean checkRedundancy, Collection<SortedIntegerList> addedSortedIntegerLists) {
        Stream<SortedIntegerList> cnfStream = cleanedClausesList.stream();
        if (checkRedundancy) {
            final Set<SortedIntegerList> oldMigSortedIntegerLists = oldModalImplicationGraph.getVertices().stream()
                    .flatMap(v -> v.getComplexClauses().stream())
                    .collect(Collectors.toCollection(HashSet::new));
            final HashSet<SortedIntegerList> redundantSortedIntegerLists = oldModalImplicationGraph.getCnf().getClauseList().stream()
                    .map(c -> cleanClause(c, oldModalImplicationGraph)) //
                    .filter(Objects::nonNull) //
                    .filter(c -> c.size() > 2) //
                    .filter(c -> !oldMigSortedIntegerLists.contains(c)) //
                    .map(c ->
                            c.adapt(oldModalImplicationGraph.getCnf().getVariableMap(), variables).get()) //
                    .peek(c -> c.setOrder(SortedIntegerList.Order.NATURAL)) //
                    .collect(Collectors.toCollection(HashSet::new));

            cnfStream = cnfStream //
                    .map(c -> c.adapt(cnf.getVariableMap(), variables).get()) //
                    .peek(c -> c.setOrder(SortedIntegerList.Order.NATURAL));

            switch (changes) {
                case ADDED: {
                    if (add) {
                        final Sat4JSolutionSolver redundancySolver = new Sat4JSolutionSolver(new CNF(variables));
                        final int[] affectedVariables = addedSortedIntegerLists.stream()
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
                                    if (redundantSortedIntegerLists.contains(c)) {
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
                                .filter(c -> (c.size() < 3) || !redundantSortedIntegerLists.contains(c));
                    }
                    modalImplicationGraph.setRedundancyStatus(ModalImplicationGraph.BuildStatus.Incremental);
                    break;
                }
                case REMOVED: {
                    final Sat4JSolutionSolver redundancySolver = new Sat4JSolutionSolver(new CNF(variables));
                    cnfStream = cnfStream
                            .sorted(lengthComparator)
                            .distinct()
                            .filter(c -> {
                                if (c.size() < 3) {
                                    return true;
                                }
                                if (redundantSortedIntegerLists.contains(c)) {
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
                        final Sat4JSolutionSolver redundancySolver = new Sat4JSolutionSolver(new CNF(variables));
                        final int[] affectedVariables = addedSortedIntegerLists.stream()
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
                                    if (redundantSortedIntegerLists.contains(c)) {
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
                        final Sat4JSolutionSolver redundancySolver = new Sat4JSolutionSolver(new CNF(variables));
                        cnfStream = cnfStream
                                .sorted(lengthComparator)
                                .distinct()
                                .filter(c -> (c.size() < 3)
                                        || !redundantSortedIntegerLists.contains(c)
                                        || !isRedundant(redundancySolver, c))
                                .peek(redundancySolver.getFormula()::push);
                    }
                    modalImplicationGraph.setRedundancyStatus(ModalImplicationGraph.BuildStatus.Incremental);
                    break;
                }
                case UNCHANGED: {
                    cnfStream = cnfStream.distinct().filter(c -> (c.size() < 3) || !redundantSortedIntegerLists.contains(c));
                    modalImplicationGraph.setRedundancyStatus(modalImplicationGraph.getRedundancyStatus());
                    break;
                }
                default:
                    throw new IllegalStateException(String.valueOf(changes));
            }
            cnfStream = cnfStream
                    .map(c -> c.adapt(variables, cnf.getVariableMap()).get())
                    .peek(c -> c.setOrder(SortedIntegerList.Order.NATURAL));
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
                for (final SortedIntegerList strongEdge : oldModalImplicationGraph.getDetectedStrong()) {
                    final SortedIntegerList adaptSortedIntegerList = strongEdge
                            .adapt(
                                    oldModalImplicationGraph.getCnf().getVariableMap(),
                                    modalImplicationGraph.getCnf().getVariableMap())
                            .get();
                    if (adaptSortedIntegerList != null) {
                        final int[] literals = adaptSortedIntegerList.getIntegers();
                        final int l1 = -literals[0];
                        final int l2 = -literals[1];
                        for (final SortedIntegerList solution : solver.getSolutionHistory()) {
                            if (solution.containsAll(l1, l2)) {
                                continue loop;
                            }
                        }
                        solver.getAssumptionList().push(l1);
                        solver.getAssumptionList().push(l2);
                        switch (solver.hasSolution()) {
                            case FALSE:
                                cleanedClausesList.add(adaptSortedIntegerList);
                                modalImplicationGraph.getDetectedStrong().add(adaptSortedIntegerList);
                            case TIMEOUT:
                            case TRUE:
                                break;
                        }
                        solver.getAssumptionList().pop();
                        solver.getAssumptionList().pop();
                    }
                }
                break;
            case ADDED:
            case UNCHANGED:
                for (final SortedIntegerList strongEdge : oldModalImplicationGraph.getDetectedStrong()) {
                    final SortedIntegerList adaptSortedIntegerList = strongEdge
                            .adapt(
                                    oldModalImplicationGraph.getCnf().getVariableMap(),
                                    modalImplicationGraph.getCnf().getVariableMap())
                            .get();
                    if (adaptSortedIntegerList != null) {
                        cleanedClausesList.add(adaptSortedIntegerList);
                        modalImplicationGraph.getDetectedStrong().add(adaptSortedIntegerList);
                    }
                }
                break;
            default:
                throw new IllegalStateException(String.valueOf(changes));
        }
    }

    protected void checkOldCoreLiterals(int[] coreDead) {
        solver.setSelectionStrategy(SStrategy.inverse(fixedFeatures));
        for (final int literal : coreDead) {
            final int varX = fixedFeatures[Math.abs(literal) - 1];
            if (varX == 0) {
                modalImplicationGraph.getVertex(-literal).setStatus(Vertex.Status.Normal);
                modalImplicationGraph.getVertex(literal).setStatus(Vertex.Status.Normal);
            } else {
                solver.getAssumptionList().push(-varX);
                switch (solver.hasSolution()) {
                    case FALSE:
                        solver.getAssumptionList().replaceLast(varX);
                        modalImplicationGraph.getVertex(varX).setStatus(Vertex.Status.Core);
                        modalImplicationGraph.getVertex(-varX).setStatus(Vertex.Status.Dead);
                        break;
                    case TIMEOUT:
                        solver.getAssumptionList().pop();
                        fixedFeatures[Math.abs(literal) - 1] = 0;
                        modalImplicationGraph.getVertex(-varX).setStatus(Vertex.Status.Normal);
                        modalImplicationGraph.getVertex(varX).setStatus(Vertex.Status.Normal);
                        break;
                    case TRUE:
                        solver.getAssumptionList().pop();
                        modalImplicationGraph.getVertex(-varX).setStatus(Vertex.Status.Normal);
                        modalImplicationGraph.getVertex(varX).setStatus(Vertex.Status.Normal);
                        SortedIntegerList.resetConflicts(fixedFeatures, solver.getInternalSolution());
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
