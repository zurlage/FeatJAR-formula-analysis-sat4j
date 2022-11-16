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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.sat4j.specs.IConstr;

/**
 * Finds redundant clauses with respect to a given {@link CNF}. This analysis
 * works by adding every clause group (see {@link ClauseAnalysis}) to the given
 * {@link CNF} at the beginning an then removing and re-adding each clause group
 * individually. If a clause group is redundant with respect to the current
 * formula, it is marked as redundant and removed completely from the
 * {@link CNF}, otherwise it is kept as part of the {@link CNF} for the
 * remaining analysis. Clauses are added in the same order a they appear in the
 * given clauses list.<br>
 * For an independent analysis of every clause group use
 * {@link IndependentRedundancyAnalysis}.
 *
 * @author Sebastian Krieter
 *
 * @see AddRedundancyAnalysis
 * @see IndependentRedundancyAnalysis
 */
public class RemoveRedundancyAnalysis extends ClauseAnalysis<List<SortedIntegerList>> {
    public RemoveRedundancyAnalysis(Computation<CNF> inputComputation, List<SortedIntegerList> literalListIndexList) {
        super(inputComputation, literalListIndexList);
    }

    public RemoveRedundancyAnalysis(Computation<CNF> inputComputation, List<SortedIntegerList> literalListIndexList, Assignment assumptions, long timeoutInMs, long randomSeed) {
        super(inputComputation, literalListIndexList, assumptions, timeoutInMs, randomSeed);
    }

    @Override
    public FutureResult<List<SortedIntegerList>> compute() {
        return initializeSolver().thenCompute(((solver, monitor) -> {
            if (literalListIndexList == null) {
                return Collections.emptyList();
            }
            if (clauseGroupSize == null) {
                clauseGroupSize = new int[literalListIndexList.size()];
                Arrays.fill(clauseGroupSize, 1);
            }
            monitor.setTotalSteps(clauseGroupSize.length + 1);

            final List<SortedIntegerList> resultList = new ArrayList<>(clauseGroupSize.length);
            for (int i = 0; i < literalListIndexList.size(); i++) {
                resultList.add(null);
            }

            final List<IConstr> constrs = new ArrayList<>(literalListIndexList.size());
            for (final SortedIntegerList sortedIntegerList : literalListIndexList) {
                constrs.add(solver.getSolverFormula().push(sortedIntegerList));
            }

            monitor.addStep();

            int endIndex = 0;
            for (int i = 0; i < clauseGroupSize.length; i++) {
                final int startIndex = endIndex;
                endIndex += clauseGroupSize[i];
                boolean completelyRedundant = true;
                boolean removedAtLeastOne = false;
                for (int j = startIndex; j < endIndex; j++) {
                    final IConstr cm = constrs.get(j);
                    if (cm != null) {
                        removedAtLeastOne = true;
                        solver.getSolverFormula().remove(cm);
                    }
                }

                if (removedAtLeastOne) {
                    for (int j = startIndex; j < endIndex; j++) {
                        final SortedIntegerList sortedIntegerList = literalListIndexList.get(j);

                        final Result<Boolean> hasSolution = solver.hasSolution(sortedIntegerList.negate());
                        if (Result.of(false).equals(hasSolution)) {
                        } else if (Result.empty().equals(hasSolution)) {
                            //reportTimeout();
                        } else if (Result.of(true).equals(hasSolution)) {
                            solver.getSolverFormula().push(sortedIntegerList);
                            completelyRedundant = false;
                        } else {
                            throw new AssertionError(hasSolution);
                        }
                    }
                }

                if (completelyRedundant) {
                    resultList.set(i, literalListIndexList.get(startIndex));
                }
                monitor.addStep();
            }

            return resultList;
        }));
    }
}
