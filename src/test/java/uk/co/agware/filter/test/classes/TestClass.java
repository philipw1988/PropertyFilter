package uk.co.agware.filter.test.classes;

import uk.co.agware.filter.annotations.ReadOnly;

import java.math.BigDecimal;
import java.util.List;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 10/04/2016.
 */
public class TestClass {

    @ReadOnly
    private String testString;
    private BigDecimal testBD;
    private List<String> stringList;
    private List<SecondTestClass> secondTestClasses;

    public TestClass() {
    }

    public TestClass(String testString, BigDecimal testBD, List<String> stringList, List<SecondTestClass> secondTestClasses) {
        this.testString = testString;
        this.testBD = testBD;
        this.stringList = stringList;
        this.secondTestClasses = secondTestClasses;
    }

    public String getTestString() {
        return testString;
    }

    public void setTestString(String testString) {
        this.testString = testString;
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
        return testString != null ? testString.equals(testClass.testString) : testClass.testString == null;
    }

    @Override
    public int hashCode() {
        return testString != null ? testString.hashCode() : 0;
    }
}
