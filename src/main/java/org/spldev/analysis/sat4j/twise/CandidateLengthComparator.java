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
package org.spldev.analysis.sat4j.twise;

import java.util.*;

import org.spldev.clauses.*;
import org.spldev.util.data.*;

/**
 * Compares two candidates for covering, consisting of a partial configuration
 * and a literal set. Considers number of literals in the partial configuration
 * and in the literal set.
 *
 * @author Sebastian Krieter
 */
class CandidateLengthComparator implements Comparator<Pair<LiteralList, TWiseConfiguration>> {

	@Override
	public int compare(Pair<LiteralList, TWiseConfiguration> o1, Pair<LiteralList, TWiseConfiguration> o2) {
		final int diff = o2.getValue().countLiterals - o1.getValue().countLiterals;
		return diff != 0 ? diff : o2.getKey().size() - o1.getKey().size();
	}

}
