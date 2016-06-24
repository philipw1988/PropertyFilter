package uk.co.agware.filter.data;

import java.util.List;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 24/06/2016.
 */
public interface IGroup extends Comparable<IGroup> {

    String getName();

    void setName(String name);

    List<String> getMembers();

    void setMembers(List<String> members);

    List<IAccess> getAccess();

    <T extends IAccess> void setAccess(List<T> accessList);
}
