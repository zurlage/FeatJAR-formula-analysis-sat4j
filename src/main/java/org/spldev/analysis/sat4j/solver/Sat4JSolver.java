/* -----------------------------------------------------------------------------
 * Formula-Analysis-Sat4J Lib - Library to analyze propositional formulas with Sat4J.
 * Copyright (C) 2021  Sebastian Krieter
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
package org.spldev.analysis.sat4j.solver;

import java.util.*;

import org.sat4j.minisat.*;
import org.sat4j.minisat.core.*;
import org.sat4j.minisat.orders.*;
import org.spldev.analysis.sat4j.solver.SStrategy.*;
import org.spldev.analysis.sat4j.solver.strategy.*;
import org.spldev.clauses.*;
import org.spldev.formula.structure.atomic.literal.*;

/**
 * Sat solver using Sat4J.
 *
 * @author Sebastian Krieter
 */
public class Sat4JSolver extends AbstractSat4JSolver<Solver<?>> {

	protected final int[] order;
	protected SStrategy strategy;

	public Sat4JSolver(CNF cnf) {
		super(cnf);
		strategy = SStrategy.original();
		order = new int[cnf.getVariableMap().size()];
		setOrderFix();
	}

	public Sat4JSolver(VariableMap variableMap) {
		super(variableMap);
		strategy = SStrategy.original();
		order = new int[variableMap.size()];
		setOrderFix();
	}

	@Override
	protected Solver<?> createSolver() {
		return (Solver<?>) SolverFactory.newDefault();
	}

	@Override
	protected void initSolver(List<LiteralList> clauses) {
		super.initSolver(clauses);
		solver.getOrder().init();
	}

	public int[] getOrder() {
		return order;
	}

	public SStrategy getSelectionStrategy() {
		return strategy;
	}

	public void setOrder(int[] order) {
		assert order.length <= this.order.length;
		System.arraycopy(order, 0, this.order, 0, order.length);
	}

	public void setOrderFix() {
		for (int i = 0; i < order.length; i++) {
			order[i] = i + 1;
		}
	}

	public void shuffleOrder() {
		shuffleOrder(new Random());
	}

	public void shuffleOrder(Random rnd) {
		for (int i = order.length - 1; i >= 0; i--) {
			final int index = rnd.nextInt(i + 1);
			final int a = order[index];
			order[index] = order[i];
			order[i] = a;
		}
	}

	private void setSelectionStrategy(IOrder strategy) {
		solver.setOrder(strategy);
		solver.getOrder().init();
	}

	public void setSelectionStrategy(SStrategy strategy) {
		this.strategy = strategy;
		switch (strategy.strategy()) {
		case FastRandom:
			setSelectionStrategy(new FixedOrderHeap(new RandomSelectionStrategy(), order));
			break;
		case Fixed:
			setSelectionStrategy(new FixedOrderHeap( //
				new FixedLiteralSelectionStrategy(((FixedStrategy) strategy).getModel()), //
				order));
			break;
		case InverseFixed:
			setSelectionStrategy(new FixedOrderHeap( //
				new FixedLiteralSelectionStrategy(((InverseFixedStrategy) strategy).getModel()), //
				order));
			break;
		case MIGRandom:
			setSelectionStrategy(new FixedOrderHeap2(new UniformRandomSelectionStrategy(
				((MIGRandomStrategy) strategy).getDist()), order));
			break;
		case Negative:
			setSelectionStrategy(new FixedOrderHeap(new NegativeLiteralSelectionStrategy(), order));
			break;
		case Original:
			setSelectionStrategy(new VarOrderHeap(new RSATPhaseSelectionStrategy()));
			break;
		case Positive:
			setSelectionStrategy(new FixedOrderHeap(new PositiveLiteralSelectionStrategy(), order));
			break;
		case UniformRandom:
			setSelectionStrategy(new FixedOrderHeap2(new UniformRandomSelectionStrategy(
				((UniformRandomStrategy) strategy).getDist()), order));
			break;
		default:
			throw new IllegalStateException(String.valueOf(strategy.strategy()));
		}
	}

}
