package uk.co.agware.filter.test.classes;

import uk.co.agware.filter.annotations.Hidden;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 10/04/2016.
 */
@Hidden
public class NoDefaultConstructor {

    private String id;

    public NoDefaultConstructor(String id) {
        this.id = id;
    }
}
