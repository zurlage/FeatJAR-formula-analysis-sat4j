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
package org.spldev.analysis.mig.solver.visitor;

import java.util.*;

import org.sat4j.core.*;
import org.sat4j.specs.*;
import org.spldev.analysis.mig.solver.*;
import org.spldev.clauses.*;

public class RecursiveTraverser extends ATraverser {

	public RecursiveTraverser(MIG mig) {
		super(mig);
	}

	@Override
	public void setModel(int[] model) {
		super.setModel(model);
		Arrays.fill(dfsMark, false);
	}

	@Override
	public void traverse(int... literals) {
		for (final int curLiteral : literals) {
			traverse(true, curLiteral);
		}
	}

	@Override
	public void traverseStrong(int... literals) {
		for (final int curLiteral : literals) {
			traverseStrongRec(curLiteral);
		}
	}

	private void traverseStrongRec(int curLiteral) {
		final Vertex vertex = mig.getVertex(curLiteral);

		// Strong Edges
		for (final Vertex stronglyConnectedVertex : vertex.getStrongEdges()) {
			final int literal = stronglyConnectedVertex.getVar();
			final int modelIndex = Math.abs(literal) - 1;
			if (currentConfiguration[modelIndex] == 0) {
				currentConfiguration[modelIndex] = literal;
				visitor.visitStrong(literal);
			}
		}

		// Weak Edges
		final List<LiteralList> complexClauses = vertex.getComplexClauses();
		final VecInt v = new VecInt();
		outerLoop: for (final LiteralList clause : complexClauses) {
			v.clear();
			final int[] literals = clause.getLiterals();
			for (int j = 0; j < literals.length; j++) {
				final int literal = literals[j];
				if (literal == -curLiteral) {
					continue;
				}
				final int value = currentConfiguration[Math.abs(literal) - 1];

				if (value == 0) {
					// add literal to list
					if (v.size() >= 1) {
						continue outerLoop;
					}
					v.push(literal);
				} else {
					if (value == literal) {
						// Clause is satisfied
						continue outerLoop;
					} else {
						// Do nothing
					}
				}
			}

			if (v.size() == 1) {
				final int literal = v.get(0);
				final int modelIndex = Math.abs(literal) - 1;
				if (currentConfiguration[modelIndex] == 0) {
					currentConfiguration[modelIndex] = literal;
					visitor.visitStrong(literal);
					traverseStrongRec(literal);
				}
			}
		}
	}

	private void traverse(boolean strongPath, int curLiteral) {
		final Vertex vertex = mig.getVertex(curLiteral);

		if (strongPath) {
			final int modelIndex = Math.abs(curLiteral) - 1;
			if (currentConfiguration[modelIndex] == 0) {
				currentConfiguration[modelIndex] = curLiteral;
				visitor.visitStrong(curLiteral);
			}
		}

		final int vertexIndex = MIG.getVertexIndex(curLiteral);
		if (!dfsMark[vertexIndex]) {
			dfsMark[vertexIndex] = true;
			if (!strongPath) {
				visitor.visitWeak(curLiteral);
			}

			// Strong Edges
			for (final Vertex strongVertex : vertex.getStrongEdges()) {
				if (currentConfiguration[Math.abs(strongVertex.getVar()) - 1] == 0) {
					traverse(strongPath, strongVertex.getVar());
				}
			}

			final List<LiteralList> complexClauses = vertex.getComplexClauses();

			// Weak Edges
			final VecInt v = new VecInt();
			outerLoop: for (final LiteralList clause : complexClauses) {
				v.clear();
				final int[] literals = clause.getLiterals();
				for (int j = 0; j < literals.length; j++) {
					final int literal = literals[j];
					if (literal == -curLiteral) {
						continue;
					}
					final int value = currentConfiguration[Math.abs(literal) - 1];

					if (value == 0) {
						// add literal to list
						v.push(literal);
					} else {
						if (value == literal) {
							// Clause is satisfied
							continue outerLoop;
						} else {
							// Do nothing
						}
					}
				}

				if (v.size() == 1) {
					final int literal = v.get(0);
					if (currentConfiguration[Math.abs(literal) - 1] == 0) {
						traverse(strongPath, literal);
					}
				} else {
					for (final IteratorInt iterator = v.iterator(); iterator.hasNext();) {
						final int literal = iterator.next();
						if (currentConfiguration[Math.abs(literal) - 1] == 0) {
							traverse(false, literal);
						}
					}
				}
			}
		}
	}

}
