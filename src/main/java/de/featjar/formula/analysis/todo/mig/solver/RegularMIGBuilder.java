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
package de.featjar.formula.analysis.todo.mig.solver;

import de.featjar.base.task.IMonitor;

/**
 * Adjacency matrix implementation for a feature graph.
 *
 * @author Sebastian Krieter
 */
public class RegularMIGBuilder extends MIGBuilder {

    @Override
    public ModalImplicationGraph execute(CNF cnf, IMonitor monitor) {
        monitor.setTotalSteps(24 + (detectStrong ? 1020 : 0) + (checkRedundancy ? 100 : 10));

        init(cnf);
        monitor.addStep();

        if (!satCheck(cnf)) {
            throw new SolverContradictionException("CNF is not satisfiable!");
        }
        monitor.addStep();
        findCoreFeatures(monitor.newChildMonitor(10));

        cleanClauses();
        monitor.addStep();

        if (detectStrong) {
            addClauses(cnf, false, monitor.newChildMonitor(10));

            bfsStrong(monitor.newChildMonitor(10));

            bfsWeak(null, monitor.newChildMonitor(1000));
            modalImplicationGraph.setStrongStatus(ModalImplicationGraph.BuildStatus.Complete);
        } else {
            modalImplicationGraph.setStrongStatus(ModalImplicationGraph.BuildStatus.None);
        }

        addClauses(cnf, checkRedundancy, monitor.newChildMonitor(checkRedundancy ? 100 : 10));

        bfsStrong(monitor.newChildMonitor(10));

        finish();
        monitor.addStep();

        return modalImplicationGraph;
    }
}
