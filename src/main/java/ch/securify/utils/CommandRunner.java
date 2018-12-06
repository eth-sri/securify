package ch.securify.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class CommandRunner {

    private static boolean DEBUG = true;

    public static String runCommand(String command) throws IOException, InterruptedException {
        Process proc;
        String result = "";
        log("CMD: " + command);

        // Souffle works with this PATH
        String[] envp = {"PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/home/dani/bin"};
        proc = Runtime.getRuntime().exec(command, envp);

        // Read the output
        BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));

        // Output of the command
        {
            String line;
            while ((line = reader.readLine()) != null) {
                result = result + line + "\n";
                log(line);
            }
        }

        // Display the errors
        {
            String line;
            reader = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

            while ((line = reader.readLine()) != null) {
                result = result + line + "\n";
                log(line);
            }
        }

        proc.waitFor();
        if (proc.exitValue() != 0) {
            throw new IOException();
        }
        return result;
    }

    protected static void log(String msg) {
        if (DEBUG)
            System.out.println("CommandRunner: " + msg);
    }
}
