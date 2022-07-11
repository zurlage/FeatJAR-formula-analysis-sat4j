/* -----------------------------------------------------------------------------
 * formula-analysis-sat4j - Analysis of propositional formulas using Sat4j
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
 * See <https://github.com/FeatJAR/formula-analysis-sat4j> for further information.
 * -----------------------------------------------------------------------------
 */
package de.featjar.assignment;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import de.featjar.analysis.Analysis;
import de.featjar.analysis.mig.ConditionallyCoreDeadAnalysisMIG;
import de.featjar.analysis.sat4j.*;
import de.featjar.clauses.CNF;
import de.featjar.clauses.CNFProvider;
import de.featjar.clauses.LiteralList;
import de.featjar.formula.ModelRepresentation;
import de.featjar.formula.structure.atomic.literal.Literal;
import de.featjar.formula.structure.atomic.literal.VariableMap;
import de.featjar.formula.structure.compound.And;
import de.featjar.formula.structure.compound.Or;
import de.featjar.transform.CNFSlicer;
import de.featjar.util.data.Problem;
import de.featjar.util.data.Result;
import de.featjar.util.job.Executor;
import de.featjar.util.logging.Logger;
import org.junit.jupiter.api.*;
import de.featjar.analysis.*;
import de.featjar.analysis.mig.*;
import de.featjar.analysis.sat4j.*;
import de.featjar.clauses.*;
import de.featjar.formula.*;
import de.featjar.formula.structure.atomic.literal.*;
import de.featjar.formula.structure.compound.*;
import de.featjar.formula.structure.term.bool.*;
import de.featjar.transform.*;
import de.featjar.util.data.*;
import de.featjar.util.job.*;
import de.featjar.util.logging.*;

public class CNFTest {

	@Test
	public void testAnalyses() {
		final VariableMap variables = VariableMap.fromNames(
			Arrays.asList("a", "b", "c", "d", "e"));
		final Literal a = new LiteralPredicate((BoolVariable) variables.getVariable("a").get(), true);
		final Literal b = new LiteralPredicate((BoolVariable) variables.getVariable("b").get(), true);
		final Literal c = new LiteralPredicate((BoolVariable) variables.getVariable("c").get(), true);
		final Literal d = new LiteralPredicate((BoolVariable) variables.getVariable("d").get(), true);
		final Literal e = new LiteralPredicate((BoolVariable) variables.getVariable("e").get(), true);

		final And formula = new And(
			new Or(d),
			new Or(e.flip()),
			new Or(a, b),
			new Or(a.flip(), c),
			new Or(d, b, e.flip()),
			new Or(b.flip(), c, d),
			new Or(c.flip(), d.flip(), e.flip()));

		final ModelRepresentation rep = new ModelRepresentation(formula);

		executeAnalysis(rep, new HasSolutionAnalysis());
		executeAnalysis(rep, new AddRedundancyAnalysis());
		executeAnalysis(rep, new AtomicSetAnalysis());
		executeAnalysis(rep, new CauseAnalysis());
		executeAnalysis(rep, new ContradictionAnalysis());
		executeAnalysis(rep, new CoreDeadAnalysis());
		executeAnalysis(rep, new CountSolutionsAnalysis());
		executeAnalysis(rep, new IndependentContradictionAnalysis());
		executeAnalysis(rep, new IndependentRedundancyAnalysis());
		executeAnalysis(rep, new IndeterminateAnalysis());
		executeAnalysis(rep, new RemoveRedundancyAnalysis());
		executeAnalysis(rep, new ConditionallyCoreDeadAnalysisMIG());

		final CNF cnf = rep.get(CNFProvider.fromFormula());
		final CNFSlicer slicer = new CNFSlicer(new LiteralList(2));
		final CNF slicedCNF = Executor.run(slicer, cnf).orElse(Logger::logProblems);

		cnf.adapt(slicedCNF.getVariableMap()).orElse(Logger::logProblems);
		slicedCNF.adapt(cnf.getVariableMap()).orElse(Logger::logProblems);
	}

	private void executeAnalysis(ModelRepresentation rep, Analysis<?> analysis) {
		final Result<?> result = rep.getResult(analysis);
		Logger.logInfo(analysis.getClass().getName());
		result.map(Object::toString).orElse(this::reportProblems);
	}

	private void reportProblems(List<Problem> problems) {
		Logger.logProblems(problems);
		fail();
	}

}
