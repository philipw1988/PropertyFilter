package uk.co.agware.filter.data;

import java.util.List;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 24/06/2016.
 */
public interface Access extends Comparable<Access> {

    String getObjectClass();

    void setObjectClass(String name);

    String getDisplayName();

    void setDisplayName(String s);

    AccessType getAccess();

    void setAccess(AccessType read);

    <T extends Permission> List<T> getPermissions();

    <T extends Permission> void setPermissions(List<T> permissions);

    boolean isModifiable();

    void setModifiable(boolean b);
}
