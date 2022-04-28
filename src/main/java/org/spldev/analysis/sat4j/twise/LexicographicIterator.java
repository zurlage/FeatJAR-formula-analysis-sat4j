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
package org.spldev.analysis.sat4j.twise;

import java.util.*;

/**
 * Presence condition combination iterator that iterates of the combinations in
 * lexicographical order.
 *
 * @author Sebastian Krieter
 */
public class LexicographicIterator extends ACombinationIterator {

	private final int[] c;

	public LexicographicIterator(int t, List<PresenceCondition> expressions) {
		super(t, expressions);
		c = new int[t];
		for (int i = 0; i < (c.length - 1); i++) {
			c[i] = i;
		}
		c[t - 1] = t - 2;
	}

	@Override
	protected int[] computeCombination(long index) {
		int i = t;
		for (; i > 0; i--) {
			final int ci = ++c[i - 1];
			if (ci < ((n - t) + i)) {
				break;
			}
		}
		if ((i == 0) && (c[i] == ((n - t) + 1))) {
			return null;
		}

		for (; i < t; i++) {
			if (i == 0) {
				c[i] = 0;
			} else {
				c[i] = c[i - 1] + 1;
			}
		}
		return c;
	}

	@Override
	protected long nextIndex() {
		return 0;
	}

	@Override
	public long getIndex() {
		long index = 0;
		for (int i = 0; i < c.length; i++) {
			index += binomialCalculator.binomial(c[i], i + 1);
		}
		return index;
	}

	@Override
	public void reset() {
		super.reset();
		for (int i = 0; i < (c.length - 1); i++) {
			c[i] = i;
		}
		c[t - 1] = t - 2;
	}
}
