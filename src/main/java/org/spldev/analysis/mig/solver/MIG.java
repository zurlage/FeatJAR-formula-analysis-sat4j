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
package org.spldev.analysis.mig.solver;

import java.util.*;

import org.spldev.analysis.mig.solver.Vertex.*;
import org.spldev.analysis.mig.solver.visitor.*;
import org.spldev.analysis.solver.*;
import org.spldev.clauses.*;

/**
 * Adjacency list implementation for a feature graph.
 *
 * @author Sebastian Krieter
 */
public class MIG {

	public enum BuildStatus {
		None, Incremental, Complete
	}

	public static int getVertexIndex(int literal) {
		return literal < 0
			? (-(literal + 1)) << 1
			: (((literal - 1) << 1) + 1);
	}

	public static int getVertexIndex(Vertex vertex) {
		return getVertexIndex(vertex.getVar());
	}

	private final ArrayList<LiteralList> detectedStrong = new ArrayList<>();

	private final List<Vertex> adjList;
	private final CNF cnf;

	private BuildStatus redundancyStatus = BuildStatus.None;
	private BuildStatus strongStatus = BuildStatus.None;

	public MIG(CNF cnf) {
		this.cnf = cnf;
		final int numVariables = cnf.getVariableMap().size();
		adjList = new ArrayList<>(numVariables << 1);
		for (int i = 0; i < numVariables; i++) {
			addVertex();
		}
	}

	private void addVertex() {
		final int nextID = size() + 1;
		adjList.add(new Vertex(-nextID));
		adjList.add(new Vertex(nextID));
	}

	public void copyValues(MIG other) {
		adjList.addAll(other.adjList);
	}

	public Traverser traverse() {
		return new Traverser(this);
	}

	public Vertex getVertex(int literal) {
		return adjList.get(getVertexIndex(literal));
	}

	public List<Vertex> getVertices() {
		return Collections.unmodifiableList(adjList);
	}

	public ArrayList<LiteralList> getDetectedStrong() {
		return detectedStrong;
	}

	public int size() {
		return adjList.size() >> 1;
	}

	public CNF getCnf() {
		return cnf;
	}

	public void addClause(LiteralList clause) {
		final int[] literals = clause.getLiterals();
		switch (clause.size()) {
		case 0:
			throw new RuntimeContradictionException();
		case 1: {
			final int literal = literals[0];
			if (literal > 0) {
				getVertex(literal).setStatus(Status.Core);
				getVertex(-literal).setStatus(Status.Dead);
			} else if (literal < 0) {
				getVertex(literal).setStatus(Status.Dead);
				getVertex(-literal).setStatus(Status.Core);
			} else {
				throw new RuntimeContradictionException();
			}
			break;
		}
		case 2: {
			getVertex(-literals[0]).addStronglyConnected(getVertex(literals[1]));
			getVertex(-literals[1]).addStronglyConnected(getVertex(literals[0]));
			break;
		}
		default: {
			for (final int literal : literals) {
				getVertex(-literal).addWeaklyConnected(clause);
			}
			break;
		}
		}
	}

	public BuildStatus getStrongStatus() {
		return strongStatus;
	}

	public void setStrongStatus(BuildStatus strongStatus) {
		this.strongStatus = strongStatus;
	}

	public BuildStatus getRedundancyStatus() {
		return redundancyStatus;
	}

	public void setRedundancyStatus(BuildStatus redundancyStatus) {
		this.redundancyStatus = redundancyStatus;
	}

//	public void removeClause(LiteralList clause) {
//		final int[] literals = clause.getLiterals();
//		switch (clause.size()) {
//		case 0:
//			throw new RuntimeContradictionException();
//		case 1: {
//			break;
//		}
//		case 2: {
//			getVertex(-literals[0]).getStrongEdges().remove(getVertex(literals[1]));
//			getVertex(-literals[1]).getStrongEdges().remove(getVertex(literals[0]));
//			break;
//		}
//		default: {
//			for (final int literal : literals) {
//				final Vertex vertex = getVertex(-literal);
//				// TODO increase performance
//				vertex.getComplexClauses().remove(clause);
//			}
//			break;
//		}
//		}
//	}

}
