/*
 *  Copyright 2018 Jakob Beckmann, ETH Zürich
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package ch.securify;

import java.util.*;
import java.io.*;
import java.nio.file.*;


class OutputGenerator {
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_YELLOW = "\u001B[33m";



    /**
     * Print the contract analysis output using syntax similar to clang style errors.
     *
     * @param  allContractResults the result from the contract analysis
     */
    static void print(TreeMap<String, SolidityResult> allContractResults) {
        allContractResults.forEach(OutputGenerator::handleContract);
    }


    /**
     * Handles the output production for a single contract.
     *
     * @param  contractIdentifier the identifier of a single contract extracted from the analysis; this consists of path:name
     * @param  contractResult     the result of the analysis on the contract named in the first argument
     */
    private static void handleContract(String contractIdentifier, SolidityResult contractResult) {
        String[] parts = contractIdentifier.split(":");
        String contractName = parts[parts.length - 1];
        String contractPath = contractIdentifier.substring(0, contractIdentifier.length() - contractName.length() - 1);

        try {
            List<String> contractLines = Files.readAllLines(Paths.get(contractPath));

            contractResult.results.forEach((name, pattern) ->
                    OutputGenerator.handlePattern(name, pattern, contractName, contractPath, contractLines));
        } catch(IOException e) {
            System.out.println("Could not open contract source file: " + contractPath);
        }
    }


    /**
     * Handles the output production for a single pattern.
     *
     * @param  patternName    the name of the pattern
     * @param  pattern       the actual pattern object containing violations, warnings, conflicts, and safe
     * @param  contractName the name of the contract for which this pattern applies
     * @param  sourceFile    the path of the source file containing the contract for which this pattern applies
     * @param  lines         the content of the source file containing the contract
     */
    private static void handlePattern(String patternName, SmallPatternResult pattern, String contractName, String sourceFile, List<String> lines) {
        for (Integer lineNum: pattern.violations) {
            StringBuilder header = new StringBuilder(OutputGenerator.ANSI_RED + "Violation" + OutputGenerator.ANSI_RESET + " for " + patternName);
            OutputGenerator.handleLine(contractName, sourceFile, lines, lineNum, header);
        }
        for (Integer lineNum: pattern.warnings) {
            StringBuilder header = new StringBuilder(OutputGenerator.ANSI_YELLOW + "Warning" + OutputGenerator.ANSI_RESET + " for " + patternName);
            OutputGenerator.handleLine(contractName, sourceFile, lines, lineNum, header);
        }
        for (Integer lineNum: pattern.conflicts) {
            StringBuilder header = new StringBuilder(OutputGenerator.ANSI_YELLOW + "Warning" + OutputGenerator.ANSI_RESET + " for " + patternName);
            OutputGenerator.handleLine(contractName, sourceFile, lines, lineNum, header);
        }
    }



    /**
     * Handles the output production for a single line of a pattern.
     *
     * @param  contractName the name of the contract to which this line applies
     * @param  sourceFile   the path to the source file containing the contract to which this lines applies
     * @param  lines        the content of the source file containing the contract
     * @param  lineNum      the line number within the contract to which this lines applies
     * @param  header       the header to prepend to this line of output
     */
    private static void handleLine(String contractName, String sourceFile, List<String> lines, int lineNum, StringBuilder header) {
        if (lineNum < 0) {
            header.append(String.format(" in contract '%s'\n", contractName));
        } else {
            header.append(String.format(" in contract '%s':\n", contractName));
            int lineStart = Math.max(0, lineNum - 2);
            int lineStop = Math.min(lines.size(), lineNum + 3);
            for (int idx = lineStart; idx < lineStop; ++idx) {
                if (idx != lineNum) {
                    header.append(String.format("    |%s\n", lines.get(idx)));
                } else {
                    header.append(String.format("  > |%s\n", lines.get(idx)));
                }
            }
            header.append(String.format("  at %s(%d)\n", sourceFile, lineNum + 1));
        }
        System.out.println(header);
    }
}
