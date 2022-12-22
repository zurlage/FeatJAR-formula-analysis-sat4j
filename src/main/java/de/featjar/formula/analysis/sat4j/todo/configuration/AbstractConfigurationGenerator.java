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
package de.featjar.formula.analysis.sat4j.todo.configuration;

import de.featjar.formula.analysis.sat4j.solver.SAT4JSolutionSolver;
import de.featjar.formula.analysis.bool.BooleanSolutionList;
import de.featjar.base.computation.Cache;
import de.featjar.base.task.IMonitor;
import java.util.ArrayList;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Finds certain solutions of propositional formulas.
 *
 * @author Sebastian Krieter
 */
public abstract class AbstractConfigurationGenerator
        implements ConfigurationGenerator {

    private int maxSampleSize = Integer.MAX_VALUE;

    protected boolean allowDuplicates = false;

    @Override
    public int getLimit() {
        return maxSampleSize;
    }

    @Override
    public void setLimit(int limit) {
        maxSampleSize = limit;
    }

    @Override
    public boolean isAllowDuplicates() {
        return allowDuplicates;
    }

    @Override
    public void setAllowDuplicates(boolean allowDuplicates) {
        this.allowDuplicates = allowDuplicates;
    }

    @Override
    public int characteristics() {
        return NONNULL | IMMUTABLE;
    }

    @Override
    public long estimateSize() {
        return Long.MAX_VALUE;
    }

    @Override
    public boolean tryAdvance(Consumer<? super SortedIntegerList> consumer) {
        final SortedIntegerList sortedIntegerList = get();
        if (sortedIntegerList != null) {
            consumer.accept(sortedIntegerList);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Spliterator<SortedIntegerList> trySplit() {
        return null;
    }

    @Override
    public void init(Cache c, IMonitor monitor) {
        solver = createSolver(c.get(solverInputComputation).get());
        monitor.checkCancel();
        prepareSolver(solver);
        init(monitor);
    }

    protected void init(IMonitor monitor) {}

    @Override
    public final BooleanSolutionList analyze(SAT4JSolutionSolver solver, IMonitor monitor) throws Exception {
        init(monitor);
        monitor.setTotalSteps(maxSampleSize);
        return new BooleanSolutionList(
                solver.getVariableMap(),
                StreamSupport.stream(this, false) //
                        .limit(maxSampleSize) //
                        .peek(c -> monitor.addStep()) //
                        .collect(Collectors.toCollection(ArrayList::new)));
    }
}
