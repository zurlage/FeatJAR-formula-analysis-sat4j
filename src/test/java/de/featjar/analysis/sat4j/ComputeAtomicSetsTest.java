/*
 * Copyright (C) 2025 FeatJAR-Development-Team
 *
 * This file is part of FeatJAR-FeatJAR-formula-analysis-sat4j.
 *
 * FeatJAR-formula-analysis-sat4j is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3.0 of the License,
 * or (at your option) any later version.
 *
 * FeatJAR-formula-analysis-sat4j is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FeatJAR-formula-analysis-sat4j. If not, see <https://www.gnu.org/licenses/>.
 *
 * See <https://github.com/FeatureIDE/FeatJAR-formula-analysis-sat4j> for further information.
 */
package de.featjar.analysis.sat4j;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.featjar.Common;
import de.featjar.analysis.sat4j.computation.ComputeAtomicSetsSAT4J;
import de.featjar.base.computation.Computations;
import de.featjar.formula.assignment.BooleanAssignmentList;
import de.featjar.formula.assignment.ComputeBooleanClauseList;
import de.featjar.formula.computation.ComputeCNFFormula;
import de.featjar.formula.computation.ComputeNNFFormula;
import de.featjar.formula.structure.IFormula;
import org.junit.jupiter.api.Test;

public class ComputeAtomicSetsTest extends Common {

    @Test
    public void gplHas27AtomicSets() {
        BooleanAssignmentList atomicSets = Computations.of(loadFormula("GPL/model.xml"))
                .cast(IFormula.class)
                .map(ComputeNNFFormula::new)
                .map(ComputeCNFFormula::new)
                .map(ComputeBooleanClauseList::new)
                .map(ComputeAtomicSetsSAT4J::new)
                .compute();

        assertEquals(22, atomicSets.size());
    }
}
