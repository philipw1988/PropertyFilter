package uk.co.agware.filter.impl;

import uk.co.agware.filter.data.Access;
import uk.co.agware.filter.data.AccessType;

import java.util.List;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 17/09/2015.
 */
public class AccessImpl implements Access<PermissionImpl> {

    private String objectClass;
    private String displayName;
    private AccessType access;
    private boolean modifiable;
    private List<PermissionImpl> permissions;

    public AccessImpl() {
        modifiable = false;
    }

    public AccessImpl(String objectClass, AccessType access, boolean modifiable) {
        this.objectClass = objectClass;
        this.access = access;
        this.modifiable = modifiable;
    }

    @Override
    public String getObjectClass() {
        return objectClass;
    }

    @Override
    public void setObjectClass(String objectClass) {
        this.objectClass = objectClass;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public AccessType getAccess() {
        return access;
    }

    @Override
    public void setAccess(AccessType access) {
        this.access = access;
    }

    @Override
    public List<PermissionImpl> getPermissions() {
        return permissions;
    }

    @Override
    public void setPermissions(List<PermissionImpl> permissions) {
        this.permissions = permissions;
    }

    @Override
    public boolean isModifiable() {
        return modifiable;
    }

    @Override
    public void setModifiable(boolean modifiable) {
        this.modifiable = modifiable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AccessImpl)) return false;

        AccessImpl access1 = (AccessImpl) o;

        if (modifiable != access1.modifiable) return false;
        if (objectClass != null ? !objectClass.equals(access1.objectClass) : access1.objectClass != null) return false;
        return access == access1.access;

    }

    @Override
    public int hashCode() {
        int result = objectClass != null ? objectClass.hashCode() : 0;
        result = 31 * result + (access != null ? access.hashCode() : 0);
        result = 31 * result + (modifiable ? 1 : 0);
        return result;
    }

    @Override
    public int compareTo(Access o) {
        if(this.objectClass == null) return -1;
        if(o.getObjectClass() == null) return 1;
        return this.objectClass.compareTo(o.getObjectClass());
    }

    @Override
    public String toString() {
        return "Access{" +
                "objectClass='" + objectClass + '\'' +
                ", access=" + access +
                ", permissions=" + permissions +
                '}';
    }
}
