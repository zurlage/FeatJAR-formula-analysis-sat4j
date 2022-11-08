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
import de.featjar.formula.analysis.Assignment;
import de.featjar.formula.analysis.sat.clause.CNF;

import java.util.List;

/**
 * Base class for an analysis that works on a list of clauses. Clauses can be
 * grouped together, for instance if they belong to the same constraint. Grouped
 * clauses should be handled as a unit by the implementing analysis.
 *
 * @param <T> the type of the analysis result.
 *
 * @author Sebastian Krieter
 */
public abstract class ClauseAnalysis<T> extends Sat4JAnalysis<T> {

    protected List<SortedIntegerList> literalListIndexList;
    protected int[] clauseGroupSize;

    protected ClauseAnalysis(Computation<CNF> inputComputation, List<SortedIntegerList> literalListIndexList) {
        super(inputComputation);
        this.literalListIndexList = literalListIndexList;
    }

    protected ClauseAnalysis(Computation<CNF> inputComputation, List<SortedIntegerList> literalListIndexList, Assignment assumptions, long timeoutInMs, long randomSeed) {
        super(inputComputation, assumptions, timeoutInMs, randomSeed);
        this.literalListIndexList = literalListIndexList;
    }

    public List<SortedIntegerList> getClauseList() {
        return literalListIndexList;
    }

    public void setClauseList(List<SortedIntegerList> literalListIndexList) {
        this.literalListIndexList = literalListIndexList;
    }

    public int[] getClauseGroups() {
        return clauseGroupSize;
    }

    public void setClauseGroupSize(int[] clauseGroups) {
        this.clauseGroupSize = clauseGroups;
    }
}
