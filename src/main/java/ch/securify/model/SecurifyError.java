package ch.securify.model;

import java.io.PrintWriter;
import java.io.StringWriter;

public class SecurifyError{
    public String error;
    public String stackTrace;

    public SecurifyError(String error, Exception e){
        this.error = error;
        this.stackTrace = exceptionToString(e);
    }

    private static String exceptionToString(Exception e){
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}
