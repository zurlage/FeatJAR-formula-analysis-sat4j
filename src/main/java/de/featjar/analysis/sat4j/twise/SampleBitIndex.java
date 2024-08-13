/*
 * Copyright (C) 2024 FeatJAR-Development-Team
 *
 * This file is part of FeatJAR-formula-analysis-sat4j.
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
 * See <https://github.com/FeatureIDE/FeatJAR-formula-analysis-sat4j> for further information.
 */
package de.featjar.analysis.sat4j.twise;

import de.featjar.formula.assignment.ABooleanAssignment;
import java.util.BitSet;
import java.util.List;
import java.util.function.Predicate;

/**
 * Calculates statistics regarding t-wise feature coverage of a set of
 * solutions.
 *
 * @author Sebastian Krieter
 */
public class SampleBitIndex implements Predicate<int[]> {

    private final BitSet[] bitSetReference;
    private final int size;

    public SampleBitIndex(List<? extends ABooleanAssignment> sample, final int size) {
        this.size = size;
        bitSetReference = new BitSet[2 * size + 1];

        final int sampleSize = sample.size();
        for (int j = 1; j <= size; j++) {
            BitSet negIndices = new BitSet(sampleSize);
            BitSet posIndices = new BitSet(sampleSize);
            for (int i = 0; i < sampleSize; i++) {
                ABooleanAssignment config = sample.get(i);
                if (config.get(j - 1) < 0) {
                    negIndices.set(i);
                } else {
                    posIndices.set(i);
                }
            }
            bitSetReference[size - j] = negIndices;
            bitSetReference[j + size] = posIndices;
        }
    }

    private BitSet getBitSet(int[] literals) {
        BitSet first = bitSetReference[literals[0] + size];
        BitSet bitSet = new BitSet(first.size());
        bitSet.xor(first);
        for (int k = 1; k < literals.length; k++) {
            bitSet.and(bitSetReference[literals[k] + size]);
        }
        return bitSet;
    }

    @Override
    public boolean test(int[] literals) {
        return !getBitSet(literals).isEmpty();
    }

    public int index(int[] literals) {
        return getBitSet(literals).length();
    }

    public int size(int[] literals) {
        return getBitSet(literals).cardinality();
    }
}
