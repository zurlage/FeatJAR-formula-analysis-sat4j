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

import de.featjar.base.data.Result;
import de.featjar.formula.analysis.bool.BooleanClause;
import de.featjar.formula.analysis.bool.BooleanSolution;
import org.sat4j.specs.TimeoutException;

public abstract class ModelComparator {

    public static boolean eq(CNF cnf1, final CNF cnf2) throws TimeoutException {
        return compare(cnf2, cnf1) && compare(cnf1, cnf2);
    }

    public static boolean compare(CNF cnf1, final CNF cnf2) throws TimeoutException {
        final Sat4JSolutionSolver solver = new Sat4JSolutionSolver(cnf1);
        for (final BooleanClause sortedIntegerList : cnf2.getClauseList().getAll()) {
            final Result<Boolean> hasSolution = solver.hasSolution(new BooleanSolution(sortedIntegerList.negate().getIntegers())); // TODO: add .toSolution and friends
            if (hasSolution.isEmpty())
                throw new TimeoutException();
            if (hasSolution.equals(Result.of(true)))
                return false;
            else
                break;
        }
        return true;
    }
}
