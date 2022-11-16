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
import de.featjar.formula.analysis.sat4j.solver.Sat4JSolver;
import de.featjar.formula.analysis.sat4j.solver.SStrategy;
import de.featjar.formula.analysis.sat4j.solver.Sat4JSolutionSolver;
import de.featjar.formula.analysis.solver.SATSolver;
import de.featjar.formula.assignment.VariableAssignment;
import de.featjar.formula.clauses.CNF;
import de.featjar.formula.clauses.LiteralList;
import de.featjar.base.task.Monitor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Finds redundant clauses with respect to a given {@link CNF}. This analysis
 * works by adding and removing each clause group (see {@link ClauseAnalysis})
 * to the given {@link CNF} individually. All clause groups are analyzed
 * separately without considering their interdependencies.<br>
 * For a dependent analysis of all clause groups use
 * {@link RemoveRedundancyAnalysis}.
 *
 * @author Sebastian Krieter
 *
 * @see RemoveRedundancyAnalysis
 */
public class IndependentRedundancyAnalysis extends ClauseAnalysis<List<LiteralList>> {

    public IndependentRedundancyAnalysis(Computation<CNF> inputComputation, List<LiteralList> clauseList) {
        super(inputComputation, clauseList);
    }

    public IndependentRedundancyAnalysis(Computation<CNF> inputComputation, List<LiteralList> clauseList, VariableAssignment assumptions, long timeoutInMs, long randomSeed) {
        super(inputComputation, clauseList, assumptions, timeoutInMs, randomSeed);
    }

    @Override
    public FutureResult<List<LiteralList>> compute() {
        return initializeSolver().thenCompute((solver, monitor) -> {
            if (clauseList == null) {
                return Collections.emptyList();
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

            final List<LiteralList> solutionList = solver.rememberSolutionHistory(Sat4JSolver.MAX_SOLUTION_BUFFER);

            if (solver.hasSolution().equals(Result.of(true))) {
                solver.setSelectionStrategy(SStrategy.random(random));

                int endIndex = 0;
                groupLoop:
                for (int i = 0; i < clauseGroupSize.length; i++) {
                    final int startIndex = endIndex;
                    endIndex += clauseGroupSize[i];
                    clauseLoop:
                    for (int j = startIndex; j < endIndex; j++) {
                        final LiteralList clause = clauseList.get(j);
                        final LiteralList complement = clause.negate();

                        for (final LiteralList solution : solutionList) {
                            if (solution.containsAll(complement)) {
                                continue clauseLoop;
                            }
                        }

                        final Result<Boolean> hasSolution = solver.hasSolution(complement);
                        if (hasSolution.equals(Result.of(false))) {
                            resultList.set(i, clause);
                            continue groupLoop;
                        } else if (hasSolution.equals(Result.empty())) {
                            //reportTimeout();
                        } else if (hasSolution.equals(Result.of(true))) {
                            solver.shuffleOrder(random);
                        } else {
                            throw new AssertionError(hasSolution);
                        }
                    }
                }
            }

            return resultList;
        });
    }
}
