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
package de.featjar.formula.analysis.sat4j.todo;

import de.featjar.base.Feat;
import de.featjar.base.data.Computation;
import de.featjar.base.data.FutureResult;

import java.util.*;

import static de.featjar.base.data.Computations.async;

/**
 * Finds clauses responsible for core and dead features.
 *
 * @author Sebastian Krieter
 */
public class CauseAnalysis extends ClauseAnalysis<List<CauseAnalysis.Anomalies>> {
    public CauseAnalysis(Computation<CNF> inputComputation, List<SortedIntegerList> literalListIndexList) {
        super(inputComputation, literalListIndexList);
    }

    public CauseAnalysis(Computation<CNF> inputComputation, List<SortedIntegerList> literalListIndexList, Assignment assumptions, long timeoutInMs, long randomSeed) {
        super(inputComputation, literalListIndexList, assumptions, timeoutInMs, randomSeed);
    }

    public static class Anomalies {

        protected SortedIntegerList deadVariables = new SortedIntegerList();
        protected List<SortedIntegerList> redundantSortedIntegerLists = Collections.emptyList();

        public SortedIntegerList getDeadVariables() {
            return deadVariables;
        }

        public void setDeadVariables(SortedIntegerList variables) {
            if (variables == null) {
                deadVariables = new SortedIntegerList();
            } else {
                deadVariables = variables;
            }
        }

        public List<SortedIntegerList> getRedundantClauses() {
            return redundantSortedIntegerLists;
        }

        public void setRedundantClauses(List<SortedIntegerList> redundantSortedIntegerLists) {
            if (redundantSortedIntegerLists == null) {
                this.redundantSortedIntegerLists = Collections.emptyList();
            } else {
                this.redundantSortedIntegerLists = redundantSortedIntegerLists;
            }
        }
    }

    private Anomalies anomalies;
    protected boolean[] relevantConstraint;

    public Anomalies getAnomalies() {
        return anomalies;
    }

    public void setAnomalies(Anomalies anomalies) {
        this.anomalies = anomalies;
    }

    public boolean[] getRelevantConstraint() {
        return relevantConstraint;
    }

    public void setRelevantConstraint(boolean[] relevantConstraint) {
        this.relevantConstraint = relevantConstraint;
    }

    @Override
    public FutureResult<List<Anomalies>> compute() {
        return initializeSolver().thenCompute((solver, monitor) -> {
            if (literalListIndexList == null) {
                return Collections.emptyList();
            }
            if (clauseGroupSize == null) {
                clauseGroupSize = new int[literalListIndexList.size()];
                Arrays.fill(clauseGroupSize, 1);
            }
            final List<Anomalies> resultList = new ArrayList<>(clauseGroupSize.length);
            for (int i = 0; i < literalListIndexList.size(); i++) {
                resultList.add(null);
            }
            if (anomalies == null) {
                return resultList;
            }
            monitor.setTotalSteps(literalListIndexList.size() + 3);

            SortedIntegerList remainingVariables = anomalies.deadVariables.getAbsoluteValuesOfIntegers();
            final List<SortedIntegerList> remainingSortedIntegerLists = new ArrayList<>(anomalies.redundantSortedIntegerLists);
            monitor.addStep();

            if (!remainingSortedIntegerLists.isEmpty()) {
                final List<SortedIntegerList> result =
                        async(solver.getCNF())
                                .map(IndependentRedundancyAnalysis.class, remainingSortedIntegerLists).getResult()
                        .orElse(p -> Feat.log().problems(p));
                remainingSortedIntegerLists.removeIf(result::contains);
            }
            monitor.addStep();

            if (remainingVariables.getIntegers().length > 0) {
                remainingVariables = remainingVariables.removeAll(
                        async(solver.getCNF()).map(CoreDeadAnalysis.class, remainingVariables).getResult()
                                .orElse(p -> Feat.log().problems(p)));
            }
            monitor.addStep();

            int endIndex = 0;
            for (int i = 0; i < clauseGroupSize.length; i++) {
                if ((remainingVariables.getIntegers().length == 0) && remainingSortedIntegerLists.isEmpty()) {
                    break;
                }

                final int startIndex = endIndex;
                endIndex += clauseGroupSize[i];
                solver.getSolverFormula().push(literalListIndexList.subList(startIndex, endIndex));
                if (relevantConstraint[i]) {
                    if (remainingVariables.getIntegers().length > 0) {
                        final SortedIntegerList deadVariables =
                                async(solver.getCNF()).map(CoreDeadAnalysis.class, remainingVariables).getResult().get();
                        if (deadVariables.getIntegers().length != 0) {
                            getAnomalies(resultList, i).setDeadVariables(deadVariables);
                            remainingVariables = remainingVariables.removeAll(deadVariables);
                        }
                    }

                    if (!remainingSortedIntegerLists.isEmpty()) {
                        final List<SortedIntegerList> newLiteralListIndexList =
                                async(solver.getCNF()).map(IndependentRedundancyAnalysis.class, remainingSortedIntegerLists).getResult().get();
                        newLiteralListIndexList.removeIf(Objects::isNull);
                        if (!newLiteralListIndexList.isEmpty()) {
                            getAnomalies(resultList, i).setRedundantClauses(newLiteralListIndexList);
                            remainingSortedIntegerLists.removeAll(newLiteralListIndexList);
                        }
                    }
                }

                monitor.addStep();
            }

            return resultList;
        });
    }

    protected Anomalies getAnomalies(final List<Anomalies> resultList, final Integer curIndex) {
        Anomalies curAnomalies = resultList.get(curIndex);
        if (curAnomalies == null) {
            curAnomalies = new Anomalies();
            resultList.set(curIndex, curAnomalies);
        }
        return curAnomalies;
    }
}
