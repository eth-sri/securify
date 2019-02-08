package ch.securify;

import ch.securify.analysis.SecurifyErrors;
import ch.securify.utils.Hex;
import com.google.common.base.CharMatcher;
import com.google.gson.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.reverse;

class SmallPatternResult {
    String name;
    TreeSet<Integer> violations;
    TreeSet<Integer> warnings;
    TreeSet<Integer> safe;
    TreeSet<Integer> conflicts;

    SmallPatternResult(TreeSet<Integer> violations, TreeSet<Integer> warnings, TreeSet<Integer> safe, TreeSet<Integer> conflicts) {
        this.violations = violations;
        this.warnings = warnings;
        this.safe = safe;
        this.conflicts = conflicts;
    }
}

class MappingNotFoundException extends RuntimeException {
    RuntimeException baseException;
    int bytecodeOffset;

    public MappingNotFoundException(){
    }

    public MappingNotFoundException(RuntimeException e, int bytecodeOffset) {
        this.baseException = e;
        this.bytecodeOffset = bytecodeOffset;
    }
}

public class CompilationHelpers {
    public static String sanitizeLibraries(String hexCode) {
        final String dummyAddress = "1000000000000000000000000000000000000010";
        StringBuilder sanitized = new StringBuilder();
        for (int i = 0; i < hexCode.length(); i++) {
            if (hexCode.charAt(i) == '_') {
                sanitized.append(dummyAddress);
                i += dummyAddress.length() - 1;
            } else {
                sanitized.append(hexCode.charAt(i));
            }
        }
        return sanitized.toString();
    }

    public static byte[] extractBinaryFromHexFile(String filehex) throws IOException {
        File contractBinHexFile = new File(filehex);
        String hexCode = Files.readAllLines(contractBinHexFile.toPath()).get(0);
        return Hex.decode(sanitizeLibraries(hexCode));
    }

    static int bytecodeOffsetToSourceOffset(int bytecodeOffset, List<String[]> map) throws MappingNotFoundException {
        try {
            map = map.subList(0, bytecodeOffset);
        } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
            throw new MappingNotFoundException(e, bytecodeOffset);
        }

        reverse(map);
        for (String[] offset : map) {
            if (!offset[0].equals("")) {
                int res = Integer.parseInt(offset[0]);
                if (res < 0) {
                   throw new MappingNotFoundException();
                }
                return res;
            }
        }
        throw new MappingNotFoundException();
    }

    static List<String[]> explodeMappingString(String map) {
        ArrayList<String[]> mapping = new ArrayList<>();
        for (String s : map.split(";")) {
            mapping.add(s.split(":"));
        }
        return mapping;
    }

    private static String readFile(String path) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, Charset.defaultCharset());
    }

    static JsonObject compileContracts(String solc, String filesol) throws IOException, InterruptedException, RuntimeException {
        ProcessBuilder p = new ProcessBuilder(solc, "--combined-json", "abi,ast,bin-runtime,srcmap-runtime", filesol);

        File f = File.createTempFile("securify_compilation_", ".json");
        f.deleteOnExit();

        File fErr = File.createTempFile("securify_compilation_error", ".log");
        fErr.deleteOnExit();

        final Process process = p.redirectOutput(f).redirectError(fErr).start();
        process.waitFor();

        int exitValue = process.exitValue();
        if(exitValue != 0){
            System.err.print(readFile(fErr.getPath()));
            throw new RuntimeException();
        }


        JsonObject jsonObject = new JsonParser().parse(readFile(f.getPath())).getAsJsonObject();

        return jsonObject.get("contracts").getAsJsonObject();
    }

    static JsonObject parseCompilationOutput(String compilationOutputFile) throws IOException {
        return new JsonParser().parse(readFile(compilationOutputFile)).getAsJsonObject();
    }

    private static TreeSet<Integer> getMatchedLines(byte[] contract, JsonArray matches, String map, SecurifyErrors securifyErrors) throws MappingNotFoundException {
        TreeSet<Integer> matchedLines = new TreeSet<>();
        for (JsonElement m : matches) {
            int byteOffset = m.getAsInt();
            int line;
            try {
                int srcOffset = CompilationHelpers.bytecodeOffsetToSourceOffset(byteOffset, CompilationHelpers.explodeMappingString(map));
                String matchingSubstring = new String(Arrays.copyOfRange(contract, 0, srcOffset), UTF_8);
                line = CharMatcher.is('\n').countIn(matchingSubstring);
            } catch (MappingNotFoundException e) {
                line = -1;
                securifyErrors.add("mapping_error", e);
            }
            matchedLines.add(line);
        }
        return matchedLines;
    }

    static SolidityResult getMappingsFromStatusFile(String livestatusfile, String map, byte[] contract) throws IOException {
        JsonObject jsonObject = new JsonParser().parse(readFile(livestatusfile)).getAsJsonObject();
        Set<Map.Entry<String, JsonElement>> results = jsonObject.get("patternResults").getAsJsonObject().entrySet();

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        SecurifyErrors securifyErrors = gson.fromJson(jsonObject.get("securifyErrors"), SecurifyErrors.class);
        SolidityResult allResults = new SolidityResult(securifyErrors);

        for (Map.Entry<String, JsonElement> e : results) {
            JsonArray violations = e.getValue().getAsJsonObject().get("violations").getAsJsonArray();
            JsonArray safe = e.getValue().getAsJsonObject().get("safe").getAsJsonArray();
            JsonArray warnings = e.getValue().getAsJsonObject().get("warnings").getAsJsonArray();
            JsonArray conflicts = e.getValue().getAsJsonObject().get("conflicts").getAsJsonArray();

            SmallPatternResult pResults = new SmallPatternResult(
                    getMatchedLines(contract, violations, map, allResults.securifyErrors),
                    getMatchedLines(contract, warnings, map, allResults.securifyErrors),
                    getMatchedLines(contract, safe, map, allResults.securifyErrors),
                    getMatchedLines(contract, conflicts, map, allResults.securifyErrors));

            allResults.results.put(e.getKey(), pResults);
        }

        return allResults;
    }
}
