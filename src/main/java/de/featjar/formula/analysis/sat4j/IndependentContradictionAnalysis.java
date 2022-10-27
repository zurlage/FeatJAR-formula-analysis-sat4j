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

import de.featjar.base.data.Computation;
import de.featjar.base.data.FutureResult;
import de.featjar.base.data.Result;
import de.featjar.formula.analysis.solver.SolverContradictionException;
import de.featjar.formula.assignment.VariableAssignment;
import de.featjar.formula.clauses.CNF;
import de.featjar.formula.clauses.LiteralList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Finds contradicting clauses with respect to a given {@link CNF}. This
 * analysis works by adding and removing each clause group (see
 * {@link ClauseAnalysis}) to the given {@link CNF} individually. All clause
 * groups are analyzed separately without considering their
 * interdependencies.<br>
 * For a dependent analysis of all clause groups use
 * {@link ContradictionAnalysis}.
 *
 * @author Sebastian Krieter
 *
 * @see ContradictionAnalysis
 */
public class IndependentContradictionAnalysis extends ClauseAnalysis<List<LiteralList>> {

    protected IndependentContradictionAnalysis(Computation<CNF> inputComputation, List<LiteralList> clauseList) {
        super(inputComputation, clauseList);
    }

    protected IndependentContradictionAnalysis(Computation<CNF> inputComputation, List<LiteralList> clauseList, VariableAssignment assumptions, long timeoutInMs, long randomSeed) {
        super(inputComputation, clauseList, assumptions, timeoutInMs, randomSeed);
    }

    @Override
    public FutureResult<List<LiteralList>> compute() {
        return initializeSolver().thenCompute((solver, monitor) -> {
            if (clauseList == null) {
                clauseList = solver.getCNF().getClauses();
            }
            if (clauseGroupSize == null) {
                clauseGroupSize = new int[clauseList.size()];
                Arrays.fill(clauseGroupSize, 1);
            }
            monitor.setTotalSteps(clauseList.size() + 1);

            final List<LiteralList> resultList = new ArrayList<>(clauseGroupSize.length);
            for (int i = 0; i < clauseList.size(); i++) {
                resultList.add(null);
            }
            monitor.addStep();

            int endIndex = 0;
            for (int i = 0; i < clauseGroupSize.length; i++) {
                final int startIndex = endIndex;
                endIndex += clauseGroupSize[i];
                final List<LiteralList> subList = clauseList.subList(startIndex, endIndex);

                try {
                    solver.getSolverFormula().push(subList);
                } catch (final SolverContradictionException e) {
                    resultList.set(i, clauseList.get(startIndex));
                    monitor.addStep();
                    continue;
                }

                final Result<Boolean> hasSolution = solver.hasSolution();
                if (Result.of(false).equals(hasSolution)) {
                    resultList.set(i, clauseList.get(startIndex));
                } else if (Result.empty().equals(hasSolution)) {
                    //reportTimeout();
                } else if (Result.of(true).equals(hasSolution)) {
                } else {
                    throw new AssertionError(hasSolution);
                }

                solver.getSolverFormula().pop(subList.size());
                monitor.addStep();
            }

            return resultList;
        });
    }
}
