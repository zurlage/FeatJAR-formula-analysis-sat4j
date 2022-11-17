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
package de.featjar.formula.analysis.sat4j.solver;

import de.featjar.base.data.Pair;
import de.featjar.base.data.Result;
import de.featjar.formula.analysis.sat.clause.CNF;
import de.featjar.formula.analysis.sat.clause.ClauseList;
import de.featjar.formula.analysis.sat.solution.Solution;
import de.featjar.formula.analysis.solver.AssumptionList;
import de.featjar.formula.analysis.solver.SolutionSolver;
import org.sat4j.core.VecInt;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.TimeoutException;

import java.util.*;

/**
 * Base class for solvers using Sat4J.
 *
 * @author Sebastian Krieter
 */
public abstract class Sat4JSolver<T extends ISolver> implements SolutionSolver<Solution> {
    public static final int MAX_SOLUTION_BUFFER = 1000;
    protected CNF cnf;
    protected final T solver;
    protected ClauseList assumptionList = new ClauseList();
    protected final Sat4JFormula formula;

    // TODO extract solution history in separate class
    protected LinkedList<Solution> solutionHistory = null;
    protected int solutionHistoryLimit = -1;
    protected int[] lastModel = null; // todo: (last) Solution?
    protected boolean globalTimeout = false;
    private boolean contradiction = false;
    private long timeout;

//    public Sat4JSolver() {
//        cnf = null;
//        solver = createSolver();
//        configureSolver();
//        formula = new Sat4JFormula(this);
//        initSolver(Collections.emptyList());
//    }

    public Sat4JSolver(CNF cnf) {
        this.cnf = cnf;
        solver = createSolver();
        configureSolver();
        formula = new Sat4JFormula(this);
        initSolver(cnf.getClauseList());
    }

    /**
     * @return The {@link CNF sat instance} given to the solver.
     */
    public CNF getCNF() {
        return cnf;
    }

    @Override
    public ClauseList getAssumptionList() {
        return assumptionList;
    }

    @Override
    public void setAssumptionList(AssumptionList<?> clauseList) { //throws SolverContradictionException  {
        this.assumptionList = (ClauseList) clauseList;
    }

    @Override
    public Sat4JFormula getSolverFormula() {
        return formula;
    }

    /**
     * Returns a copy of the last solution found by satisfiability solver. Can only
     * be called after a successful call of {@link #hasSolution()} or
     * {@link #hasSolution(int...)}.
     *
     * @return A {@link SortedIntegerList} representing the satisfying assignment.
     *
     * @see #hasSolution()
     * @see #hasSolution(int...)
     */
    @Override
    public Result<Solution> getSolution() {
        return lastModel == null ? Result.empty() : Result.of(new Solution(getLastModelCopy(), false));
    }

    public int[] getInternalSolution() {
        return lastModel;
    }

    private int[] getLastModelCopy() {
        return Arrays.copyOf(lastModel, lastModel.length);
    }

    /**
     * Checks whether there is a satisfying solution considering the clauses of the
     * solver and the given variable assignment.
     *
     * @param assignment The temporarily variable assignment for this call.
     * @return A {@link Result<Boolean>}.
     *
     * @see #hasSolution()
     * @see #hasSolution(int...)
     * @see #getInternalSolution()
     */
    public Result<Boolean> hasSolution(Solution assignment) {
        return hasSolution(assignment.getIntegers());
    }

    /**
     * Completely resets the solver, removing all its assignments, variables, and
     * clauses.
     */
    @Override
    public void reset() {
        solver.reset();
        if (solutionHistory != null) {
            solutionHistory.clear();
            lastModel = null;
        }
    }

    /**
     * Creates the Sat4J solver instance.
     *
     * @return Sat4J solver
     */
    protected abstract T createSolver();

    /**
     * Add clauses to the solver. Initializes the order instance.
     */
    protected void initSolver(ClauseList sortedIntegerLists) {
        final int size = cnf.getVariableMap().getVariableCount(); // todo: before, this was formula.getvariablemap - may cause an issue with pushing to the assumption stack?
        //		final List<LiteralList> clauses = satInstance.getClauses();
        try {
            if (!sortedIntegerLists.isEmpty()) {
                solver.setExpectedNumberOfClauses(sortedIntegerLists.size() + 1);
                formula.push(sortedIntegerLists);
            }
            if (size > 0) {
                final VecInt pseudoClause = new VecInt(size + 1);
                for (int i = 1; i <= size; i++) {
                    pseudoClause.push(i);
                }
                pseudoClause.push(-1);
                solver.addClause(pseudoClause);
            }
        } catch (final Exception e) {
            contradiction = true;
        }
    }

