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

import de.featjar.formula.analysis.sat4j.todo.twise.TWiseConfigurationGenerator;
import de.featjar.formula.analysis.bool.ABooleanAssignmentList;
import de.featjar.formula.io.ExpressionGroupFormat;
import de.featjar.base.cli.Commands;
import de.featjar.base.io.IO;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

/**
 * Generates configurations for a given propositional formula such that t-wise
 * feature coverage is achieved.
 *
 * @author Sebastian Krieter
 */
public class TWiseAlgorithm extends AlgorithmWrapper<TWiseConfigurationGenerator> {

    @Override
    protected TWiseConfigurationGenerator newAlgorithm() {
        return new TWiseConfigurationGenerator();
    }

    @Override
    protected boolean parseArgument(TWiseConfigurationGenerator gen, String arg, ListIterator<String> iterator)
            throws IllegalArgumentException {
        if (!super.parseArgument(gen, arg, iterator)) {
            switch (arg) {
                case "-s":
                    gen.setRandom(new Random(Long.parseLong(Commands.getArgValue(iterator, arg))));
                    break;
                case "-t":
                    gen.setT(Integer.parseInt(Commands.getArgValue(iterator, arg)));
                    break;
                case "-m":
                    gen.setIterations(Integer.parseInt(Commands.getArgValue(iterator, arg)));
                    break;
                case "-e":
                    gen.setNodes(readExpressionFile(Paths.get(Commands.getArgValue(iterator, arg))));
                    break;
                default:
                    return false;
            }
        }
        return true;
    }

    private List<List<ABooleanAssignmentList>> readExpressionFile(Path expressionFile) {
        final List<List<ABooleanAssignmentList>> expressionGroups;
        if (expressionFile != null) {
            expressionGroups = IO.load(expressionFile, new ExpressionGroupFormat())
                    .orElseThrow(p -> new IllegalArgumentException(
                            p.isEmpty() ? null : p.get(0).toException()));
        } else {
            expressionGroups = null;
        }
        return expressionGroups;
    }

    @Override
    public String getName() {
        return "yasa";
    }

    @Override
    public String getHelp() {
        final StringBuilder helpBuilder = new StringBuilder();
        helpBuilder.append("\t");
        helpBuilder.append(getName());
        helpBuilder.append(": generates a set of valid configurations such that t-wise feature coverage is achieved\n");
        helpBuilder.append("\t\t-t <Value>    Specify size of interactions to be covered\n");
        helpBuilder.append("\t\t-m <Value>    Specify number of iterations\n");
        helpBuilder.append("\t\t-l <Value>    Specify maximum number of configurations\n");
        helpBuilder.append("\t\t-s <Value>    Specify random seed\n");
        helpBuilder.append("\t\t-e <Path>     Specify path to expression file\n");
        return helpBuilder.toString();
    }
}
