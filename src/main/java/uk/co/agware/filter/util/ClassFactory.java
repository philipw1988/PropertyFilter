package uk.co.agware.filter.util;

import uk.co.agware.filter.data.Access;
import uk.co.agware.filter.data.Permission;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 24/06/2016.
 */
public interface ClassFactory {

    <T extends Access> T createAccessClass();

    <T extends Access> T copyAccessClass(T old);

    <T extends Permission> T createPermissionClass();

    <T extends Permission> T copyPermissionClass(T old);
}
