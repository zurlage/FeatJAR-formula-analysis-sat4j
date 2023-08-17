/*
 * Copyright (C) 2023 FeatJAR-Development-Team
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
package de.featjar.assignment;

import static de.featjar.base.computation.Computations.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import de.featjar.base.computation.Computations;
import de.featjar.base.io.IO;
import de.featjar.formula.analysis.bool.BooleanAssignmentList;
import de.featjar.formula.analysis.bool.BooleanClauseList;
import de.featjar.formula.analysis.bool.BooleanRepresentationComputation;
import de.featjar.formula.analysis.sat4j.ComputeAtomicSetsSAT4J;
import de.featjar.formula.io.KConfigReaderFormat;
import de.featjar.formula.structure.formula.IFormula;
import de.featjar.formula.transformer.ComputeCNFFormula;
import de.featjar.formula.transformer.ComputeNNFFormula;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

public class CNFTransformTest {

    @Test
    public void testDistributiveBug() {
        final Path modelFile = Paths.get("src/test/resources/kconfigreader/distrib-bug.model");

        BooleanAssignmentList atomicSets = await(IO.load(modelFile, new KConfigReaderFormat())
                .toComputation()
                .cast(IFormula.class)
                .map(ComputeNNFFormula::new)
                .map(ComputeCNFFormula::new)
                .map(BooleanRepresentationComputation::new)
                .map(Computations::getKey)
                .cast(BooleanClauseList.class)
                .map(ComputeAtomicSetsSAT4J::new));

        // TODO correct? replace with better test
        assertEquals(2, atomicSets.size());
    }
}
