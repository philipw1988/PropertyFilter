package uk.co.agware.filter.impl;

import uk.co.agware.filter.data.Access;
import uk.co.agware.filter.data.Permission;
import uk.co.agware.filter.util.ClassFactory;
import uk.co.agware.filter.util.FilterUtil;

import java.util.stream.Collectors;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 24/06/2016.
 */
public class DefaultClassFactory implements ClassFactory {

    @Override
    public AccessImpl createAccessClass() {
        return new AccessImpl();
    }

    @Override
    public AccessImpl copyAccessClass(Access old) {
        AccessImpl access = new AccessImpl();
        if(old == null) throw new IllegalArgumentException("Trying to create a copy of a null Access");
        access.setObjectClass(old.getObjectClass());
        access.setDisplayName(old.getDisplayName());
        access.setAccess(old.getAccess());
        access.setModifiable(old.isModifiable());
        access.setPermissions(FilterUtil.nullSafe(old.getPermissions()).stream().map(this::copyPermissionClass).collect(Collectors.toList()));
        return access;
    }

    @Override
    public PermissionImpl createPermissionClass() {
        return new PermissionImpl();
    }

    @Override
    public PermissionImpl copyPermissionClass(Permission old) {
        PermissionImpl permission = new PermissionImpl();
        if(old == null) throw new IllegalArgumentException("Trying to create a copy of a null Permission");
        permission.setPropertyName(old.getPropertyName());
        permission.setDisplayName(old.getDisplayName());
        permission.setPermission(old.getPermission());
        permission.setModifiable(old.isModifiable());
        return permission;
    }
}
