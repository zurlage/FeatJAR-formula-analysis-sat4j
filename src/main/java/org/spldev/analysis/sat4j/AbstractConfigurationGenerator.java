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
import java.util.function.*;
import java.util.stream.*;

import org.spldev.analysis.sat4j.solver.*;
import org.spldev.clauses.*;
import org.spldev.clauses.solutions.*;
import org.spldev.formula.*;
import org.spldev.util.job.*;

/**
 * Finds certain solutions of propositional formulas.
 *
 * @author Sebastian Krieter
 */
public abstract class AbstractConfigurationGenerator extends Sat4JAnalysis<SolutionList> implements
	ConfigurationGenerator {

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
	public boolean tryAdvance(Consumer<? super LiteralList> consumer) {
		final LiteralList literalList = get();
		if (literalList != null) {
			consumer.accept(literalList);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public Spliterator<LiteralList> trySplit() {
		return null;
	}

	@Override
	public void init(ModelRepresentation rep, InternalMonitor monitor) {
		solver = createSolver(rep.get(solverInputProvider));
		monitor.checkCancel();
		prepareSolver(solver);
		init(monitor);
	}

	protected void init(InternalMonitor monitor) {
	}

	@Override
	public final SolutionList analyze(Sat4JSolver solver, InternalMonitor monitor) throws Exception {
		init(monitor);
		monitor.setTotalWork(maxSampleSize);
		return new SolutionList(solver.getVariables(), StreamSupport.stream(this, false) //
			.limit(maxSampleSize) //
			.peek(c -> monitor.step()) //
			.collect(Collectors.toCollection(ArrayList::new)));
	}

}
