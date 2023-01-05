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
package de.featjar.formula.analysis.todo.mig.solver;

import de.featjar.formula.analysis.sat4j.solver.ISelectionStrategy;
import de.featjar.formula.analysis.sat4j.solver.SAT4JSolutionSolver;
import de.featjar.base.computation.Progress;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Adjacency matrix implementation for a feature graph.
 *
 * @author Sebastian Krieter
 */
public abstract class MIGBuilder implements IMonitorableFunction<CNF, ModalImplicationGraph> {

    /**
     * For sorting clauses by length. Starting with the longest.
     */
    protected static final Comparator<SortedIntegerList> lengthComparator = Comparator.comparing(o -> o.getIntegers().length);

    protected final Random random = new Random(112358);

    protected boolean checkRedundancy = true;
    protected boolean detectStrong = true;

    protected SAT4JSolutionSolver solver;
    protected List<SortedIntegerList> cleanedClausesList;
    protected int[] fixedFeatures;

    protected ModalImplicationGraph modalImplicationGraph;

    protected void init(CNF cnf) {
        modalImplicationGraph = new ModalImplicationGraph(cnf);
    }

    protected boolean satCheck(CNF cnf) {
        solver = new SAT4JSolutionSolver(cnf);
        solver.rememberSolutionHistory(1000);
        fixedFeatures = solver.findSolution().getLiterals();
        return fixedFeatures != null;
    }

    protected void findCoreFeatures(IMonitor monitor) {
        monitor.setTotalSteps(fixedFeatures.length);

        solver.setSelectionStrategy(ISelectionStrategy.inverse(fixedFeatures));

        // find core/dead features
        for (int i = 0; i < fixedFeatures.length; i++) {
            final int varX = fixedFeatures[i];
            if (varX != 0) {
                solver.getAssignment().add(-varX);
                final SATSolver.Result<Boolean> hasSolution = solver.hasSolution();
                switch (hasSolution) {
                    case FALSE:
                        solver.getAssignment().replaceLast(varX);
                        modalImplicationGraph.getVertex(-varX).setStatus(Vertex.Status.Dead);
                        modalImplicationGraph.getVertex(varX).setStatus(Vertex.Status.Core);
                        break;
                    case TIMEOUT:
                        solver.getAssignment().remove();
                        break;
                    case TRUE:
                        solver.getAssignment().remove();
                        SortedIntegerList.resetConflicts(fixedFeatures, solver.getInternalSolution());
                        solver.shuffleOrder(random);
                        break;
                }
            }
            monitor.addStep();
        }
        monitor.setDone();
    }

    protected long addClauses(CNF cnf, boolean checkRedundancy, Progress progress) {
        monitor.setTotalSteps(cleanedClausesList.size());
        Stream<SortedIntegerList> stream = cleanedClausesList.stream();
        if (checkRedundancy) {
            final SAT4JSolutionSolver newSolver = new SAT4JSolutionSolver(new CNF(cnf.getVariableMap()));
            stream = stream.sorted(lengthComparator)
                    .distinct()
                    .peek(c -> monitor.addStep())
                    .filter(
                            clause -> //
                            (clause.getIntegers().length < 3) //
                                            || !isRedundant(newSolver, clause)) //
                    .peek(newSolver.getFormula()::push); //
        } else {
            stream = stream.distinct().peek(c -> monitor.addStep());
        }
        final long count = stream.peek(modalImplicationGraph::addClause).count();
        monitor.setDone();
        return count;
    }

    //	protected void addStrongEdges() {
    //		cleanedClausesList.stream().filter(c -> c.size() == 2).distinct().forEach(mig::addClause);
    //	}

