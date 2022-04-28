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
package org.spldev.assignment;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.*;

import org.junit.jupiter.api.*;
import org.spldev.analysis.sat4j.*;
import org.spldev.clauses.*;
import org.spldev.formula.*;
import org.spldev.formula.io.*;
import org.spldev.formula.structure.*;
import org.spldev.util.io.*;
import org.spldev.util.io.format.*;

public class CNFTransformTest {

	@Test
	public void testDistributiveBug() {
		final Path modelFile = Paths.get("src/test/resources/kconfigreader/distrib-bug.model");
		final Formula formula = FileHandler.load(modelFile, FormatSupplier.of(new KConfigReaderFormat())).orElseThrow();

		final ModelRepresentation rep = new ModelRepresentation(formula);
		final List<LiteralList> atomicSets = new AtomicSetAnalysis().getResult(rep).orElseThrow();
		assertEquals(5, atomicSets.size());
	}

}
