package ch.securify;

import ch.securify.analysis.SecurifyErrors;
import ch.securify.patterns.AbstractPattern;
import ch.securify.patterns.PatternDescription;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import static java.util.jar.Attributes.Name.IMPLEMENTATION_VERSION;
import static java.util.jar.Attributes.Name.MAIN_CLASS;

class SolidityResult {
    private static String securifyVersion = null;
    public String version;

    private static void setSecurifyVersion(String secVersion) {
        if (securifyVersion != null ) {
            throw new RuntimeException("Securify version is already specified.");
        }
        securifyVersion = secVersion;
    }

    SolidityResult(SecurifyErrors securifyErrors) {
        if (securifyVersion == null ) {
            SolidityResult.setSecurifyVersion(SolidityResult.getVersion());
        }

        this.version = securifyVersion;
        this.securifyErrors = securifyErrors;
    }

    /**
     * Get the Securify version from the MANIFEST
     *
     * @return The version of Securify being executed
     */
    private static String getVersion() {
        String className = Main.class.getCanonicalName();
        try {
            Enumeration<URL> resources = Main.class.getClassLoader()
                    .getResources("META-INF/MANIFEST.MF");
            while (resources.hasMoreElements()) {
                Manifest manifest = new Manifest(resources.nextElement().openStream());
                Attributes at = manifest.getMainAttributes();
                Object main = at.getValue(MAIN_CLASS);
                if (main != null && at.getValue(MAIN_CLASS).equals(className) ) {
                    return at.getValue(IMPLEMENTATION_VERSION);
                }
            }
        } catch (IOException e) {
            System.err.println("Error while setting Securify version");
            e.printStackTrace();
        }
        return "unknown_version";
    }

    /**
     * Add the descriptions of all the patterns
     *
     * @param patterns: all the patterns considered in this Securify version
     */
    static void setPatternDescriptions(List<AbstractPattern> patterns) {
        // only allow initialization once
        patternDescriptions = new LinkedList<>();
        patterns.forEach(pattern -> patternDescriptions.add(pattern.getDescription()));
    }

    public static List<PatternDescription> patternDescriptions;

    TreeMap<String, SmallPatternResult> results = new TreeMap<>();

    SecurifyErrors securifyErrors;
}
