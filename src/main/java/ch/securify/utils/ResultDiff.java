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

import ch.securify.model.ContractResult;
import ch.securify.model.PatternResult;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.gson.*;
import com.google.gson.stream.JsonReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ResultDiff {

    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static Map<String, ContractResult> parseJSON(String pathToJSON) throws FileNotFoundException {
        Map<String, ContractResult> tmp = new HashMap<>();

        File fileJSON = new File(pathToJSON);
        FileReader fr = new FileReader(fileJSON);
        JsonReader jr = new JsonReader(fr);

        JsonArray results = gson.fromJson(jr, JsonArray.class);
        for (int i = 0; i < results.size(); i++) {
            JsonElement je = results.get(i);
            JsonObject res = gson.fromJson(je, JsonObject.class);
            String address = gson.fromJson(res.get("address"), String.class);
            Boolean status = gson.fromJson(res.get("status"), Boolean.class);
            if (status) {
                tmp.put(address, null);
            } else {
                ContractResult result = gson.fromJson(res.get("result"), ContractResult.class);
                tmp.put(address, result);
            }
        }

        return tmp;
    }

    private static Map<String, Pair<ContractResult, ContractResult>> diffResults(Map<String, ContractResult> oldResults, Map<String, ContractResult> newResults) {
        Map<String, Pair<ContractResult, ContractResult>> diff = new HashMap<>();

        assert(newResults.keySet().equals(oldResults.keySet()));

        for (String addr : newResults.keySet()) {
            if (oldResults.get(addr) == null)
                continue;
            assert(newResults.get(addr) != null);

            ContractResult oldResult = oldResults.get(addr);
            ContractResult newResult = newResults.get(addr);

			if (oldResult == null || newResult == null) continue;

            if (oldResult.decompiled != newResult.decompiled
                    || oldResult.finished != newResult.finished
                    || (oldResult.error != null && !oldResult.error.equals(newResult.error))) {
                diff.put(addr, new Pair<>(oldResult, newResult));
                continue;
            }

            if (diff.keySet().contains(addr))
                continue;

            // Check whether the results differ in any of the patterns
            for (String pattern : oldResult.patternResults.keySet()) {
                if (!oldResult.patternResults.containsKey(pattern) || !newResult.patternResults.containsKey(pattern)) {
                    continue;
                }
                PatternResult oldPatternResult = oldResult.patternResults.get(pattern);
                PatternResult newPatternResult = newResult.patternResults.get(pattern);
                if (oldPatternResult.completed != newPatternResult.completed
                        || (oldPatternResult.error != null && newPatternResult.error != null && !oldPatternResult.error.equals(newPatternResult.error))
                        || oldPatternResult.hasViolations != newPatternResult.hasViolations) {
                    diff.put(addr, new Pair<>(oldResult, newResult));
                    break;

                }
            }

        }
        return diff;
    }

    private static void printDiff(String oldResultsPath, String newResultsPath) throws FileNotFoundException {
        Map<String, ContractResult> oldResults = parseJSON(oldResultsPath);
        Map<String, ContractResult> newResults = parseJSON(newResultsPath);

        Map<String, Pair<ContractResult, ContractResult>> diff = diffResults(oldResults, newResults);

        for (String addr: diff.keySet()) {
            System.out.println("\nContract: " + addr);
            ContractResult oldResult = oldResults.get(addr);
            ContractResult newResult = newResults.get(addr);

            if (oldResult.decompiled != newResult.decompiled)
                System.out.println("Methods decompiled: " + oldResult.decompiled + " <> " + newResult.decompiled);
            if (oldResult.finished != newResult.decompiled)
                System.out.println("Finished: " + oldResult.finished + " <> " + newResult.finished);
            if (oldResult.error != null && !oldResult.error.equals(newResult.error))
                System.out.println("Error: " + oldResult.error + " <> " + newResult.error);


            System.out.println("Pattern results:");
            for (String pattern : oldResult.patternResults.keySet()) {
                PatternResult oldPatternResult = oldResult.patternResults.get(pattern);
                PatternResult newPatternResult = newResult.patternResults.get(pattern);
                if (oldPatternResult.completed != newPatternResult.completed
                        || oldPatternResult.error != null && newPatternResult.error != null && !oldPatternResult.error.equals(newPatternResult.error)
                        || oldPatternResult.hasViolations != newPatternResult.hasViolations)
                    System.out.println("\t" + pattern);
                if (oldPatternResult.completed != newPatternResult.completed)
                    System.out.println("\t\tCompleted: " + oldPatternResult.completed + " <> " + newPatternResult.completed);
                if (oldPatternResult.error != null && !oldPatternResult.error.equals(newPatternResult.error))
                    System.out.println("\t\tError: " + oldPatternResult.error + " <> " + newPatternResult.error);
                if (oldPatternResult.hasViolations != newPatternResult.hasViolations)
                    System.out.println("\t\tMatched: " + oldPatternResult.hasViolations + " <> " + newPatternResult.hasViolations);
            }
        }

        System.out.println("\nDifferences: " + diff.keySet().size() + " out of " + oldResults.keySet().size());
    }

    private static class Args {
        @Parameter(names = {"--oldResults"}, description = "Old results in JSON format")
        private String oldResults;

        @Parameter(names = {"--newResults"}, description = "New results in JSON format")
        private String newResults;
    }

    public static void main(String[] rawrgs) throws IOException {
        ResultDiff.Args args = new ResultDiff.Args();
        try {
            new JCommander(args, rawrgs);
        }
        catch (ParameterException e) {
            System.out.println(e.getMessage());
            new JCommander(args).usage();
            return;
        }

        if (args.oldResults != null && args.newResults != null) {
            printDiff(args.oldResults, args.newResults);
        } else {
            new JCommander(args).usage();
        }
    }
}
