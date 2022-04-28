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
 * Presence condition combination iterator that uses the combinatorial number
 * system to enumerate all combinations and then alternately iterates over
 * certain randomized partitions of the combination space.
 *
 * @author Sebastian Krieter
 */
public class RandomPartitionIterator extends PartitionIterator {

	public RandomPartitionIterator(int t, List<PresenceCondition> expressions) {
		this(t, expressions, new Random(42));
	}

	public RandomPartitionIterator(int t, List<PresenceCondition> expressions, Random random) {
		super(t, expressions, 4);

		for (int i = 0; i < dim.length; i++) {
			final int[] dimArray = dim[i];
			for (int j = dimArray.length - 1; j >= 0; j--) {
				final int index = random.nextInt(j + 1);
				final int a = dimArray[index];
				dimArray[index] = dimArray[j];
				dimArray[j] = a;
			}
		}
	}

}
