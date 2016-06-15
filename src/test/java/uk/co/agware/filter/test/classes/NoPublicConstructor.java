package uk.co.agware.filter.test.classes;

import uk.co.agware.filter.annotations.Create;
import uk.co.agware.filter.annotations.FilterTarget;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 15/06/2016.
 */
@FilterTarget
@Create
public class NoPublicConstructor {

    private NoPublicConstructor(){}
}