    //	protected void addWeakEdges(boolean checkRedundancy, InternalMonitor monitor) {
    //		monitor.setTotalWork(cleanedClausesList.size());
    //		Stream<LiteralList> stream = cleanedClausesList.stream().filter(c -> c.size() > 2);
    //		if (checkRedundancy) {
    ////			int[] dsa = new int[mig.size() + 1];
    //			final Sat4JSolver newSolver = new Sat4JSolver(new CNF(mig.getCnf().getVariableMap()));
    //			stream = stream //
    //					.sorted(lengthComparator) //
    //					.distinct() //
    //					.peek(c -> monitor.step()) //
    ////				.filter(c -> {
    ////					for (int literal : c.getLiterals()) {
    ////						for (Vertex strong : mig.getVertex(-literal).getStrongEdges()) {
    ////							for (int literal2 : c.getLiterals()) {
    ////								if (strong.getVar() == literal2) {
    ////									return false;
    ////								}
    ////							}
    ////						}
    ////					}
    ////					return true;
    ////				}) //
    ////				.filter(c -> {
    ////					for (int literal : c.getLiterals()) {
    ////						dsa[Math.abs(literal)] = literal;
    ////					}
    ////					for (int literal : c.getLiterals()) {
    ////						for (Vertex strong : mig.getVertex(-literal).getStrongEdges()) {
    ////							if (dsa[Math.abs(strong.getVar())] == strong.getVar()) {
    ////								return false;
    ////							}
    ////						}
    ////					}
    ////					for (int literal : c.getLiterals()) {
    ////						dsa[Math.abs(literal)] = 0;
    ////					}
    ////					return true;
    ////				}) //
    //					.filter(c -> !isRedundant(newSolver, c)) //
    //					.peek(newSolver::addClause); //
    //		} else {
    //			stream = stream //
    //					.distinct() //
    //					.peek(c -> monitor.step());
    //		}
    //		stream.forEach(mig::addClause);
    //		monitor.done();
    //	}
    //
    //	protected void addWeakEdges2(boolean checkRedundancy, InternalMonitor monitor) {
    //		monitor.setTotalWork(cleanedClausesList.size());
    //		Stream<LiteralList> stream = cleanedClausesList.stream().filter(c -> c.size() > 2);
    //		if (checkRedundancy) {
    //			stream = stream //
    //					.sorted(lengthComparator) //
    //					.distinct() //
    //					.peek(c -> monitor.step()) //
    //			;
    //		} else {
    //			stream = stream //
    //					.distinct() //
    //					.peek(c -> monitor.step());
    //		}
    //		stream.forEach(mig::addClause);
    //		monitor.done();
    //	}

    protected void cleanClauses() {
        cleanedClausesList = new ArrayList<>(modalImplicationGraph.getCnf().getClauseList().size());
        modalImplicationGraph.getCnf().getClauseList().stream()
                .map(c -> cleanClause(c, modalImplicationGraph))
                .filter(Objects::nonNull)
                .forEach(cleanedClausesList::add);
    }

    protected SortedIntegerList cleanClause(SortedIntegerList clause, ModalImplicationGraph modalImplicationGraph) {
        final int[] literals = clause.getIntegers();
        final LinkedHashSet<Integer> literalSet = new LinkedHashSet<>(literals.length << 1);

        // Sort out dead and core features
        int childrenCount = clause.size();
        for (int i = 0; i < childrenCount; i++) {
            final int var = literals[i];
            modalImplicationGraph.size();
            final Vertex.Status status = modalImplicationGraph.getVertex(var).getStatus();
            switch (status) {
                case Core:
                    return null;
                case Dead:
                    if (childrenCount <= 2) {
                        return null;
                    }
                    childrenCount--;
                    // Switch literals (faster than deletion within an array)
                    literals[i] = literals[childrenCount];
                    literals[childrenCount] = var;
                    i--;
                    break;
                case Normal:
                    if (literalSet.contains(-var)) {
                        return null;
                    } else {
                        literalSet.add(var);
                    }
                    break;
                default:
                    throw new IllegalStateException(String.valueOf(status));
            }
        }
        final int[] literalArray = new int[literalSet.size()];
        int i = 0;
        for (final int lit : literalSet) {
            literalArray[i++] = lit;
        }
        return new SortedIntegerList(literalArray, SortedIntegerList.Order.NATURAL);
    }

