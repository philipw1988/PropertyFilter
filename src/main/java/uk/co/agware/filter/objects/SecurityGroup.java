package uk.co.agware.filter.objects;

import java.util.List;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 17/09/2015.
 */
public class SecurityGroup implements Comparable<SecurityGroup> {

    private String name;
    private List<String> members;
    private List<Access> access;

    public SecurityGroup() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }

    public List<Access> getAccess() {
        return access;
    }

    public void setAccess(List<Access> access) {
        this.access = access;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SecurityGroup)) return false;
        SecurityGroup that = (SecurityGroup) o;
        return name != null ? name.equals(that.name) : that.name == null;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    @Override
    public int compareTo(SecurityGroup o) {
        if(this.name == null) return -1;
        if(o.name == null) return 1;
        return this.name.compareTo(o.name);
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