    @Override
    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
        if (timeout > 0)
            solver.setTimeoutMs(timeout);
        else
            ;// todo: explicitly unset timeout?
    }

    @Override
    public Result<Solution> findSolution() {
        return hasSolution().equals(Result.of(true)) ? getSolution() : null;
    }

    public List<Solution> getSolutionHistory() {
        return solutionHistory != null ? solutionHistory : Collections.emptyList();
    }

    private int[] getAssumptionArray() {
        final List<Pair<Integer, Object>> all = new ArrayList<>(); // TODO: currently all assumptions are ignored, was assumptionList.getAll(); before
        final int[] literals = new int[all.size()];
        int index = 0;
        for (final Pair<Integer, Object> entry : all) {
            final int variable = entry.getKey();
            literals[index++] = (entry.getValue() == Boolean.TRUE) ? variable : -variable;
        }
        return literals;
    }

    /**
     * Checks whether there is a satisfying solution considering the clauses of the
     * solver.
     *
     * @return A {@link Result<Boolean>}.
     *
     * @see #hasSolution(SortedIntegerList)
     * @see #hasSolution(int...)
     * @see #getInternalSolution()
     */
    @Override
    public Result<Boolean> hasSolution() {
        if (contradiction) {
            lastModel = null;
            return Result.of(false);
        }

        final int[] assumptionArray = getAssumptionArray();
        if (solutionHistory != null) {
            for (final Solution solution : solutionHistory) {
                if (solution.containsAll(assumptionArray)) {
                    lastModel = solution.getIntegers();
                    return Result.of(true);
                }
            }
        }

        try {
            if (solver.isSatisfiable(new VecInt(assumptionArray), globalTimeout)) {
                lastModel = solver.model();
                addSolution();
                return Result.of(true);
            } else {
                lastModel = null;
                return Result.of(false);
            }
        } catch (final TimeoutException e) {
            lastModel = null;
            return Result.empty();
        }
    }

    /**
     * Checks whether there is a satisfying solution considering the clauses of the
     * solver and the given variable assignment.<br>
     * Does only consider the given {@code assignment} and <b>not</b> the global
     * assignment variable of the solver.
     *
     * @param assignment The temporarily variable assignment for this call.
     * @return A {@link Result<Boolean>}.
     *
     * @see #hasSolution(SortedIntegerList)
     * @see #hasSolution()
     * @see #getInternalSolution()
     */
    public Result<Boolean> hasSolution(int... assignment) {
        if (contradiction) {
            return Result.of(false);
        }

        if (solutionHistory != null) {
            for (final Solution solution : solutionHistory) {
                if (solution.containsAll(assignment)) {
                    lastModel = solution.getIntegers();
                    return Result.of(true);
                }
            }
        }

        final int[] unitClauses = new int[assignment.length];
        System.arraycopy(assignment, 0, unitClauses, 0, unitClauses.length);

        try {
            // TODO why is this necessary?
            if (solver.isSatisfiable(new VecInt(unitClauses), globalTimeout)) {
                lastModel = solver.model();
                addSolution();
                return Result.of(true);
            } else {
                lastModel = null;
                return Result.of(false);
            }
        } catch (final TimeoutException e) {
            lastModel = null;
            return Result.empty();
        }
    }

    private void addSolution() {
        if (solutionHistory != null) {
            solutionHistory.addFirst(getSolution().get());
            if (solutionHistory.size() > solutionHistoryLimit) {
                solutionHistory.removeLast();
            }
        }
    }

    public int[] getContradictoryAssignment() {
        final IVecInt unsatExplanation = solver.unsatExplanation();
        return Arrays.copyOf(unsatExplanation.toArray(), unsatExplanation.size());
    }

    public List<Solution> rememberSolutionHistory(int numberOfSolutions) {
        if (numberOfSolutions > 0) {
            solutionHistory = new LinkedList<>();
            solutionHistoryLimit = numberOfSolutions;
        } else {
            solutionHistory = null;
            solutionHistoryLimit = -1;
        }
        return getSolutionHistory();
    }

    public boolean isGlobalTimeout() {
        return globalTimeout;
    }

    public void setGlobalTimeout(boolean globalTimeout) {
        this.globalTimeout = globalTimeout;
    }

    protected void configureSolver() {
        solver.setTimeoutMs(1_000_000);
        solver.setDBSimplificationAllowed(true);
        solver.setKeepSolverHot(true);
        solver.setVerbose(false);
    }
}
