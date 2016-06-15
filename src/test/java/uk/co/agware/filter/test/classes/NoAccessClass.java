package uk.co.agware.filter.test.classes;

import uk.co.agware.filter.annotations.FilterTarget;
import uk.co.agware.filter.annotations.NoAccess;
import uk.co.agware.filter.annotations.ReadOnly;
import uk.co.agware.filter.annotations.Write;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 12/06/2016.
 */
@FilterTarget
@NoAccess
public class NoAccessClass {
    @NoAccess private String id;
    @ReadOnly private String name;
    @Write private String email;
    private String other;
}
