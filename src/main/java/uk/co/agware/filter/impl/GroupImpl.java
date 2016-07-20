package uk.co.agware.filter.impl;

import uk.co.agware.filter.data.Access;
import uk.co.agware.filter.data.Group;

import java.util.List;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 17/09/2015.
 */
public class GroupImpl implements Group {

    private String name;
    private List<String> members;
    private List<Access> access;

    public GroupImpl() {
    }

    @Override
    public String getId() {
        return null;
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
    public List<Access> getAccess() {
        return access;
    }

    @Override
    public <T extends Access> void setAccess(List<T> accessList) {
        access = (List<Access>) accessList;
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
    public int compareTo(Group o) {
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