    protected final boolean isRedundant(SAT4JSolutionSolver solver, SortedIntegerList curSortedIntegerList) {
        return solver.hasSolution(curSortedIntegerList.negate()) == SATSolver.Result<Boolean>.FALSE;
    }

    protected void bfsStrong(IMonitor monitor) {
        monitor.setTotalSteps(modalImplicationGraph.getVertices().size());
        final boolean[] mark = new boolean[modalImplicationGraph.size() + 1];
        final ArrayDeque<Vertex> queue = new ArrayDeque<>();
        for (final Vertex vertex : modalImplicationGraph.getVertices()) {
            Arrays.fill(mark, false);
            final Vertex complement = modalImplicationGraph.getVertex(-vertex.getVar());

            mark[Math.abs(vertex.getVar())] = true;
            for (final Vertex stronglyConnectedVertex : vertex.getStrongEdges()) {
                mark[Math.abs(stronglyConnectedVertex.getVar())] = true;
                queue.add(stronglyConnectedVertex);
            }
            while (!queue.isEmpty()) {
                final Vertex curVertex = queue.removeFirst();
                for (final Vertex stronglyConnectedVertex : curVertex.getStrongEdges()) {
                    final int index = Math.abs(stronglyConnectedVertex.getVar());
                    if (!mark[index]) {
                        mark[index] = true;
                        queue.add(stronglyConnectedVertex);

                        final Vertex stronglyConnectedComplement = modalImplicationGraph.getVertex(-stronglyConnectedVertex.getVar());
                        vertex.addStronglyConnected(stronglyConnectedVertex);
                        stronglyConnectedComplement.addStronglyConnected(complement);
                    }
                }
            }
            monitor.addStep();
        }
        monitor.setDone();
    }

