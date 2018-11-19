package ch.securify;

import java.util.TreeMap;

class SolidityResult {
    final TreeMap<String, SmallPatternResult> results;

    SolidityResult() {
        results = new TreeMap<>();
    }
}
