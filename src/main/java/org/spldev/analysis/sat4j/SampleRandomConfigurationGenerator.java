/* -----------------------------------------------------------------------------
 * Formula-Analysis-Sat4J Lib - Library to analyze propositional formulas with Sat4J.
 * Copyright (C) 2021-2022  Sebastian Krieter
 * 
 * This file is part of Formula-Analysis-Sat4J Lib.
 * 
 * Formula-Analysis-Sat4J Lib is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 * 
 * Formula-Analysis-Sat4J Lib is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Formula-Analysis-Sat4J Lib.  If not, see <https://www.gnu.org/licenses/>.
 * 
 * See <https://github.com/skrieter/formula-analysis-sat4j> for further information.
 * -----------------------------------------------------------------------------
 */
package org.spldev.analysis.sat4j;

import java.util.*;

import org.spldev.analysis.sat4j.solver.*;
import org.spldev.analysis.solver.SatSolver.*;
import org.spldev.clauses.*;
import org.spldev.clauses.solutions.*;
import org.spldev.util.data.*;
import org.spldev.util.job.*;
import org.spldev.util.logging.*;

/**
 * Finds certain solutions of propositional formulas.
 *
 * @author Sebastian Krieter
 */
public class SampleRandomConfigurationGenerator extends RandomConfigurationGenerator {

	public static final Identifier<SolutionList> identifier = new Identifier<>();

	@Override
	public Identifier<SolutionList> getIdentifier() {
		return identifier;
	}

	private int sampleSize = 100;
	private List<LiteralList> sample;
	private SampleDistribution dist;

	@Override
	protected void init(InternalMonitor monitor) {
		satisfiable = findCoreFeatures(solver);
		if (!satisfiable) {
			return;
		}

		final RandomConfigurationGenerator gen = new FastRandomConfigurationGenerator();
		gen.setAllowDuplicates(false);
		gen.setRandom(getRandom());
		gen.setLimit(sampleSize);
		sample = Executor.run(gen::execute, solver.getCnf()).map(
			SolutionList::getSolutions).orElse(Logger::logProblems);
		if ((sample == null) || sample.isEmpty()) {
			satisfiable = false;
			return;
		}

		dist = new SampleDistribution(sample);
		dist.setRandom(getRandom());
		solver.setSelectionStrategy(SStrategy.uniform(dist));
	}

	@Override
	protected void reset() {
		dist.reset();
	}

	private boolean findCoreFeatures(Sat4JSolver solver) {
		final int[] fixedFeatures = solver.findSolution().getLiterals();
		if (fixedFeatures == null) {
			return false;
		}
		solver.setSelectionStrategy(SStrategy.inverse(fixedFeatures));

		// find core/dead features
		for (int i = 0; i < fixedFeatures.length; i++) {
			final int varX = fixedFeatures[i];
			if (varX != 0) {
				solver.getAssumptions().push(-varX);
				final SatResult hasSolution = solver.hasSolution();
				switch (hasSolution) {
				case FALSE:
					solver.getAssumptions().replaceLast(varX);
					break;
				case TIMEOUT:
					solver.getAssumptions().pop();
					break;
				case TRUE:
					solver.getAssumptions().pop();
					LiteralList.resetConflicts(fixedFeatures, solver.getInternalSolution());
					solver.shuffleOrder(getRandom());
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
