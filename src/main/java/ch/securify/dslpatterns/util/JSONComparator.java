package ch.securify.dslpatterns.util;

import ch.securify.model.ContractResult;
import ch.securify.utils.Pair;
import com.google.gson.*;
import com.google.gson.stream.JsonReader;

import java.io.*;
import java.util.*;

public class JSONComparator {

    private class SingleResult {
        List<Integer> violations = new ArrayList<>();
        List<Integer> warnings = new ArrayList<>();
        List<Integer> safe = new ArrayList<>();
        List<Integer> conflicts = new ArrayList<>();


        @Override
        public boolean equals(Object o) {
            // self check
            if (this == o)
                return true;
            // null check
            if (o == null)
                return false;
            // type check and cast
            if (getClass() != o.getClass())
                return false;
            SingleResult other = (SingleResult) o;
            // field comparison
            return (violations.equals(other.violations)) &&
                    (warnings.equals(other.warnings)) &&
                    (safe.equals(other.safe)) &&
                    (conflicts.equals(other.conflicts));
        }

        @Override
        public String toString(){
            StringBuilder sb = new StringBuilder();

            sb.append("violations: ");
            sb.append(violations);
            sb.append(" warnings: ");
            sb.append(warnings);
            sb.append(" safe: ");
            sb.append(safe);
            sb.append(" conflicts: ");
            sb.append(conflicts);

            return sb.toString();
        }
    }

    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) throws IOException {
        Map<String, Map<String, Map<String, SingleResult>>> resultSet = new HashMap<>();


        for (String arg : args) {
            System.out.println(arg);
        }
        File dir = new File(args[0]);

        File[] jsons = dir.listFiles((dir1, filename) -> filename.endsWith(".json"));

        for(File json : jsons) {
            String filename = json.getName();
            if(filename.charAt(0) == '.')
                continue;
            System.out.println(filename);
            System.out.println(json.getAbsolutePath());
            resultSet.put(filename, parseJSON(json.getAbsolutePath()));
        }

        Set<String> done = new HashSet<>();
        for(Map.Entry<String, Map<String, Map<String, SingleResult>>> res : resultSet.entrySet()) {
            String fileName = res.getKey();
            if(done.contains(fileName))
                continue;

            done.add(fileName);
            Map<String, Map<String, SingleResult>> noDSLResults;
            Map<String, Map<String, SingleResult>> DSLResults;
            if(fileName.contains("NODSL")) {
                System.out.println("****" + fileName.replace("NODSL.json", ".sol"));
                noDSLResults = res.getValue();
                String DSLFilename = fileName.replace("NODSL", "DSL");
                DSLResults = resultSet.get(DSLFilename);
                done.add(DSLFilename);
            }
            else {
                System.out.println("****" + fileName.replace("DSL.json", ".sol"));
                DSLResults = res.getValue();
                String noDSLFilename = fileName.replace("DSL", "NODSL");
                noDSLResults = resultSet.get(noDSLFilename);
                done.add(noDSLFilename);
            }

            compareAndPrint(noDSLResults, DSLResults);
        }

        System.out.println(resultSet);
    }

    private static void compareAndPrint(Map<String, Map<String, SingleResult>> noDSLResults,
                                        Map<String, Map<String, SingleResult>> DSLResults) {
        //NoWritesAfterCallDAO , unRestrictedWrite , unRestrictedWriteUP , unRestrictedTransferEtherFlow ,
        // unHandledException , TOD , TODIIAmount , ValidatedArgumentsMissingInputValidation

        for(Map.Entry<String, Map<String, SingleResult>> entry : noDSLResults.entrySet()) {
            String contractName = entry.getKey();
            System.out.println(contractName);
            Map<String, SingleResult> noDSLContractResults = entry.getValue();
            Map<String, SingleResult> DSLContractResults = DSLResults.get(contractName);

            //NoWritesAfterCallDAO
            SingleResult DAO = noDSLContractResults.get("DAO");
            SingleResult NoWritesAfterCall = DSLContractResults.get("NoWritesAfterCallDAO");
            if(DAO == null || NoWritesAfterCall == null)
                continue;
            System.out.println("NoWritesAfterCall / DAO: ");
            compareAndPrintSingleResult(DAO, NoWritesAfterCall);


            //unRestrictedWriteUP
            SingleResult UnrestrictedWrite = noDSLContractResults.get("UnrestrictedWrite");
            SingleResult unRestrictedWriteUP = DSLContractResults.get("unRestrictedWriteUP");
            System.out.println("unRestrictedWrite");
            compareAndPrintSingleResult(UnrestrictedWrite, unRestrictedWriteUP);

            //unRestrictedTransferEtherFlow
            SingleResult UnrestrictedEtherFlow = noDSLContractResults.get("UnrestrictedEtherFlow");
            SingleResult unRestrictedTransferEtherFlow = DSLContractResults.get("unRestrictedTransferEtherFlow");
            System.out.println("unRestrictedTransferEtherFlow");
            compareAndPrintSingleResult(UnrestrictedEtherFlow, unRestrictedTransferEtherFlow);

            //unHandledException
            SingleResult UnhandledException = noDSLContractResults.get("UnhandledException");
            SingleResult unHandledException = DSLContractResults.get("unHandledException");
            System.out.println("unHandledException");
            compareAndPrintSingleResult(UnhandledException, unHandledException);

            //unHandledException
            SingleResult TODAmount = noDSLContractResults.get("TODAmount");
            SingleResult TODIIAmount = DSLContractResults.get("TODIIAmount");
            System.out.println("TODAmount");
            compareAndPrintSingleResult(TODAmount, TODIIAmount);

            //todo: validated arguments missing input validation
        }
    }

    private static void compareAndPrintSingleResult(SingleResult noDSL, SingleResult dsl) {
        if(noDSL.equals(dsl)) {
            System.out.println("No differences");
        }
        else {
            System.out.print("NODSL: ");
            System.out.println(noDSL);

            System.out.print("DSL:   ");
            System.out.println(dsl);
        }
    }

    private static Map<String, Map<String, SingleResult>> parseJSON(String pathToJSON) throws FileNotFoundException {
        Map<String, Map<String, SingleResult>> tmp = new HashMap<>(1);

        File fileJSON = new File(pathToJSON);
        FileReader fr = new FileReader(fileJSON);
        JsonReader jr = new JsonReader(fr);

        JsonObject bigObj = gson.fromJson(jr, JsonObject.class);
        System.out.println(bigObj.entrySet());
        Set<Map.Entry<String, JsonElement>> contractResults = bigObj.entrySet();

        for(Map.Entry<String, JsonElement> entry : contractResults) {
            Set<Map.Entry<String, JsonElement>> singleContractResults = entry.getValue().getAsJsonObject().get("results").getAsJsonObject().entrySet();
            System.out.println(singleContractResults);

            Map<String, SingleResult> map1 = new HashMap<>(6);
            for(Map.Entry<String, JsonElement> pattRes : singleContractResults) {
                map1.put(pattRes.getKey(), gson.fromJson(pattRes.getValue(), SingleResult.class));
            }
            tmp.put(entry.getKey(), map1);
                   // gson.fromJson(jr, JsonArray.class);
        }

        /*JsonArray results = gson.fromJson(jr, JsonArray.class);
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
        }*/

        return tmp;
    }
}
