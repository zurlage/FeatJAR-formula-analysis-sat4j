/*
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
 * See <https://github.com/FeatureIDE/FeatJAR-formula-analysis-sat4j> for further information.
 */
package de.featjar.analysis.mig.solver;

import de.featjar.analysis.mig.io.MIGFormat;
import de.featjar.clauses.CNFComputation;
import de.featjar.base.data.Store;
import de.featjar.base.data.Computation;
import de.featjar.base.data.Result;
import de.featjar.base.io.IO;

import java.nio.file.Path;

/**
 * Abstract creator to derive an element from a {@link Store }.
 *
 * @author Sebastian Krieter
 */
@FunctionalInterface
public interface MIGComputation extends Computation<MIG> {
    static MIGComputation empty() {
        return (c, m) -> Result.empty();
    }

    static MIGComputation of(MIG mig) {
        return (c, m) -> Result.of(mig);
    }

    static MIGComputation loader(Path path) {
        return (c, m) -> IO.load(path, new MIGFormat());
    }

    static <T> MIGComputation fromFormula() {
        return (c, m) -> Computation.convert(c, CNFComputation.identifier, new RegularMIGBuilder(), m);
    }

    static <T> MIGComputation fromCNF() {
        return (c, m) -> Computation.convert(c, CNFComputation.fromFormula(), new RegularMIGBuilder(), m);
    }

    //	static <T> MIGProvider fromOldMig(MIG oldMig) {
    //		return (c, m) -> Provider.convert(c, CNFProvider.identifier, new IncrementalMIGBuilder(oldMig), m);
    //	}

}
