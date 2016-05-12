package uk.co.agware.filter.test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import uk.co.agware.filter.PropertyFilter;
import uk.co.agware.filter.objects.Access;
import uk.co.agware.filter.objects.Permission;
import uk.co.agware.filter.objects.Group;
import uk.co.agware.filter.test.classes.SecondTestClass;
import uk.co.agware.filter.test.classes.TestClass;
import uk.co.agware.filter.util.FilterUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 10/04/2016.
 */
@SuppressWarnings("Duplicates")
public class TestPropertyFilterSave {

    private String testString1 = "Test String";
    private BigDecimal testBD1 = BigDecimal.valueOf(37);
    private String listString1 = "First";
    private String listString2 = "Second";
    private String listString3 = "Not Fourth";

    private TestClass testClass;
    private SecondTestClass secondTestClass1;
    private String secondTestName1 = "Second Test";
    private Integer secondTestInt1 = 2;
    private SecondTestClass secondTestClass2;
    private String secondTestName2 = "Second Test 2";
    private Integer secondTestInt2 = null;
    private SecondTestClass secondTestClass3;
    private String secondTestName3 = "Second Test 3";
    private Integer secondTestInt3 = 22;

    private String testString2 = "String";
    private BigDecimal testBD2 = BigDecimal.valueOf(42);
    private String listString21 = "1st";
    private String listString22 = "2nd";
    private String listString23 = "3rd";

    private TestClass testClass2;
    private SecondTestClass secondTestClass11;
    private String secondTestName11 = "Second Test";
    private Integer secondTestInt11 = 22;
    private SecondTestClass secondTestClass22;
    private String secondTestName22 = "Second Test 2";
    private Integer secondTestInt22 = 212;
    private SecondTestClass secondTestClass23;
    private String secondTestName23 = "Second Test 3";
    private Integer secondTestInt23 = 222;

    private PropertyFilter propertyFilter;
    private String username = "test";
    private String groupName = "Test Group";

    @Before
    public void setUp(){
        List<SecondTestClass> secondTestClasses = new ArrayList<>(3);
        secondTestClass1 = new SecondTestClass(secondTestName1, secondTestInt1);
        secondTestClasses.add(secondTestClass1);
        secondTestClass2 = new SecondTestClass(secondTestName2, secondTestInt2);
        secondTestClasses.add(secondTestClass2);
        secondTestClass3 = new SecondTestClass(secondTestName3, secondTestInt3);
        secondTestClasses.add(secondTestClass3);

        testClass = new TestClass();
        testClass.setTestString(testString1);
        testClass.setTestBD(testBD1);
        testClass.setSecondTestClasses(secondTestClasses);
        testClass.setStringList(new ArrayList<>(Arrays.asList(listString1, listString2, listString3)));

        List<SecondTestClass> secondTestClasses2 = new ArrayList<>(3);
        secondTestClass11 = new SecondTestClass(secondTestName11, secondTestInt11);
        secondTestClasses2.add(secondTestClass11);
        secondTestClass22 = new SecondTestClass(secondTestName22, secondTestInt22);
        secondTestClasses2.add(secondTestClass22);
        secondTestClass23 = new SecondTestClass(secondTestName23, secondTestInt23);
        secondTestClasses2.add(secondTestClass23);

        testClass2 = new TestClass();
        testClass2.setTestString(testString2);
        testClass2.setTestBD(testBD2);
        testClass2.setSecondTestClasses(secondTestClasses2);
        testClass2.setStringList(new ArrayList<>(Arrays.asList(listString21, listString22, listString23)));
    }

    @Test
    public void testNoAccess(){
        propertyFilter = new PropertyFilter();
        FilterUtil.setDefaultAccessType(Access.Type.NO_ACCESS);
        FilterUtil.setDefaultPermissionType(Permission.Type.NO_ACCESS);
        List<Access> accessList = FilterUtil.getFullAccessList("uk.co.agware.filter.test.classes");
        Group group = new Group();
        group.setName(groupName);
        group.setAccess(accessList);
        group.setMembers(Collections.singletonList(username));
        propertyFilter.setGroups(Collections.singletonList(group));

        Exception ex = null;
        try {
            propertyFilter.parseObjectForSaving(testClass, testClass2, username);
        } catch (IllegalAccessException e) {
            ex = e;
        }
        Assert.assertNotNull(ex);
    }

    @Test
    public void testReadAccess(){
        propertyFilter = new PropertyFilter();
        FilterUtil.setDefaultAccessType(Access.Type.READ);
        FilterUtil.setDefaultPermissionType(Permission.Type.NO_ACCESS);
        List<Access> accessList = FilterUtil.getFullAccessList("uk.co.agware.filter.test.classes");
        Group group = new Group();
        group.setName(groupName);
        group.setAccess(accessList);
        group.setMembers(Collections.singletonList(username));
        propertyFilter.setGroups(Collections.singletonList(group));

        Exception ex = null;
        try {
            propertyFilter.parseObjectForSaving(testClass, testClass2, username);
        } catch (IllegalAccessException e) {
            ex = e;
        }
        Assert.assertNotNull(ex);
    }

