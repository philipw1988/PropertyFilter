package uk.co.agware.filter.test.classes;

import uk.co.agware.filter.annotations.FilterTarget;
import uk.co.agware.filter.annotations.Update;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 10/04/2016.
 */
@FilterTarget("NDC")
@Update
public class NoDefaultConstructor {

    private String id;

    public NoDefaultConstructor(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
