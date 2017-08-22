package exceptions.pushservices;

import java.util.concurrent.CompletionException;

/**
 * Thrown on a hard, breaking which will fail a message and all recipients
 */
public class PlatformEndpointException extends CompletionException {
    public int mStatusCode = 0;
    public String mErrorMessage = "";

    public PlatformEndpointException(int statusCode, String errorMessage) {
        super(String.format("GCM Endpoint returned a failing %d status code.", statusCode));
        mStatusCode = statusCode;
        mErrorMessage = errorMessage;
    }

    private PlatformEndpointException() {
    }
}
