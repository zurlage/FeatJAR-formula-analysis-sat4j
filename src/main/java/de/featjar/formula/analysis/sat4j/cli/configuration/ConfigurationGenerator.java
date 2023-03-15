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

import de.featjar.base.FeatJAR;
import de.featjar.base.cli.*;
import de.featjar.base.io.format.IFormat;
import de.featjar.formula.analysis.sat4j.todo.configuration.AbstractConfigurationGenerator;
import de.featjar.formula.analysis.bool.BooleanSolutionList;
import de.featjar.formula.io.ListFormat;
import de.featjar.formula.io.FormulaFormats;
import de.featjar.base.data.Result;
import de.featjar.base.log.Log;
import de.featjar.formula.structure.formula.IFormula;
import de.featjar.formula.transformer.ComputeCNFFormula;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.stream.Collectors;

import static de.featjar.base.computation.Computations.async;

/**
 * Command line interface for sampling algorithms.
 *
 * @author Sebastian Krieter
 * @author Elias Kuiter
 */
public class ConfigurationGenerator implements ICommand {
    public static final Option<String> OUTPUT_FORMAT_OPTION = new StringOption("format")
            .setRequired(true)
            .setDescription(() -> "Specify format by identifier. One of " +
                    getFormats().stream()
                            .map(IFormat::getName)
                            .map(String::toLowerCase)
                            .collect(Collectors.joining(", ")));

    public static final Option<Boolean> DRY_RUN_OPTION = new Flag("dry-run")
            .setDescription("Perform dry run");

    public static final Option<Boolean> RECURSIVE_OPTION = new Flag("recursive")
            .setDescription("");

    public static final Option<Boolean> CNF_OPTION = new Flag("cnf")
            .setDescription("Transform into CNF before conversion");

    @Override
    public List<Option<?>> getOptions() {
        return List.of(INPUT_OPTION, OUTPUT_OPTION, OUTPUT_FORMAT_OPTION, DRY_RUN_OPTION, RECURSIVE_OPTION);
    }

    @Override
    public String getDescription() {
        return "Converts formulas from one format to another";
    }

    protected static List<IFormat<IFormula>> getFormats() {
        return FeatJAR.extensionPoint(ConfigurationGeneratorAlgorithms.class).getExtensions();
        private final List<AlgorithmWrapper<? extends AbstractConfigurationGenerator>> algorithms =

    }

    @Override
    public void run(IOptionInput argumentParser) {
        String input = INPUT_OPTION.parseFrom(argumentParser).get();
        String output = OUTPUT_OPTION.parseFrom(argumentParser).get();
        String outputFormatString = OUTPUT_FORMAT_OPTION.parseFrom(argumentParser).get();
        IFormat<IFormula> outputFormat = getFormats().stream() // todo: find by extension ID
                .filter(f -> Objects.equals(outputFormatString, f.getName().toLowerCase()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown format: " + outputFormatString));
        boolean dryRun = DRY_RUN_OPTION.parseFrom(argumentParser).get();
        boolean recursive = RECURSIVE_OPTION.parseFrom(argumentParser).get();
        boolean CNF = CNF_OPTION.parseFrom(argumentParser).get();
        if (!Commands.isValidInput(input)) {
            throw new IllegalArgumentException("input file invalid");
        }
        FeatJAR.log().info(input + " -> " + output);
        if (!dryRun) {
            convert(input, output, outputFormat, CNF);
        }
    }

    private void convert(String input, String output, IFormat<IFormula> outputFormat, boolean CNF) {
        try {
            final Result<IFormula> formula = Commands.loadFile(input, FeatJAR.extensionPoint(FormulaFormats.class));
            if (!formula.isPresent()) {
                FeatJAR.log().error("formula file could not be parsed");
            }
            if (formula.hasProblems())
                FeatJAR.log().problem(formula.getProblems().get());
            IFormula expression = formula.get();
            if (CNF) {
                expression = async(expression).map(ComputeCNFFormula::new).getResult().get();
            }
            Commands.saveFile(expression, output, outputFormat);
        } catch (final Exception e) {
            FeatJAR.log().error(e);
        }
    }



    @Override
    public String getName() {
        return "genconfig";
    }

    @Override
    public String getDescription() {
        return "Generates configurations with various sampling algorithms";
    }

    @Override
    public void run(List<String> args) {
        String input = Commands.STANDARD_INPUT;
        String output = Commands.STANDARD_OUTPUT;
        AlgorithmWrapper<? extends AbstractConfigurationGenerator> algorithm = null;
        int limit = Integer.MAX_VALUE;
        String verbosity = Commands.DEFAULT_VERBOSITY;

        final List<String> remainingArguments = new ArrayList<>();
        for (final ListIterator<String> iterator = args.listIterator(); iterator.hasNext(); ) {
            final String arg = iterator.next();
            switch (arg) {
                case "-a": {
                    // TODO add plugin for icpl and chvatal
                    final String name = Commands.getArgValue(iterator, arg).toLowerCase();
                    algorithm = algorithms.stream()
                            .filter(a -> Objects.equals(name, a.getName()))
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("Unknown algorithm: " + name));
                    break;
                }
                case "-o": {
                    output = Commands.getArgValue(iterator, arg);
                    break;
                }
                case "-i": {
                    input = Commands.getArgValue(iterator, arg);
                    break;
                }
                case "-v": {
                    verbosity = Commands.getArgValue(iterator, arg);
                    break;
                }
                case "-l":
                    limit = Integer.parseInt(Commands.getArgValue(iterator, arg));
                    break;
                default: {
                    remainingArguments.add(arg);
                    break;
                }
            }
        }

        Commands.installLog(verbosity);

        if (algorithm == null) {
            throw new IllegalArgumentException("No algorithm specified!");
        }
        final AbstractConfigurationGenerator generator =
                algorithm.parseArguments(remainingArguments).orElse(Log::problem);
        if (generator != null) {
            generator.setLimit(limit);
            final ModelRepresentation c = Commands.loadFile(input, FormulaFormats.getInstance()) //
                    .map(ModelRepresentation::new) //
                    .orElseThrow(p -> new IllegalArgumentException(
                            p.isEmpty() ? null : p.get(0).toException()));
            final Result<BooleanSolutionList> result = c.getResult(generator);
            String finalOutput = output;
            result.ifPresentOrElse(list -> Commands.saveFile(list, finalOutput, new ListFormat()), Log::problem);
        }
    }

    @Override
    public String getUsage() {
        final StringBuilder helpBuilder = new StringBuilder();
        helpBuilder.append("\tGeneral Parameters:\n");
        helpBuilder.append("\t\t-i <Path>    Specify path to input feature model file (default: system:in.xml>)\n");
        helpBuilder.append("\t\t-o <Path>    Specify path to output CSV file (default: system:out>)\n");
        helpBuilder.append("\t\t-v <Level>   Specify verbosity. One of: none, error, info, debug, progress\n");
        helpBuilder.append("\t\t-a <Name>    Specify algorithm by name. One of:\n");
        algorithms.forEach(a ->
                helpBuilder.append("\t\t                 ").append(a.getName()).append("\n"));
        helpBuilder.append("\n");
        helpBuilder.append("\tAlgorithm Specific Parameters:\n\t");
        algorithms.forEach(a -> helpBuilder.append(a.getHelp().replace("\n", "\n\t")));
        return helpBuilder.toString();
    }
}
