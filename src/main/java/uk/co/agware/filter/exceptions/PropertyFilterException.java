package uk.co.agware.filter.exceptions;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 8/06/2016.
 *
 * A checked exception for when something goes wrong because of user error
 */
public class PropertyFilterException extends Exception {

    public PropertyFilterException() {
    }

    public PropertyFilterException(String message) {
        super(message);
    }

    public PropertyFilterException(String message, Throwable cause) {
        super(message, cause);
    }

    public PropertyFilterException(Throwable cause) {
        super(cause);
    }

    public PropertyFilterException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
