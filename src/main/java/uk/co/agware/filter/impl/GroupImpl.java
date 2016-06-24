package uk.co.agware.filter.impl;

import uk.co.agware.filter.data.IAccess;
import uk.co.agware.filter.data.IGroup;

import java.util.List;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 17/09/2015.
 */
public class GroupImpl implements IGroup {

    private String name;
    private List<String> members;
    private List<IAccess> access;

    public GroupImpl() {
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public List<String> getMembers() {
        return members;
    }

    @Override
    public void setMembers(List<String> members) {
        this.members = members;
    }

    @Override
    public List<IAccess> getAccess() {
        return access;
    }

    @Override
    public <T extends IAccess> void setAccess(List<T> accessList) {
        access = (List<IAccess>) accessList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GroupImpl)) return false;
        GroupImpl that = (GroupImpl) o;
        return name != null ? name.equals(that.name) : that.name == null;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    @Override
    public int compareTo(IGroup o) {
        if(this.name == null) return -1;
        if(o.getName() == null) return 1;
        return this.name.compareTo(o.getName());
    }

    @Override
    public String toString() {
        return "SecurityGroup{" +
                "name='" + name + '\'' +
                ", members=" + members +
                ", access=" + access +
                '}';
    }
}
