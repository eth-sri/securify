package ch.securify;

import junit.framework.TestCase;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.TreeMap;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import static ch.securify.Main.processSolidityFile;

public class DecompilationBugTest {

    @Test
    public void testFirstExample() throws IOException, InterruptedException {
        processWithoutException("src/test/resources/solidity/decompile.sol");
    }

    private void processWithoutException(String filesol) throws IOException, InterruptedException {
        File tmpFile = File.createTempFile("solc-0.5", "");
        String tmpPath = tmpFile.getPath();
        URL website = new URL("https://github.com/ethereum/solidity/releases/download/v0.5.0/solc-static-linux");
        try (InputStream in = website.openStream()) {
            Files.copy(in, tmpFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        File f = new File(tmpPath);
        f.setExecutable(true, true);
        String livestatusfile = File.createTempFile("securify_livestatusfile", "").getPath();
        TreeMap<String, SolidityResult> output = processSolidityFile(tmpPath, filesol, livestatusfile);
        output.values().forEach(s -> TestCase.assertTrue(s.securifyErrors.isEmpty()));
    }
}
