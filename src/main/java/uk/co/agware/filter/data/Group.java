package uk.co.agware.filter.data;

import java.util.List;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 24/06/2016.
 */
public interface Group extends Comparable<Group> {

    String getId();

    String getName();

    void setName(String name);

    List<String> getMembers();

    void setMembers(List<String> members);

    <T extends Access> List<T> getAccess();

    <T extends Access> void setAccess(List<T> accessList);
}
