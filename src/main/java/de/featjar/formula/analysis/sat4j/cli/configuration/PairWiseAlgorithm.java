/*
 * Copyright (C) 2022 Elias Kuiter
 *
 * This file is part of cli.
 *
 * cli is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3.0 of the License,
 * or (at your option) any later version.
 *
 * cli is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with cli. If not, see <https://www.gnu.org/licenses/>.
 *
 * See <https://github.com/FeatureIDE/FeatJAR-cli> for further information.
 */
package de.featjar.formula.analysis.sat4j.cli.configuration;

import de.featjar.formula.analysis.sat4j.todo.configuration.AbstractConfigurationGenerator;
import de.featjar.formula.analysis.sat4j.todo.configuration.PairWiseConfigurationGenerator;
import de.featjar.base.cli.Commands;
import java.util.ListIterator;
import java.util.Random;

/**
 * Generates configurations for a given propositional formula such that two-wise
 * feature coverage is achieved.
 *
 * @author Sebastian Krieter
 */
public class PairWiseAlgorithm extends AlgorithmWrapper<AbstractConfigurationGenerator> {

    @Override
    protected PairWiseConfigurationGenerator newAlgorithm() {
        return new PairWiseConfigurationGenerator();
    }

    @Override
    protected boolean parseArgument(AbstractConfigurationGenerator gen, String arg, ListIterator<String> iterator)
            throws IllegalArgumentException {
        switch (arg) {
            case "-s":
                gen.setRandom(new Random(Long.parseLong(Commands.getArgValue(iterator, arg))));
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    public String getName() {
        return "incling";
    }

    @Override
    public String getHelp() {
        final StringBuilder helpBuilder = new StringBuilder();
        helpBuilder.append("\t");
        helpBuilder.append(getName());
        helpBuilder.append(
                ": generates a set of valid configurations such that two-wise feature coverage is achieved\n");
        helpBuilder.append("\t\t-l <Value>    Specify maximum number of configurations\n");
        helpBuilder.append("\t\t-s <Value>    Specify random seed\n");
        return helpBuilder.toString();
    }
}
