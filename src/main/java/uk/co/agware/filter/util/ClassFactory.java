package uk.co.agware.filter.util;

import uk.co.agware.filter.data.IAccess;
import uk.co.agware.filter.data.IPermission;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 24/06/2016.
 */
public interface ClassFactory {

    IAccess createAccessClass();

    <T extends IAccess> T copyAccessClass(T old);

    IPermission createPermissionClass();

    <T extends IPermission> T copyPermissionClass(T old);
}
