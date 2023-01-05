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

import de.featjar.formula.analysis.bool.BooleanSolutionList;
import de.featjar.base.task.Executor;
import de.featjar.base.log.Log;
import java.util.Collections;
import java.util.List;

/**
 * Finds certain solutions of propositional formulas.
 *
 * @author Sebastian Krieter
 */
public class EnumeratingRandomConfigurationGenerator extends RandomConfigurationGenerator {

    private List<SortedIntegerList> allConfigurations;

    @Override
    protected void init(IMonitor monitor) {
        final AllConfigurationGenerator gen = new AllConfigurationGenerator();
        allConfigurations = Executor.apply(gen::execute, solver.getCnf(), monitor)
                .map(BooleanSolutionList::getSolutions)
                .orElse(Collections::emptyList, Log::problem);
        if (!allowDuplicates) {
            Collections.shuffle(allConfigurations, random);
        }
    }

    @Override
    public SortedIntegerList get() {
        if (allConfigurations.isEmpty()) {
            return null;
        }
        if (allowDuplicates) {
            return allConfigurations.get(random.nextInt(allConfigurations.size()));
        } else {
            return allConfigurations.remove(allConfigurations.size());
        }
    }
}
