package ch.securify;

import com.google.common.base.CharMatcher;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.omg.CosNaming.NamingContextPackage.NotFound;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.IOException;
import java.lang.RuntimeException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.reverse;

class SmallPatternResult {
    private String name;
    private LinkedHashSet<Integer> violations;
    private LinkedHashSet<Integer> warnings;
    private LinkedHashSet<Integer> safe;
    private LinkedHashSet<Integer> conflicts;

    SmallPatternResult(LinkedHashSet<Integer> violations, LinkedHashSet<Integer> warnings, LinkedHashSet<Integer> safe, LinkedHashSet<Integer> conflicts) {
        this.violations = violations;
        this.warnings = warnings;
        this.safe = safe;
        this.conflicts = conflicts;
    }
}

public class CompilationHelpers {
    public static String sanitizeLibraries(String hexCode) {
        final String dummyAddress = "1000000000000000000000000000000000000010";
        String sanitized = "";
        for (int i = 0; i < hexCode.length(); i++) {
            if (hexCode.charAt(i) == '_') {
                sanitized += dummyAddress;
                i += dummyAddress.length() - 1;
            } else {
                sanitized += hexCode.charAt(i);
            }
        }
        return sanitized;
    }

    public static byte[] extractBinaryFromHexFile(String filehex) throws IOException {
        File contractBinHexFile = new File(filehex);
        String hexCode = Files.readAllLines(contractBinHexFile.toPath()).get(0);
        return DatatypeConverter.parseHexBinary(sanitizeLibraries(hexCode));
    }

    static int bytecodeOffsetToSourceOffset(int bytecodeOffset, List<String[]> map) throws NotFound {
        try {
            map = map.subList(0, bytecodeOffset);
        } catch (IndexOutOfBoundsException e) {
            throw new NotFound();
        }

        reverse(map);
        for (String[] offset : map) {
            if (!offset[0].equals("")) {
                return Integer.parseInt(offset[0]);
            }
        }
        throw new NotFound();
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

    static JsonObject compileContracts(String filesol) throws IOException, InterruptedException, RuntimeException {
        ProcessBuilder p = new ProcessBuilder("solc",
//        "--optimize",
        "--combined-json", "abi,ast,bin-runtime,srcmap-runtime", filesol);

        File f = File.createTempFile("securify_compilation_", ".json");
        f.deleteOnExit();

        final Process process = p.redirectOutput(f).redirectError(ProcessBuilder.Redirect.INHERIT).start();

        process.waitFor();
        int exitValue = process.exitValue();
        if(exitValue != 0){
            throw new RuntimeException();
        }

        JsonObject jsonObject = new JsonParser().parse(readFile(f.getPath())).getAsJsonObject();

        return jsonObject.get("contracts").getAsJsonObject();
    }

    private static LinkedHashSet<Integer> getMatchedLines(byte[] contract, JsonArray matches, String map) throws NotFound {
        LinkedHashSet<Integer> matchedLines = new LinkedHashSet<>();
        for (JsonElement m : matches) {
            int byteOffset = m.getAsInt();
            int line;
            try {
                int srcOffset = CompilationHelpers.bytecodeOffsetToSourceOffset(byteOffset, CompilationHelpers.explodeMappingString(map));
                String matchingSubstring = new String(Arrays.copyOfRange(contract, 0, srcOffset), UTF_8);
                line = CharMatcher.is('\n').countIn(matchingSubstring);
            } catch (NotFound e) {
                line = -1;
            }
            matchedLines.add(line);
        }
        return matchedLines;
    }

    static SolidityResult getMappingsFromStatusFile(String livestatusfile, String map, byte[] contract) throws IOException, NotFound {
        JsonObject jsonObject = new JsonParser().parse(readFile(livestatusfile)).getAsJsonObject();
        Set<Map.Entry<String, JsonElement>> results = jsonObject.get("patternResults").getAsJsonObject().entrySet();
        SolidityResult allResults = new SolidityResult();

        for (Map.Entry<String, JsonElement> e : results) {
            JsonArray violations = e.getValue().getAsJsonObject().get("violations").getAsJsonArray();
            JsonArray safe = e.getValue().getAsJsonObject().get("safe").getAsJsonArray();
            JsonArray warnings = e.getValue().getAsJsonObject().get("warnings").getAsJsonArray();
            JsonArray conflicts = e.getValue().getAsJsonObject().get("conflicts").getAsJsonArray();

            SmallPatternResult pResults = new SmallPatternResult(
                    getMatchedLines(contract, violations, map),
                    getMatchedLines(contract, safe, map),
                    getMatchedLines(contract, warnings, map),
                    getMatchedLines(contract, conflicts, map));

            allResults.results.put(e.getKey(), pResults);

        }
        return allResults;
    }
}
