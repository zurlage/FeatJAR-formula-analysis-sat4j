/*
 * Copyright (C) 2023 FeatJAR-Development-Team
 *
 * This file is part of FeatJAR-formula-analysis-sat4j.
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

import de.featjar.base.computation.Computations;
import de.featjar.base.data.IIntegerList;
import de.featjar.base.data.Result;
import de.featjar.formula.analysis.bool.ABooleanAssignment;
import de.featjar.formula.analysis.bool.BooleanClause;
import de.featjar.formula.analysis.bool.BooleanClauseList;
import de.featjar.formula.analysis.bool.BooleanSolution;
import de.featjar.formula.analysis.combinations.ConfigurationUpdater;
import java.util.Collection;

public class RandomConfigurationUpdater implements ConfigurationUpdater {
    private final BooleanClauseList model;
    private final Long randomSeed;

    public RandomConfigurationUpdater(BooleanClauseList cnf, Long randomSeed) {
        this.model = cnf;
        this.randomSeed = randomSeed;
    }

    @Override
    public Result<BooleanSolution> update(ABooleanAssignment partialSolution) {
        return Computations.of(model)
                .map(ComputeCoreDeadVariablesSAT4J::new)
                .set(ComputeCoreDeadVariablesSAT4J.ASSUMED_ASSIGNMENT, partialSolution)
                .computeResult()
                .map(a -> a.toSolution());
    }

    @Override
    public Result<BooleanSolution> complete(
            Collection<int[]> include, Collection<int[]> exclude, Collection<int[]> choose) {
        final int orgVariableCount = model.getVariableCount();

        BooleanClauseList ll = new BooleanClauseList(orgVariableCount);

        int newVariableCount = orgVariableCount;
        if (choose != null) {
            int[] newNegativeLiterals = new int[choose.size()];
            int i = 0;
            for (int[] clause : choose) {
                int newVar = orgVariableCount + i + 1;
                newNegativeLiterals[i++] = -newVar;
                for (int l : clause) {
                    ll.add(new BooleanClause(new int[] {l, newVar}));
                }
            }

            ll.add(new BooleanClause(newNegativeLiterals));
            ll.add(new BooleanClause(IIntegerList.mergeInt(choose)).inverse());
            newVariableCount += i;
        }
        if (include != null) {
            int[] includeMerge = IIntegerList.mergeInt(include);
            for (int literal : includeMerge) {
                ll.add(new BooleanClause(literal));
            }
        }
        if (exclude != null) {
            for (int[] clause : exclude) {
                ll.add(new BooleanClause(clause).inverse());
            }
        }
        return Computations.of(new BooleanClauseList(model.getAll(), newVariableCount))
                .map(ComputeSolutionSAT4J::new)
                .set(ComputeSolutionSAT4J.ASSUMED_CLAUSE_LIST, ll)
                .set(ComputeSolutionSAT4J.RANDOM_SEED, randomSeed)
                .computeResult()
                .map(a -> a.toSolution());
    }
}
