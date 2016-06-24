package uk.co.agware.filter.impl;

import uk.co.agware.filter.data.Permission;
import uk.co.agware.filter.data.PermissionType;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 17/09/2015.
 */
public class PermissionImpl implements Permission {

    private String propertyName;
    private String displayName;
    private PermissionType permission;
    private boolean modifiable;

    public PermissionImpl() {
        this.modifiable = false;
    }

    public PermissionImpl(String propertyName, PermissionType permission, boolean modifiable) {
        this.propertyName = propertyName;
        this.permission = permission;
        this.modifiable = modifiable;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public PermissionType getPermission() {
        return permission;
    }

    public void setPermission(PermissionType permission) {
        this.permission = permission;
    }

    public boolean isModifiable() {
        return modifiable;
    }

    public void setModifiable(boolean modifiable) {
        this.modifiable = modifiable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PermissionImpl)) return false;

        PermissionImpl that = (PermissionImpl) o;

        if (modifiable != that.modifiable) return false;
        if (propertyName != null ? !propertyName.equals(that.propertyName) : that.propertyName != null) return false;
        return permission == that.permission;

    }

    @Override
    public int hashCode() {
        int result = propertyName != null ? propertyName.hashCode() : 0;
        result = 31 * result + (permission != null ? permission.hashCode() : 0);
        result = 31 * result + (modifiable ? 1 : 0);
        return result;
    }

    @Override
    public int compareTo(Permission o) {
        if(this.displayName == null) return -1;
        if(o.getDisplayName() == null) return 1;
        return this.displayName.compareTo(o.getDisplayName());
    }

    @Override
    public String toString() {
        return "Permission{" +
                "propertyName='" + propertyName + '\'' +
                ", displayName='" + displayName + '\'' +
                ", permission=" + permission +
                '}';
    }
}
