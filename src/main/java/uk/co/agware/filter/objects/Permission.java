package uk.co.agware.filter.objects;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 17/09/2015.
 */
public class Permission implements Comparable<Permission> {

    public enum Type {READ, WRITE, NO_ACCESS}

    private String propertyName;
    private String displayName;
    private Type permission;
    private boolean modifiable;

    public Permission() {
        this.modifiable = false;
    }

    public Permission(String propertyName, Type permission, boolean modifiable) {
        this.propertyName = propertyName;
        this.permission = permission;
        this.modifiable = modifiable;
    }

    public Permission(Permission p) {
        if(p == null) throw new IllegalArgumentException("Trying to create a copy of a null Permission");
        this.propertyName = p.getPropertyName();
        this.displayName = p.getDisplayName();
        this.permission = p.getPermission();
        this.modifiable = p.isModifiable();
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

    public Type getPermission() {
        return permission;
    }

    public void setPermission(Type permission) {
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
        if (!(o instanceof Permission)) return false;

        Permission that = (Permission) o;

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
        if(o.displayName == null) return 1;
        return this.displayName.compareTo(o.displayName);
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
