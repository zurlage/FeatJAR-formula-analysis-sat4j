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
package org.spldev.analysis.mig.solver;

import java.nio.file.*;

import org.spldev.analysis.mig.io.*;
import org.spldev.clauses.*;
import org.spldev.util.data.*;
import org.spldev.util.io.format.*;

/**
 * Abstract creator to derive an element from a {@link Cache }.
 *
 * @author Sebastian Krieter
 */
@FunctionalInterface
public interface MIGProvider extends Provider<MIG> {

	Identifier<MIG> identifier = new Identifier<>();

	@Override
	default Identifier<MIG> getIdentifier() {
		return identifier;
	}

	static MIGProvider empty() {
		return (c, m) -> Result.empty();
	}

	static MIGProvider of(MIG mig) {
		return (c, m) -> Result.of(mig);
	}

	static MIGProvider loader(Path path) {
		return (c, m) -> Provider.load(path, FormatSupplier.of(new MIGFormat()));
	}

	static <T> MIGProvider fromFormula() {
		return (c, m) -> Provider.convert(c, CNFProvider.identifier, new RegularMIGBuilder(), m);
	}

	static <T> MIGProvider fromCNF() {
		return (c, m) -> Provider.convert(c, CNFProvider.fromFormula(), new RegularMIGBuilder(), m);
	}

//	static <T> MIGProvider fromOldMig(MIG oldMig) {
//		return (c, m) -> Provider.convert(c, CNFProvider.identifier, new IncrementalMIGBuilder(oldMig), m);
//	}

}
