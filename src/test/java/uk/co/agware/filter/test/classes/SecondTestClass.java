package uk.co.agware.filter.test.classes;

import uk.co.agware.filter.annotations.FilterTarget;
import uk.co.agware.filter.annotations.NoAccess;
import uk.co.agware.filter.annotations.ReadOnly;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 10/04/2016.
 */
@FilterTarget("Second Test Class")
@ReadOnly
public class SecondTestClass {

    @ReadOnly
    private String id;
    private Integer number;
    @NoAccess private String secret;

    public SecondTestClass() {
    }

    public SecondTestClass(String id, Integer number, String secret) {
        this.id = id;
        this.number = number;
        this.secret = secret;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SecondTestClass)) return false;
        SecondTestClass that = (SecondTestClass) o;
        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
