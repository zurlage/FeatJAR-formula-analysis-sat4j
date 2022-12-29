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

import de.featjar.formula.analysis.sat4j.solver.ISelectionStrategy;
import de.featjar.formula.analysis.sat4j.solver.SampleDistribution;
import de.featjar.formula.analysis.sat4j.solver.SAT4JSolutionSolver;
import de.featjar.formula.analysis.bool.BooleanSolutionList;
import de.featjar.base.task.Executor;
import de.featjar.base.task.IMonitor;
import de.featjar.base.log.Log;
import java.util.List;

/**
 * Finds certain solutions of propositional formulas.
 *
 * @author Sebastian Krieter
 */
public class SampleRandomConfigurationGenerator extends RandomConfigurationGenerator {

    private int sampleSize = 100;
    private List<SortedIntegerList> sample;
    private SampleDistribution dist;

    @Override
    protected void init(IMonitor monitor) {
        satisfiable = findCoreFeatures(solver);
        if (!satisfiable) {
            return;
        }

        final RandomConfigurationGenerator gen = new FastRandomConfigurationGenerator();
        gen.setAllowDuplicates(false);
        gen.setRandom(random);
        gen.setLimit(sampleSize);
        sample = Executor.apply(gen::execute, solver.getCnf())
                .map(BooleanSolutionList::getSolutions)
                .orElse(Log::problem);
        if ((sample == null) || sample.isEmpty()) {
            satisfiable = false;
            return;
        }

        dist = new SampleDistribution(sample);
        dist.setRandom(random);
        solver.setSelectionStrategy(ISelectionStrategy.uniform(dist));
    }

    @Override
    protected void reset() {
        dist.reset();
    }

    private boolean findCoreFeatures(SAT4JSolutionSolver solver) {
        final int[] fixedFeatures = solver.findSolution().getLiterals();
        if (fixedFeatures == null) {
            return false;
        }
        solver.setSelectionStrategy(ISelectionStrategy.inverse(fixedFeatures));

        // find core/dead features
        for (int i = 0; i < fixedFeatures.length; i++) {
            final int varX = fixedFeatures[i];
            if (varX != 0) {
                solver.getAssignment().add(-varX);
                final SATSolver.Result<Boolean> hasSolution = solver.hasSolution();
                switch (hasSolution) {
                    case FALSE:
                        solver.getAssignment().replaceLast(varX);
                        break;
                    case TIMEOUT:
                        solver.getAssignment().remove();
                        break;
                    case TRUE:
                        solver.getAssignment().remove();
                        SortedIntegerList.resetConflicts(fixedFeatures, solver.getInternalSolution());
                        solver.shuffleOrder(random);
                        break;
                }
            }
        }
        return true;
    }

    public int getSampleSize() {
        return sampleSize;
    }

    public void setSampleSize(int sampleSize) {
        this.sampleSize = sampleSize;
    }
}
