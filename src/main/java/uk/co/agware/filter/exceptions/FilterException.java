package uk.co.agware.filter.exceptions;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 8/06/2016.
 *
 * An unchecked exception for when things go REALLY wrong
 */
public class FilterException extends RuntimeException {

    public FilterException() {
    }

    public FilterException(String message) {
        super(message);
    }

    public FilterException(String message, Throwable cause) {
        super(message, cause);
    }

    public FilterException(Throwable cause) {
        super(cause);
    }

    public FilterException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
