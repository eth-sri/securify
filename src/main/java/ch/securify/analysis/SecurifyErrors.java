/*
 *  Copyright 2018 ChainSecurity AG
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
package ch.securify.analysis;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;

public class SecurifyErrors {

    class Error{
        String error;
        String stackTrace;

        Error(String error, Exception e){
            this.error = error;
            this.stackTrace = exceptionToString(e);
        }

        private String exceptionToString(Exception e){
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            return sw.toString();
        }
    }

    public boolean isEmpty() {
        return errors.isEmpty();
    }

    public void add(String error, Exception e){
        errors.add(new Error(error, e));
    }

    private LinkedList<Error> errors = new LinkedList<>();

}
