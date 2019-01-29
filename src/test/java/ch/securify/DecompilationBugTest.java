package ch.securify;

import junit.framework.TestCase;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.TreeMap;

import static ch.securify.Main.processSolidityFile;

public class DecompilationBugTest {

    @Test
    public void testFirstExample() throws IOException, InterruptedException {
        processWithoutException("src/test/resources/solidity/decompile.sol");
    }

    private void processWithoutException(String filesol) throws IOException, InterruptedException {
        String livestatusfile = File.createTempFile("securify_livestatusfile", "").getPath();
        TreeMap<String, SolidityResult> output = processSolidityFile("solc", filesol, livestatusfile);
        output.values().forEach(s -> TestCase.assertTrue(s.securifyErrors.isEmpty()));
    }
}
