package uk.co.agware.filter.util;

import uk.co.agware.filter.data.Access;
import uk.co.agware.filter.data.Permission;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 24/06/2016.
 */
public interface ClassFactory<A extends Access, P extends Permission> {

    A createAccessClass();

    A copyAccessClass(A old);

    P createPermissionClass();

    P copyPermissionClass(P old);
}
