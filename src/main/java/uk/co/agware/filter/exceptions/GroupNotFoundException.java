package uk.co.agware.filter.exceptions;

/**
 * Thrown when a supplied security group does not exist
 *
 * @author Philip Ward {@literal <Philip.Ward@agware.com>}
 * @since 11/07/2016.
 */
public class GroupNotFoundException extends RuntimeException {

    public GroupNotFoundException(String group){
        super(String.format("Unable to find group %s", group));
    }
}
