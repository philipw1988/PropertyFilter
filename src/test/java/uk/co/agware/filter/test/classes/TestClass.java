package uk.co.agware.filter.test.classes;

import uk.co.agware.filter.annotations.FilterTarget;
import uk.co.agware.filter.annotations.ReadOnly;

import java.math.BigDecimal;
import java.util.List;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 10/04/2016.
 */
@FilterTarget("Test Class")
public class TestClass {

    @ReadOnly private String id;
    private BigDecimal testBD;
    private List<String> stringList;
    private List<SecondTestClass> secondTestClasses;

    public TestClass() {
    }

    public TestClass(String id, BigDecimal testBD, List<String> stringList, List<SecondTestClass> secondTestClasses) {
        this.id = id;
        this.testBD = testBD;
        this.stringList = stringList;
        this.secondTestClasses = secondTestClasses;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public BigDecimal getTestBD() {
        return testBD;
    }

    public void setTestBD(BigDecimal testBD) {
        this.testBD = testBD;
    }

    public List<String> getStringList() {
        return stringList;
    }

    public void setStringList(List<String> stringList) {
        this.stringList = stringList;
    }

    public List<SecondTestClass> getSecondTestClasses() {
        return secondTestClasses;
    }

    public void setSecondTestClasses(List<SecondTestClass> secondTestClasses) {
        this.secondTestClasses = secondTestClasses;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TestClass)) return false;
        TestClass testClass = (TestClass) o;
        return id != null ? id.equals(testClass.id) : testClass.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
