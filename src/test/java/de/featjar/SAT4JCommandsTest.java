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
package de.featjar;

import de.featjar.base.ProcessOutput;
import java.io.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SAT4JCommandsTest {

    private static final String sat4jstring = "java -jar build/libs/formula-analysis-sat4j-0.1.1-SNAPSHOT-all.jar";

    @Test
    void testProjectionCommand() throws IOException {
        ProcessOutput output = ProcessOutput.runProcess(
                sat4jstring
                        + " projection-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --slice DirectedWithEdges,DirectedWithNeighbors");
        Assertions.assertTrue(output.getErrorString().isBlank());
        Assertions.assertFalse(output.getOutputString().contains("DirectedWithEdges"));
        Assertions.assertFalse(output.getOutputString().contains("DirectedWithNeighbors"));
        Assertions.assertTrue(output.getOutputString().contains("DirectedOnlyVertices"));

        output = ProcessOutput.runProcess(
                sat4jstring + " projection-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml "
                        + "--project DirectedWithEdges,DirectedWithNeighbors,DirectedOnlyVertices,UndirectedWithEdges,UndirectedWithNeighbors,UndirectedOnlyVertices "
                        + "--slice DirectedWithEdges,DirectedWithNeighbors");
        Assertions.assertTrue(output.getErrorString().isBlank());
        Assertions.assertTrue(output.getOutputString().contains("DirectedOnlyVertices"));
        Assertions.assertTrue(output.getOutputString().contains("UndirectedWithEdges"));
        Assertions.assertTrue(output.getOutputString().contains("UndirectedWithNeighbors"));
        Assertions.assertTrue(output.getOutputString().contains("UndirectedOnlyVertices"));
        Assertions.assertFalse(output.getOutputString().contains("DirectedWithEdges"));
        Assertions.assertFalse(output.getOutputString().contains("DirectedWithNeighbors"));
        Assertions.assertTrue(output.getOutputString().split("\n").length > 0);
        Assertions.assertEquals(6, output.getOutputString().split("\n")[0].split(";").length);

        output = ProcessOutput.runProcess(
                sat4jstring + " projection-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml "
                        + "--project DirectedOnlyVertices,UndirectedWithEdges,UndirectedWithNeighbors,UndirectedOnlyVertices");
        Assertions.assertTrue(output.getErrorString().isBlank());
        Assertions.assertTrue(output.getOutputString().contains("DirectedOnlyVertices"));
        Assertions.assertTrue(output.getOutputString().contains("UndirectedWithEdges"));
        Assertions.assertTrue(output.getOutputString().contains("UndirectedWithNeighbors"));
        Assertions.assertTrue(output.getOutputString().contains("UndirectedOnlyVertices"));
        Assertions.assertFalse(output.getOutputString().contains("DirectedWithEdges"));
        Assertions.assertFalse(output.getOutputString().contains("DirectedWithNeighbors"));
        Assertions.assertTrue(output.getOutputString().split("\n").length > 0);
        Assertions.assertEquals(6, output.getOutputString().split("\n")[0].split(";").length);
    }

    @Test
    void testCoreCommand() throws IOException {
        ProcessOutput noOptions = ProcessOutput.runProcess(
                sat4jstring + " core-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml");
        Assertions.assertTrue(noOptions.getErrorString().isBlank());

        ProcessOutput seedOption = ProcessOutput.runProcess(
                sat4jstring + " core-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --seed 0");
        Assertions.assertTrue(seedOption.getErrorString().isBlank());

        ProcessOutput solverTimeoutOption = ProcessOutput.runProcess(sat4jstring
                + " core-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --solver_timeout 10");
        Assertions.assertTrue(solverTimeoutOption.getErrorString().isBlank());

        ProcessOutput browserCacheOption = ProcessOutput.runProcess(sat4jstring
                + " core-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --browser-cache true");
        Assertions.assertTrue(browserCacheOption.getErrorString().isBlank());

        ProcessOutput nonParallelOption = ProcessOutput.runProcess(sat4jstring
                + " core-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --non-parallel true");
        Assertions.assertTrue(nonParallelOption.getErrorString().isBlank());

        ProcessOutput timeoutOption = ProcessOutput.runProcess(
                sat4jstring + " core-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --timeout 10");
        Assertions.assertTrue(timeoutOption.getErrorString().isBlank());
    }

    @Test
    void testAtomicSetsCommand() throws IOException {
        ProcessOutput noOptions = ProcessOutput.runProcess(
                sat4jstring + " atomic-sets-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml");
        Assertions.assertTrue(noOptions.getErrorString().isBlank());

        ProcessOutput seedOption = ProcessOutput.runProcess(sat4jstring
                + " atomic-sets-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --seed 0");
        Assertions.assertTrue(seedOption.getErrorString().isBlank());

        ProcessOutput solverTimeoutOption = ProcessOutput.runProcess(sat4jstring
                + " atomic-sets-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --solver_timeout 10");
        Assertions.assertTrue(solverTimeoutOption.getErrorString().isBlank());

        ProcessOutput browserCacheOption = ProcessOutput.runProcess(
                sat4jstring
                        + " atomic-sets-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --browser-cache true");
        Assertions.assertTrue(browserCacheOption.getErrorString().isBlank());

        ProcessOutput nonParallelOption = ProcessOutput.runProcess(sat4jstring
                + " atomic-sets-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --non-parallel true");
        Assertions.assertTrue(nonParallelOption.getErrorString().isBlank());

        ProcessOutput timeoutOption = ProcessOutput.runProcess(sat4jstring
                + " atomic-sets-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --timeout 10");
        Assertions.assertTrue(timeoutOption.getErrorString().isBlank());
    }

    @Test
    void testSolutionCountCommand() throws IOException {
        ProcessOutput noOptions = ProcessOutput.runProcess(
                sat4jstring + " count-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml");
        Assertions.assertTrue(noOptions.getErrorString().isBlank());

        ProcessOutput seedOption = ProcessOutput.runProcess(
                sat4jstring + " count-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --seed 0");
        Assertions.assertTrue(seedOption.getErrorString().isBlank());

        ProcessOutput solverTimeoutOption = ProcessOutput.runProcess(sat4jstring
                + " count-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --solver_timeout 10");
        Assertions.assertTrue(solverTimeoutOption.getErrorString().isBlank());

        ProcessOutput browserCacheOption = ProcessOutput.runProcess(sat4jstring
                + " count-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --browser-cache true");
        Assertions.assertTrue(browserCacheOption.getErrorString().isBlank());

        ProcessOutput nonParallelOption = ProcessOutput.runProcess(sat4jstring
                + " count-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --non-parallel true");
        Assertions.assertTrue(nonParallelOption.getErrorString().isBlank());

        ProcessOutput timeoutOption = ProcessOutput.runProcess(
                sat4jstring + " count-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --timeout 10");
        Assertions.assertTrue(timeoutOption.getErrorString().isBlank());
    }

    @Test
    void testSolutionsCommand() throws IOException {
        ProcessOutput noOptions = ProcessOutput.runProcess(
                sat4jstring + " solutions-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml");
        Assertions.assertTrue(noOptions.getErrorString().isBlank());

        ProcessOutput seedOption = ProcessOutput.runProcess(
                sat4jstring + " solutions-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --seed 0");
        Assertions.assertTrue(seedOption.getErrorString().isBlank());

        ProcessOutput solverTimeoutOption = ProcessOutput.runProcess(sat4jstring
                + " solutions-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --solver_timeout 10");
        Assertions.assertTrue(solverTimeoutOption.getErrorString().isBlank());

        ProcessOutput limitOption = ProcessOutput.runProcess(
                sat4jstring + " solutions-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --n 10");
        Assertions.assertTrue(limitOption.getErrorString().isBlank());

        ProcessOutput strategyOption = ProcessOutput.runProcess(sat4jstring
                + " solutions-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --strategy negative");
        Assertions.assertTrue(strategyOption.getErrorString().isBlank());

        ProcessOutput noDuplicatesOption = ProcessOutput.runProcess(sat4jstring
                + " solutions-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --no-duplicates true");
        Assertions.assertTrue(noDuplicatesOption.getErrorString().isBlank());

        ProcessOutput browserCacheOption = ProcessOutput.runProcess(sat4jstring
                + " solutions-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --browser-cache true");
        Assertions.assertTrue(browserCacheOption.getErrorString().isBlank());

        ProcessOutput nonParallelOption = ProcessOutput.runProcess(sat4jstring
                + " solutions-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --non-parallel true");
        Assertions.assertTrue(nonParallelOption.getErrorString().isBlank());

        ProcessOutput timeoutOption = ProcessOutput.runProcess(sat4jstring
                + " solutions-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --timeout 10");
        Assertions.assertTrue(timeoutOption.getErrorString().isBlank());
    }

    @Test
    void testTWiseCommand() throws IOException {
        ProcessOutput noOptions = ProcessOutput.runProcess(
                sat4jstring + " t-wise-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml");
        Assertions.assertTrue(noOptions.getErrorString().isBlank());

        ProcessOutput seedOption = ProcessOutput.runProcess(
                sat4jstring + " t-wise-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --seed 0");
        Assertions.assertTrue(seedOption.getErrorString().isBlank());

        ProcessOutput solverTimeoutOption = ProcessOutput.runProcess(sat4jstring
                + " t-wise-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --solver_timeout 10");
        Assertions.assertTrue(solverTimeoutOption.getErrorString().isBlank());

        ProcessOutput limitOption = ProcessOutput.runProcess(
                sat4jstring + " t-wise-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --n 10");
        Assertions.assertTrue(limitOption.getErrorString().isBlank());

        ProcessOutput tOption = ProcessOutput.runProcess(
                sat4jstring + " t-wise-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --t 5");
        Assertions.assertTrue(tOption.getErrorString().isBlank());

        ProcessOutput iterationsOption = ProcessOutput.runProcess(
                sat4jstring + " t-wise-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --i 10");
        Assertions.assertTrue(iterationsOption.getErrorString().isBlank());

        ProcessOutput browserCacheOption = ProcessOutput.runProcess(sat4jstring
                + " t-wise-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --browser-cache true");
        Assertions.assertTrue(browserCacheOption.getErrorString().isBlank());

        ProcessOutput nonParallelOption = ProcessOutput.runProcess(sat4jstring
                + " t-wise-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --non-parallel true");
        Assertions.assertTrue(nonParallelOption.getErrorString().isBlank());

        ProcessOutput timeoutOption = ProcessOutput.runProcess(
                sat4jstring + " t-wise-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --timeout 10");
        Assertions.assertTrue(timeoutOption.getErrorString().isBlank());
    }
}
