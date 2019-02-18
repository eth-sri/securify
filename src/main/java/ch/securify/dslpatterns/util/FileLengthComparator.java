package ch.securify.dslpatterns.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
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

        ArrayList<String> firstLines = new ArrayList<String>();
        while (in.hasNextLine()) {
            in.nextLine();
            nLines++;
        }
        in.close();

        return nLines;
    }

    private static void compareAndPrint(String fileName1, String fileName2) {
        System.out.println(fileName1 + " and " + fileName2);
        int nLines1 = readNumberOfLines(fileName1);
        int nLines2 = readNumberOfLines(fileName2);
        if(nLines1 == nLines2)
            System.out.println("EQUAL line numbers, " + nLines1);
        else
            System.out.println("DIFFERENT line numbers, " + nLines1 + " vs " + nLines2);
    }

    public static void main(String[] args) {

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

            System.out.println(DSLFilename + " " + noDSLMustFilename + " " + noDSLMayFilename);

            //mustExplicit part
            compareAndPrint(DSLFilename + "/mustPrecede.csv", noDSLMustFilename + "/mustPrecede.csv");
            compareAndPrint(DSLFilename + "/mustDepOn.csv", noDSLMustFilename + "/reach.csv");
            compareAndPrint(DSLFilename + "/sstoreMust.csv", noDSLMustFilename + "/storage.csv");
            compareAndPrint(DSLFilename + "/mstoreMust.csv", noDSLMustFilename + "/memory.csv");

            //mayImplicit part
            compareAndPrint(DSLFilename + "/mayFollow.csv", noDSLMayFilename + "/isAfter.csv");
            compareAndPrint(DSLFilename + "/mayDepOn.csv", noDSLMayFilename + "/reach.csv");
            compareAndPrint(DSLFilename + "/instrMayDepOn.csv", noDSLMayFilename + "/reachInstr.csv");
            compareAndPrint(DSLFilename + "/sstoreMay.csv", noDSLMayFilename + "/storage.csv");
            compareAndPrint(DSLFilename + "/mstoreMay.csv", noDSLMayFilename + "/memory.csv");
        }

        return;
    }
}
