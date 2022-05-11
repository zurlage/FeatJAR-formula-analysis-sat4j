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
package org.spldev.assignment;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.*;
import org.spldev.analysis.*;
import org.spldev.analysis.mig.*;
import org.spldev.analysis.sat4j.*;
import org.spldev.clauses.*;
import org.spldev.formula.*;
import org.spldev.formula.structure.atomic.literal.*;
import org.spldev.formula.structure.compound.*;
import org.spldev.formula.structure.term.bool.*;
import org.spldev.transform.*;
import org.spldev.util.data.*;
import org.spldev.util.job.*;
import org.spldev.util.logging.*;

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
