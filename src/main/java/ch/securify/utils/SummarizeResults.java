/*
 *  Copyright 2018 Secure, Reliable, and Intelligent Systems Lab, ETH Zurich
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


package ch.securify.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

import ch.securify.model.ContractResult;

public class SummarizeResults {

    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static Map<String, Integer> totalNumViolations = new HashMap<>();
    private static Map<String, Integer> totalNumWarnings = new HashMap<>();
    private static Map<String, Integer> totalNumCompliant = new HashMap<>();

    private static Map<String, Integer> numContractsWithViolation = new HashMap<>();
    private static Map<String, Integer> numContractsCompliant = new HashMap<>();

    private static String percentage(int n1, int n2) {
        return String.format("%3.1f", ((float) n1) * 100 / n2);
    }

    private static void printStatistics() {


        for (String pattern : totalNumViolations.keySet()) {

            int contracts_safe = numContractsCompliant.getOrDefault(pattern, 0);
            int contracts_violation = numContractsWithViolation.getOrDefault(pattern, 0);

            int instr_safe = totalNumCompliant.getOrDefault(pattern, 0);
            int instr_violation = totalNumViolations.getOrDefault(pattern, 0);
            int instr_warnings = totalNumWarnings.getOrDefault(pattern, 0);

            System.out.format("%40s: %6s  %6s  %6s  \n",
                    "Instructions", "Safe", "Viol", "Warn");
            System.out.format("%40s: %6d  %6d  %6d  \n",
                    pattern,
                    instr_safe,
                    instr_violation,
                    instr_warnings);

            System.out.format("%40s: %6s  %6s   \n",
                    "Contracts", "Safe", "Viol");
            System.out.format("%40s: %6d %6d \n",
                    pattern,
                    contracts_safe,
                    contracts_violation);

            System.out.println();

        }
    }

    private static void parseJSON(String pathToJSON) throws FileNotFoundException {

        /* Read JSON file */
        File fileJSON = new File(pathToJSON);
        FileReader fr = new FileReader(fileJSON);
        JsonReader jr = new JsonReader(fr);
        JsonArray results = null;
        try {
            results = gson.fromJson(jr, JsonArray.class);
        } catch (com.google.gson.JsonSyntaxException e) {
            System.out.println("JSON exception");
            return;
        } finally {
            if (results == null) {
                return;
            }
        }

        /* Count contracts */
        for (int i = 0; i < results.size(); i++) {

            JsonElement je = results.get(i);
            JsonObject res = gson.fromJson(je, JsonObject.class);

            String address = gson.fromJson(res.get("address"), String.class);
            String status = gson.fromJson(res.get("status"), String.class);

            if (!status.equals("success")) {
                /* ? */
                System.out.println(status);
            } else {
                ContractResult result = gson.fromJson(res.get("result"), ContractResult.class);
                if (result == null || result.patternResults == null) {
                    continue;
                }

                /* Iterate over all the pattern names */
                for (String key : result.patternResults.keySet()) {

                    /* Update total number of violations, compliances and warnings */
                    int old = totalNumViolations.getOrDefault(key, 0);
                    int n = (new HashSet<>(result.patternResults.get(key).violations)).size();
                    totalNumViolations.put(key, old + n);

                    old = totalNumWarnings.getOrDefault(key, 0);
                    n = (new HashSet<>(result.patternResults.get(key).warnings)).size();
                    totalNumWarnings.put(key, old + n);

                    old = totalNumCompliant.getOrDefault(key, 0);
                    n = (new HashSet<>(result.patternResults.get(key).safe)).size();
                    totalNumCompliant.put(key, old + n);

                    // The contract contains at least one violation
                    incValueIfTrue(result.patternResults.get(key).hasViolations, numContractsWithViolation, key);

                    incValueIfTrue(!result.patternResults.get(key).hasViolations &&
                                        !result.patternResults.get(key).hasWarnings,
                                    numContractsCompliant, key);
                }
            }
        }
    }

    private static void incValueIfTrue(boolean flag, Map<String, Integer> map, String key) {
        if (flag) {
            int old = map.getOrDefault(key, 0);
            map.put(key, old + 1);
        }

    }

    /**
     * @param args
     *            List of JSON files. Each file contains the output of the
     *            analyser on a list of contracts.
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        for (String path : args) {
            parseJSON(path);
        }
        printStatistics();
    }
}
