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

import de.featjar.formula.clauses.LiteralList;
import de.featjar.formula.clauses.solutions.SolutionList;
import de.featjar.base.data.Store;
import de.featjar.base.task.Monitor;
import java.util.Spliterator;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Interface for configuration generators. Can be used as a {@link Supplier} or
 * to get a {@link Stream} or a {@link SolutionList} of configurations.
 *
 * @author Sebastian Krieter
 */
public interface ConfigurationGenerator
        extends Supplier<LiteralList>, Spliterator<LiteralList>, de.featjar.base.data.Computation {

    void init(Store rep, Monitor monitor);

    int getLimit();

    void setLimit(int limit);

    boolean isAllowDuplicates();

    void setAllowDuplicates(boolean allowDuplicates);
}