    @Test
    public void testUpdateAccessNoPermissions(){
        propertyFilter = new PropertyFilter();
        FilterUtil.setDefaultAccessType(Access.Type.UPDATE);
        FilterUtil.setDefaultPermissionType(Permission.Type.NO_ACCESS);
        List<Access> accessList = FilterUtil.getFullAccessList("uk.co.agware.filter.test.classes");
        Group group = new Group();
        group.setName(groupName);
        group.setAccess(accessList);
        group.setMembers(Collections.singletonList(username));
        propertyFilter.setGroups(Collections.singletonList(group));

        Exception ex = null;
        try {
            propertyFilter.parseObjectForSaving(testClass, testClass2, username);
        } catch (IllegalAccessException e) {
            ex = e;
        }
        Assert.assertNull(ex);
        Assert.assertEquals(testBD2, testClass2.getTestBD());
        Assert.assertEquals(3, testClass2.getStringList().size());
        Assert.assertTrue(testClass2.getStringList().contains(listString21));
        Assert.assertTrue(testClass2.getStringList().contains(listString22));
        Assert.assertTrue(testClass2.getStringList().contains(listString23));
    }

    @Test
    public void testCreateAccessNoPermissions(){
        propertyFilter = new PropertyFilter();
        FilterUtil.setDefaultAccessType(Access.Type.CREATE);
        FilterUtil.setDefaultPermissionType(Permission.Type.NO_ACCESS);
        List<Access> accessList = FilterUtil.getFullAccessList("uk.co.agware.filter.test.classes");
        Group group = new Group();
        group.setName(groupName);
        group.setAccess(accessList);
        group.setMembers(Collections.singletonList(username));
        propertyFilter.setGroups(Collections.singletonList(group));

        Exception ex = null;
        try {
            propertyFilter.parseObjectForSaving(testClass, testClass2, username);
        } catch (IllegalAccessException e) {
            ex = e;
        }
        Assert.assertNull(ex);
        Assert.assertEquals(testBD2, testClass2.getTestBD());
        Assert.assertEquals(3, testClass2.getStringList().size());
        Assert.assertTrue(testClass2.getStringList().contains(listString21));
        Assert.assertTrue(testClass2.getStringList().contains(listString22));
        Assert.assertTrue(testClass2.getStringList().contains(listString23));
    }

    @Test
    public void testUpdateAccessWritePermissions(){
        propertyFilter = new PropertyFilter();
        FilterUtil.setDefaultAccessType(Access.Type.UPDATE);
        FilterUtil.setDefaultPermissionType(Permission.Type.WRITE);
        List<Access> accessList = FilterUtil.getFullAccessList("uk.co.agware.filter.test.classes");
        Group group = new Group();
        group.setName(groupName);
        group.setAccess(accessList);
        group.setMembers(Collections.singletonList(username));
        propertyFilter.setGroups(Collections.singletonList(group));

        Exception ex = null;
        try {
            propertyFilter.parseObjectForSaving(testClass, testClass2, username);
        } catch (IllegalAccessException e) {
            ex = e;
        }
        Assert.assertNull(ex);
        Assert.assertEquals(testString2, testClass2.getTestString());
        Assert.assertEquals(testBD1, testClass2.getTestBD());
        Assert.assertEquals(3, testClass2.getStringList().size());
        Assert.assertTrue(testClass2.getStringList().contains(listString1));
        Assert.assertTrue(testClass2.getStringList().contains(listString2));
        Assert.assertTrue(testClass2.getStringList().contains(listString3));
        Assert.assertTrue(testClass2.getSecondTestClasses().contains(secondTestClass1));
        Assert.assertTrue(testClass2.getSecondTestClasses().contains(secondTestClass2));
        Assert.assertTrue(testClass2.getSecondTestClasses().contains(secondTestClass3));
    }

    @Test
    public void testCreateAccessWritePermissions(){
        propertyFilter = new PropertyFilter();
        FilterUtil.setDefaultAccessType(Access.Type.CREATE);
        FilterUtil.setDefaultPermissionType(Permission.Type.WRITE);
        List<Access> accessList = FilterUtil.getFullAccessList("uk.co.agware.filter.test.classes");
        Group group = new Group();
        group.setName(groupName);
        group.setAccess(accessList);
        group.setMembers(Collections.singletonList(username));
        propertyFilter.setGroups(Collections.singletonList(group));

        Exception ex = null;
        try {
            propertyFilter.parseObjectForSaving(testClass, testClass2, username);
        } catch (IllegalAccessException e) {
            ex = e;
        }
        Assert.assertNull(ex);
        Assert.assertEquals(testString2,testClass2.getTestString());
        Assert.assertEquals(testBD1, testClass2.getTestBD());
        Assert.assertEquals(3, testClass2.getStringList().size());
        Assert.assertTrue(testClass2.getStringList().contains(listString1));
        Assert.assertTrue(testClass2.getStringList().contains(listString2));
        Assert.assertTrue(testClass2.getStringList().contains(listString3));
        Assert.assertTrue(testClass2.getSecondTestClasses().contains(secondTestClass1));
        Assert.assertTrue(testClass2.getSecondTestClasses().contains(secondTestClass2));
        Assert.assertTrue(testClass2.getSecondTestClasses().contains(secondTestClass3));
    }
}
