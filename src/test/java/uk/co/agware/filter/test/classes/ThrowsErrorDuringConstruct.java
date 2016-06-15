package uk.co.agware.filter.test.classes;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 15/06/2016.
 */
public class ThrowsErrorDuringConstruct {

    public ThrowsErrorDuringConstruct() throws InstantiationException {
        throw new InstantiationException();
    }
}
