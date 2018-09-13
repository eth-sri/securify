/*
 *  Copyright 2018 Secure, Reliable, and Intelligent Systems Lab, ETH Zurich
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */


package ch.securify.model;

import java.util.ArrayList;
import java.util.List;

public class PatternResult {
    public boolean completed = false;
    public String error = null;

    public boolean hasViolations;
    public boolean hasWarnings;
    public boolean hasSafe;
    public boolean hasConflicts;

    public List<Integer> violations = new ArrayList<>();
    public List<Integer> warnings = new ArrayList<>();
    public List<Integer> safe = new ArrayList<>();
    public List<Integer> conflicts = new ArrayList<>();

    public void addViolation(Integer id) {
        violations.add(id);
        hasViolations = true;
    }

    public void addWarning(Integer id) {
        warnings.add(id);
        hasWarnings = true;
    }

    public void addSafe(Integer id) {
        safe.add(id);
        hasSafe = true;
    }

    public void addConflict(Integer id) {
        conflicts.add(id);
        hasConflicts = true;
    }
}
