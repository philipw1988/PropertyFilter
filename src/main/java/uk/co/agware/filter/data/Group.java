package uk.co.agware.filter.data;

import java.util.List;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 24/06/2016.
 */
public interface Group<T extends Access> extends Comparable<Group<? extends Access>> {

    String getId();

    String getName();

    void setName(String name);

    List<String> getMembers();

    void setMembers(List<String> members);

    List<T> getAccess();

    void setAccess(List<T> accessList);
}
