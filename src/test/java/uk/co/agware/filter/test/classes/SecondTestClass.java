package uk.co.agware.filter.test.classes;

import uk.co.agware.filter.annotations.ReadOnly;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 10/04/2016.
 */
public class SecondTestClass {

    @ReadOnly
    private String name;
    private Integer number;

    public SecondTestClass() {
    }

    public SecondTestClass(String name, Integer number) {
        this.name = name;
        this.number = number;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SecondTestClass)) return false;
        SecondTestClass that = (SecondTestClass) o;
        return name != null ? name.equals(that.name) : that.name == null;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
