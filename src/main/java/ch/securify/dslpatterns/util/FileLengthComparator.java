package ch.securify.dslpatterns.util;

import ch.securify.Main;
import ch.securify.analysis.AbstractDataflow;
import ch.securify.analysis.DSLAnalysis;
import ch.securify.utils.CommandRunner;
import ch.securify.utils.FileUtil;

import java.io.*;
import java.util.*;

public class FileLengthComparator {

    private static int readNumberOfLines(String fileName) {
        Scanner in = null;
        int nLines = 0;
        try {
            in = new Scanner(new FileReader(fileName));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return 0;
        }

        while (in.hasNextLine()) {
            in.nextLine();
            nLines++;
        }
        in.close();

        return nLines;
    }

    private static void compareAndPrint(String fileName1, String fileName2) {

        int nLines1 = readNumberOfLines(fileName1);
        int nLines2 = readNumberOfLines(fileName2);
        if(nLines1 != nLines2) {
            System.out.println(fileName1 + " and " + fileName2);
            System.out.println("DIFFERENT line numbers, " + nLines1 + " vs " + nLines2);
        }
        /*else
            System.out.println("EQUAL line numbers, " + nLines1);*/
    }

    private static void compareOutput() {

        File dir = new File(".");
//        File [] dirsNoDSL = dir.listFiles((dir1, name) -> name.startsWith("outNoDSL"));
        File [] dirsDSL = dir.listFiles((dir1, name) -> name.startsWith("outDSL"));

/*        Map<String, File> dirsNoDSLMap = new LinkedHashMap<>(dirsNoDSL.length);
        Arrays.stream(dirsNoDSL).forEach((file) -> dirsNoDSLMap.put(file.getName(), file));*/

        for(int i = 0; i < dirsDSL.length; ++i) {
            String DSLFilename = dirsDSL[i].getName();
            int fileIndex = Integer.parseInt(DSLFilename.substring(6));
            String noDSLMustFilename = "outNoDSL" + (fileIndex*2);
            String noDSLMayFilename = "outNoDSL" + (fileIndex*2 +1);

            //System.out.println(DSLFilename + " " + noDSLMustFilename + " " + noDSLMayFilename);

            //mustExplicit part
            compareAndPrint(DSLFilename + "/mustPrecede.csv", noDSLMustFilename + "/mustPrecede.csv");
            compareAndPrint(DSLFilename + "/mustDepOn.csv", noDSLMustFilename + "/reach.csv");
            compareAndPrint(DSLFilename + "/sstoreMust.csv", noDSLMustFilename + "/storage.csv");
            compareAndPrint(DSLFilename + "/mstoreMust.csv", noDSLMustFilename + "/memory.csv");
            compareAndPrint(DSLFilename + "/mustAssignType.csv", noDSLMustFilename + "/assignTypeDebug.csv");
            compareAndPrint(DSLFilename + "/assignVarMustDebug.csv", noDSLMustFilename + "/assignVarDebug.csv");

            //mayImplicit part
            compareAndPrint(DSLFilename + "/mayFollow.csv", noDSLMayFilename + "/isAfter.csv");
            compareAndPrint(DSLFilename + "/mayDepOn.csv", noDSLMayFilename + "/reach.csv");
            compareAndPrint(DSLFilename + "/instrMayDepOn.csv", noDSLMayFilename + "/reachInstr.csv");
            compareAndPrint(DSLFilename + "/sstoreMay.csv", noDSLMayFilename + "/storage.csv");
            compareAndPrint(DSLFilename + "/mstoreMay.csv", noDSLMayFilename + "/memory.csv");
            compareAndPrint(DSLFilename + "/mayAssignType.csv", noDSLMayFilename + "/assignTypeDebug.csv");
            compareAndPrint(DSLFilename + "/assignVarMayImplicitCollapsed.csv", noDSLMayFilename + "/assignVarDebug.csv");
        }

        return;
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        //compareOutput();


        String[] argsDSL = {"-fs", null, "--usedsl"};
        String[] argsNODSL = {"-fs", null};

        File dir = new File("./src/test/resources/solidity");
//        File [] dirsNoDSL = dir.listFiles((dir1, name) -> name.startsWith("outNoDSL"));
        File [] solFiles = dir.listFiles((dir1, name) -> name.endsWith(".sol"));

        //System.out.println(solFiles.length);

        PrintStream original = System.out;
        PrintStream emptyStream = new PrintStream(new OutputStream() {
            public void write(int b) {
                //DO NOTHING
            }
        });

        for(File solFile : solFiles) {
            argsDSL[1] = solFile.getAbsolutePath();
            argsNODSL[1] = solFile.getAbsolutePath();

            System.out.println("**** " + solFile.getName());

            System.setOut(emptyStream);

            Main.main(argsDSL);
            Main.main(argsNODSL);

            DSLAnalysis.resetCounter();
            AbstractDataflow.resetCounter();

            System.setOut(original);
            compareOutput();

            System.out.println(new File(".").getAbsolutePath());
            Arrays.stream(new File(".").listFiles((dir1, name) -> name.startsWith("outDSL"))).forEach((file) ->
            {
                try {
                    CommandRunner.runCommand("rm -rf " + file.getAbsolutePath());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            Arrays.stream(new File(".").listFiles((dir1, name) -> name.startsWith("outNoDSL"))).forEach((file) ->
            {
                try {
                    CommandRunner.runCommand("rm -rf " + file.getAbsolutePath());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            //CommandRunner.runCommand("find -name . 'outDSL*' -exec rm {} \\;");//"rm -rf outDSL*");
            //CommandRunner.runCommand("rm -rf outNoDSL*");
        }

        return;
    }
}
