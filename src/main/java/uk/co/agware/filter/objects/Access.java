package uk.co.agware.filter.objects;

import uk.co.agware.filter.util.FilterUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 17/09/2015.
 */
public class Access implements Comparable<Access> {

    public enum Type {NO_ACCESS, READ, CREATE, UPDATE}
    String objectClass;
    private Type access;
    private List<Permission> permissions;

    public Access() {
    }

    public Access(Access access){
        if(access == null) throw new IllegalArgumentException("Trying to create a copy of a null Access");
        this.objectClass = access.getObjectClass();
        this.access = access.getAccess();
        this.permissions = new ArrayList<>(FilterUtil.checkNull(access.getPermissions()).size());
        permissions.addAll(FilterUtil.checkNull(access.getPermissions()).stream().map(Permission::new).collect(Collectors.toList()));
    }

    public String getObjectClass() {
        return objectClass;
    }

    public void setObjectClass(String objectClass) {
        this.objectClass = objectClass;
    }

    public Type getAccess() {
        return access;
    }

    public void setAccess(Type access) {
        this.access = access;
    }

    public List<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<Permission> permissions) {
        this.permissions = permissions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Access)) return false;
        if (!super.equals(o)) return false;

        Access access = (Access) o;

        if (objectClass != null ? !objectClass.equals(access.objectClass) : access.objectClass != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (objectClass != null ? objectClass.hashCode() : 0);
        return result;
    }

    @Override
    public int compareTo(Access o) {
        if(this.objectClass == null) return -1;
        if(o.objectClass == null) return 1;
        return this.objectClass.compareTo(o.objectClass);
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
