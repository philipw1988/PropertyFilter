package uk.co.agware.filter.data;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 24/06/2016.
 */
public interface IPermission extends Comparable<IPermission> {

    String getPropertyName();

    String getDisplayName();

    PermissionType getPermission();

    boolean isModifiable();

    void setPermission(PermissionType noAccess);

    void setModifiable(boolean b);

    void setPropertyName(String name);

    void setDisplayName(String s);
}