    protected void bfsWeak(SortedIntegerList affectedVariables, Progress progress) {
        monitor.setTotalSteps(modalImplicationGraph.getVertices().size());
        final ArrayDeque<Vertex> queue = new ArrayDeque<>();
        final ArrayList<Integer> literals = new ArrayList<>();
        final boolean[] mark = new boolean[modalImplicationGraph.size() + 1];
        final int[] fixed = new int[modalImplicationGraph.size() + 1];
        final int orgSize = solver.getAssignment().size();
        solver.setSelectionStrategy(ISelectionStrategy.original());
        for (final Vertex vertex : modalImplicationGraph.getVertices()) {
            if (vertex.isNormal()
                    && ((affectedVariables == null)
                            || affectedVariables.containsAny(Math.abs(vertex.getVar())))) {
                final int var = vertex.getVar();
                final int negVar = -var;
                Arrays.fill(mark, false);
                Arrays.fill(fixed, 0);
                int[] model = null;

                for (final SortedIntegerList solution : solver.getSolutionHistory()) {
                    if (solution.containsAll(var)) {
                        if (model == null) {
                            model = Arrays.copyOf(solution.getIntegers(), solution.size());
                        } else {
                            SortedIntegerList.resetConflicts(model, solution.getIntegers());
                        }
                    }
                }

                solver.getAssignment().add(var);
                fixed[Math.abs(var)] = var;
                mark[Math.abs(var)] = true;
                for (final Vertex strongVertex : vertex.getStrongEdges()) {
                    final int strongVar = strongVertex.getVar();
                    solver.getAssignment().add(strongVar);
                    final int index = Math.abs(strongVar);
                    fixed[index] = strongVar;
                    mark[index] = true;
                    strongVertex.getComplexClauses().stream()
                            .flatMapToInt(c -> IntStream.of(c.getIntegers()))
                            .forEach(literals::add);
                }

                vertex.getComplexClauses().stream()
                        .flatMapToInt(c -> IntStream.of(c.getIntegers()))
                        .forEach(literals::add);

                if (model == null) {
                    model = solver.findSolution().getLiterals();
                }
                solver.setSelectionStrategy(ISelectionStrategy.inverse(model));

                for (final Integer literal : literals) {
                    final int index = Math.abs(literal);
                    if (!mark[index]) {
                        mark[index] = true;
                        queue.add(modalImplicationGraph.getVertex(literal));
                    }
                }
                literals.clear();

                while (!queue.isEmpty()) {
                    Vertex curVertex = queue.removeFirst();

                    final int varX = model[Math.abs(curVertex.getVar()) - 1];
                    if (varX != 0) {
                        curVertex = modalImplicationGraph.getVertex(varX);
                        solver.getAssignment().add(-varX);
                        switch (solver.hasSolution()) {
                            case FALSE:
                                solver.getAssignment().replaceLast(varX);
                                fixed[Math.abs(varX)] = varX;
                                final SortedIntegerList sortedIntegerList = new SortedIntegerList(negVar, varX);
                                cleanedClausesList.add(sortedIntegerList);
                                modalImplicationGraph.getDetectedStrong().add(sortedIntegerList);
                                for (final Vertex strongVertex : curVertex.getStrongEdges()) {
                                    final int index = Math.abs(strongVertex.getVar());
                                    mark[index] = true;
                                    if (fixed[index] == 0) {
                                        solver.getAssignment().add(strongVertex.getVar());
                                        fixed[index] = strongVertex.getVar();
                                    }
                                    strongVertex.getComplexClauses().stream()
                                            .flatMapToInt(c -> IntStream.of(c.getIntegers()))
                                            .forEach(literals::add);
                                }
                                break;
                            case TIMEOUT:
                                solver.getAssignment().remove();
                                curVertex.getStrongEdges().stream()
                                        .map(Vertex::getVar)
                                        .forEach(literals::add);
                                break;
                            case TRUE:
                                solver.getAssignment().remove();
                                SortedIntegerList.resetConflicts(model, solver.getInternalSolution());
                                solver.shuffleOrder(random);
                                curVertex.getStrongEdges().stream()
                                        .map(Vertex::getVar)
                                        .forEach(literals::add);

                                //							Vertex complement = mig.getVertex(-curVertex.getVar());
                                //							for (final Vertex strongVertex : complement.getStrongEdges()) {
                                //								literals.add(strongVertex.getVar());
                                //							}
                                break;
                        }
                    } else {
                        curVertex.getStrongEdges().stream().map(Vertex::getVar).forEach(literals::add);

                        //						Vertex complement = mig.getVertex(-curVertex.getVar());
                        //						for (final Vertex strongVertex : complement.getStrongEdges()) {
                        //							literals.add(strongVertex.getVar());
                        //						}
                    }
                    curVertex.getComplexClauses().stream()
                            .flatMapToInt(c -> IntStream.of(c.getIntegers()))
                            .forEach(literals::add);

                    //					Vertex complement = mig.getVertex(-curVertex.getVar());
                    //					for (final LiteralList complexClause : complement.getComplexClauses()) {
                    //						for (int literal : complexClause.getLiterals()) {
                    //							literals.add(literal);
                    //						}
                    //					}

                    for (final Integer literal : literals) {
                        final int index = Math.abs(literal);
                        if (!mark[index]) {
                            mark[index] = true;
                            queue.add(modalImplicationGraph.getVertex(literal));
                        }
                    }
                    literals.clear();
                }
            }
            solver.getAssignment().clear(orgSize);
            monitor.addStep();
        }
        for (final Vertex vertex : modalImplicationGraph.getVertices()) {
            vertex.getStrongEdges().clear();
            vertex.getComplexClauses().clear();
        }
        monitor.setDone();
    }

    protected void finish() {
        for (final Vertex vertex : modalImplicationGraph.getVertices()) {
            vertex.finish();
        }
        modalImplicationGraph.getDetectedStrong().trimToSize();
    }

    public boolean isCheckRedundancy() {
        return checkRedundancy;
    }

    public void setCheckRedundancy(boolean checkRedundancy) {
        this.checkRedundancy = checkRedundancy;
    }

    public boolean isDetectStrong() {
        return detectStrong;
    }

    public void setDetectStrong(boolean detectStrong) {
        this.detectStrong = detectStrong;
    }
}
