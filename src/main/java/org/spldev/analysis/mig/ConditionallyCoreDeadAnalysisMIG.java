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
package org.spldev.analysis.mig;

import org.sat4j.core.*;
import org.spldev.analysis.mig.solver.*;
import org.spldev.analysis.mig.solver.visitor.*;
import org.spldev.analysis.sat4j.solver.*;
import org.spldev.clauses.*;
import org.spldev.util.data.*;
import org.spldev.util.job.*;

/**
 * Finds core and dead features using a {@link MIG model implication graph}.
 *
 * @author Sebastian Krieter
 */
public class ConditionallyCoreDeadAnalysisMIG extends Sat4JMIGAnalysis<LiteralList> {

	public static final Identifier<LiteralList> identifier = new Identifier<>();

	protected int[] fixedVariables;
	protected int[] variableOrder;
	protected int newCount;

	public void setFixedFeatures(int[] fixedVariables, int newCount) {
		this.fixedVariables = fixedVariables;
		this.newCount = newCount;
	}

	public void setVariableOrder(int[] variableOrder) {
		this.variableOrder = variableOrder;
	}

	public void resetFixedFeatures() {
		fixedVariables = new int[0];
		newCount = 0;
	}

	@Override
	public Identifier<LiteralList> getIdentifier() {
		return identifier;
	}

	public ConditionallyCoreDeadAnalysisMIG() {
		super();
		resetFixedFeatures();
	}

	@Override
	public LiteralList analyze(Sat4JMIGSolver solver, InternalMonitor monitor) throws Exception {
		monitor.setTotalWork(solver.getVariables().size() + 2);

		final Traverser traverser = solver.mig.traverse();
		solver.getAssumptions().ensureSize(fixedVariables.length + 1);
		final int[] knownValues = new int[solver.getVariables().size()];

		for (final int fixedVar : fixedVariables) {
			final int var = Math.abs(fixedVar);
			knownValues[var - 1] = fixedVar;
			monitor.step();
		}

		// get core / dead variables
		for (final Vertex vertex : solver.mig.getVertices()) {
			if (vertex.isCore()) {
				final int var = vertex.getVar();
				knownValues[Math.abs(var) - 1] = var;
				monitor.step();
			}
		}

		traverser.setModel(knownValues);
		final CollectingVisitor visitor = new CollectingVisitor();
		traverser.setVisitor(visitor);
		for (int i = 0; i < newCount; i++) {
			traverser.traverse(fixedVariables[i]);
		}
		final VecInt computedValues = visitor.getResult()[0];
		VecInt valuesToCompute = visitor.getResult()[1];

		monitor.setTotalWork(valuesToCompute.size() + computedValues.size() + 3);

		for (int i = 0; i < computedValues.size(); i++) {
			final int computedVar = computedValues.get(i);
			final int var = Math.abs(computedVar);
			knownValues[var - 1] = computedVar;
			monitor.step();
		}

		if (variableOrder != null) {
			final VecInt sortedValuesToCalculate = new VecInt(valuesToCompute.size());
			for (int i = variableOrder.length - 1; i >= 0; i--) {
				final int var = variableOrder[i];
				if (valuesToCompute.contains(var)) {
					sortedValuesToCalculate.push(var);
				}
				if (valuesToCompute.contains(-var)) {
					sortedValuesToCalculate.push(-var);
				}
			}
			valuesToCompute = sortedValuesToCalculate;
		}

		for (final int var : knownValues) {
			if (var != 0) {
				solver.getAssumptions().push(var);
			}
		}
		monitor.checkCancel();

		if (!valuesToCompute.isEmpty()) {
			solver.setSelectionStrategy(SStrategy.positive());
			final int[] unknownValues = solver.findSolution().getLiterals();
			monitor.step();

			if (unknownValues != null) {
				solver.setSelectionStrategy(SStrategy.negative());
				final int[] model2 = solver.findSolution().getLiterals();
				monitor.step();

				LiteralList.resetConflicts(unknownValues, model2);
				solver.setSelectionStrategy(SStrategy.inverse(unknownValues));

				for (int k = 0; k < knownValues.length; k++) {
					final int var = knownValues[k];
					if ((var != 0) && (unknownValues[k] != 0)) {
						unknownValues[k] = 0;
					}
				}
				monitor.step();

				sat(solver, unknownValues, valuesToCompute, monitor, traverser);
			}
		}
		return new LiteralList(solver.getAssumptions().asArray(0, solver.getAssumptions().size()));
	}

	private void sat(Sat4JMIGSolver solver, int[] unknownValues, VecInt valuesToCalculate, InternalMonitor monitor,
		Traverser traverser) {
		final CollectingVisitor visitor = new CollectingVisitor();
		traverser.setVisitor(visitor);

		while (!valuesToCalculate.isEmpty()) {
			final int varX = valuesToCalculate.last();
			valuesToCalculate.pop();
			final int i = Math.abs(varX) - 1;
			if (unknownValues[i] == varX) {
				solver.getAssumptions().push(-varX);
				switch (solver.hasSolution()) {
				case FALSE:
					solver.getAssumptions().replaceLast(varX);
					unknownValues[i] = 0;
					monitor.step();
					traverser.traverseStrong(varX);
					final VecInt newFoundValues = visitor.getResult()[0];
					for (int j = 0; j < newFoundValues.size(); j++) {
						final int var = newFoundValues.get(j);
						solver.getAssumptions().push(var);
						unknownValues[Math.abs(var) - 1] = 0;
						monitor.step();
					}
					break;
				case TIMEOUT:
					solver.getAssumptions().pop();
					unknownValues[Math.abs(varX) - 1] = 0;
					monitor.step();
					break;
				case TRUE:
					solver.getAssumptions().pop();
					LiteralList.resetConflicts(unknownValues, solver.getInternalSolution());
					solver.shuffleOrder(getRandom());
					break;
				}
			}
		}
	}

}
