/*
 * Copyright (C) 2025 FeatJAR-Development-Team
 *
 * This file is part of FeatJAR-FeatJAR-formula-analysis-sat4j.
 *
 * FeatJAR-formula-analysis-sat4j is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3.0 of the License,
 * or (at your option) any later version.
 *
 * FeatJAR-formula-analysis-sat4j is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FeatJAR-formula-analysis-sat4j. If not, see <https://www.gnu.org/licenses/>.
 *
 * See <https://github.com/FeatureIDE/FeatJAR-formula-analysis-sat4j> for further information.
 */
package de.featjar.analysis.sat4j.solver;

import de.featjar.analysis.RuntimeContradictionException;

public interface IMIGVisitor {

    int[] getAddedLiterals();

    int getAddedLiteralCount();

    void propagate(int... literals) throws RuntimeContradictionException;

    void setLiterals(int... literals) throws RuntimeContradictionException;

    boolean isContradiction(int... literals);

    void reset();

    void reset(int keep);

    boolean isUndefined(int literal);

    int countUndefined(int[] literals);
}
