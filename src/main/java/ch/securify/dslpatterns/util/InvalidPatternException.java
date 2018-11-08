package ch.securify.dslpatterns.util;

import java.io.IOException;

public class InvalidPatternException extends IOException {

    public InvalidPatternException(String message) {
        super(message);
    }
}
