package uk.co.agware.filter.test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import uk.co.agware.filter.PropertyFilter;
import uk.co.agware.filter.exceptions.FilterException;
import uk.co.agware.filter.exceptions.PropertyFilterException;
import uk.co.agware.filter.objects.Access;
import uk.co.agware.filter.objects.Group;
import uk.co.agware.filter.objects.Permission;
import uk.co.agware.filter.test.classes.NoDefaultConstructor;
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
    private String secondTestId1 = "Second Test";
    private Integer secondTestInt1 = 2;
    private SecondTestClass secondTestClass2;
    private String secondTestId2 = "Second Test 2";
    private Integer secondTestInt2 = null;
    private SecondTestClass secondTestClass3;
    private String secondTestId3 = "Second Test 3";
    private Integer secondTestInt3 = 22;

    private String testString2 = "String";
    private BigDecimal testBD2 = BigDecimal.valueOf(42);
    private String listString21 = "1st";
    private String listString22 = "2nd";
    private String listString23 = "3rd";

    private TestClass testClass2;
    private SecondTestClass stc1;
    private String secondTestId21 = "Second Test";
    private Integer secondTestInt11 = 22;
    private SecondTestClass stc2;
    private String secondTestId22 = "Second Test 2";
    private Integer secondTestInt22 = 212;
    private SecondTestClass stc3;
    private String secondTestId23 = "Second Test 3";
    private Integer secondTestInt23 = 222;

    private PropertyFilter propertyFilter;
    private String username = "test";
    private String groupName = "Test Group";

    @Before
    public void setUp(){
        List<SecondTestClass> secondTestClasses = new ArrayList<>(3);
        secondTestClass1 = new SecondTestClass(secondTestId1, secondTestInt1);
        secondTestClasses.add(secondTestClass1);
        secondTestClass2 = new SecondTestClass(secondTestId2, secondTestInt2);
        secondTestClasses.add(secondTestClass2);
        secondTestClass3 = new SecondTestClass(secondTestId3, secondTestInt3);
        secondTestClasses.add(secondTestClass3);

        testClass = new TestClass();
        testClass.setId(testString1);
        testClass.setTestBD(testBD1);
        testClass.setSecondTestClasses(secondTestClasses);
        testClass.setStringList(new ArrayList<>(Arrays.asList(listString1, listString2, listString3)));

        List<SecondTestClass> secondTestClasses2 = new ArrayList<>(3);
        stc1 = new SecondTestClass(secondTestId21, secondTestInt11);
        secondTestClasses2.add(stc1);
        stc2 = new SecondTestClass(secondTestId22, secondTestInt22);
        secondTestClasses2.add(stc2);
        stc3 = new SecondTestClass(secondTestId23, secondTestInt23);
        secondTestClasses2.add(stc3);

        testClass2 = new TestClass();
        testClass2.setId(testString2);
        testClass2.setTestBD(testBD2);
        testClass2.setSecondTestClasses(secondTestClasses2);
        testClass2.setStringList(new ArrayList<>(Arrays.asList(listString21, listString22, listString23)));

        propertyFilter = new PropertyFilter();
    }

    @Test(expected = IllegalAccessException.class)
    public void testNoAccess() throws IllegalAccessException, PropertyFilterException {
        FilterUtil.setDefaultAccessType(Access.Type.NO_ACCESS);
        FilterUtil.setDefaultPermissionType(Permission.Type.NO_ACCESS);
        List<Access> accessList = FilterUtil.getFullAccessList("uk.co.agware.filter.test.classes");
        Group group = new Group();
        group.setName(groupName);
        group.setAccess(accessList);
        group.setMembers(Collections.singletonList(username));
        propertyFilter.setGroups(Collections.singletonList(group));

        propertyFilter.parseObjectForSaving(testClass, testClass2, username);
    }

    @Test(expected = IllegalAccessException.class)
    public void testReadAccess() throws IllegalAccessException, PropertyFilterException {
        FilterUtil.setDefaultAccessType(Access.Type.READ);
        FilterUtil.setDefaultPermissionType(Permission.Type.NO_ACCESS);
        List<Access> accessList = FilterUtil.getFullAccessList("uk.co.agware.filter.test.classes");
        Group group = new Group();
        group.setName(groupName);
        group.setAccess(accessList);
        group.setMembers(Collections.singletonList(username));
        propertyFilter.setGroups(Collections.singletonList(group));

        propertyFilter.parseObjectForSaving(testClass, testClass2, username);
    }

    @Test
    public void testUpdateAccessNoPermissions() throws IllegalAccessException, PropertyFilterException {
        FilterUtil.setDefaultAccessType(Access.Type.UPDATE);
        FilterUtil.setDefaultPermissionType(Permission.Type.NO_ACCESS);
        List<Access> accessList = FilterUtil.getFullAccessList("uk.co.agware.filter.test.classes");
        Group group = new Group();
        group.setName(groupName);
        group.setAccess(accessList);
        group.setMembers(Collections.singletonList(username));
        propertyFilter.setGroups(Collections.singletonList(group));

        propertyFilter.parseObjectForSaving(testClass, testClass2, username);

        Assert.assertEquals(testBD2, testClass2.getTestBD());
        Assert.assertEquals(3, testClass2.getStringList().size());
        Assert.assertTrue(testClass2.getStringList().contains(listString21));
        Assert.assertTrue(testClass2.getStringList().contains(listString22));
        Assert.assertTrue(testClass2.getStringList().contains(listString23));
    }

    @Test
    public void testCreateAccessNoPermissions() throws IllegalAccessException, PropertyFilterException {
        FilterUtil.setDefaultAccessType(Access.Type.CREATE);
        FilterUtil.setDefaultPermissionType(Permission.Type.NO_ACCESS);
        List<Access> accessList = FilterUtil.getFullAccessList("uk.co.agware.filter.test.classes");
        Group group = new Group();
        group.setName(groupName);
        group.setAccess(accessList);
        group.setMembers(Collections.singletonList(username));
        propertyFilter.setGroups(Collections.singletonList(group));

        propertyFilter.parseObjectForSaving(testClass, testClass2, username);

        Assert.assertEquals(testBD2, testClass2.getTestBD());
        Assert.assertEquals(3, testClass2.getStringList().size());
        Assert.assertTrue(testClass2.getStringList().contains(listString21));
        Assert.assertTrue(testClass2.getStringList().contains(listString22));
        Assert.assertTrue(testClass2.getStringList().contains(listString23));
    }

    @Test
    public void testUpdateAccessWritePermissions() throws IllegalAccessException, PropertyFilterException {
        FilterUtil.setDefaultAccessType(Access.Type.UPDATE);
        FilterUtil.setDefaultPermissionType(Permission.Type.WRITE);
        List<Access> accessList = FilterUtil.getFullAccessList("uk.co.agware.filter.test.classes");
        Group group = new Group();
        group.setName(groupName);
        group.setAccess(accessList);
        group.setMembers(Collections.singletonList(username));
        propertyFilter.setGroups(Collections.singletonList(group));

        propertyFilter.parseObjectForSaving(testClass, testClass2, username);

        Assert.assertEquals(testString2, testClass2.getId());
        Assert.assertEquals(testBD1, testClass2.getTestBD());
        Assert.assertEquals(3, testClass2.getStringList().size());
        Assert.assertTrue(testClass2.getStringList().contains(listString1));
        Assert.assertTrue(testClass2.getStringList().contains(listString2));
        Assert.assertTrue(testClass2.getStringList().contains(listString3));
        Assert.assertTrue(testClass2.getSecondTestClasses().contains(new SecondTestClass(secondTestId21, secondTestInt1)));
        Assert.assertTrue(testClass2.getSecondTestClasses().contains(new SecondTestClass(secondTestId22, secondTestInt2)));
        Assert.assertTrue(testClass2.getSecondTestClasses().contains(new SecondTestClass(secondTestId23, secondTestInt3)));
    }

    @Test
    public void testCreateAccessWritePermissions() throws IllegalAccessException, PropertyFilterException {
        FilterUtil.setDefaultAccessType(Access.Type.CREATE);
        FilterUtil.setDefaultPermissionType(Permission.Type.WRITE);
        List<Access> accessList = FilterUtil.getFullAccessList("uk.co.agware.filter.test.classes");
        Group group = new Group();
        group.setName(groupName);
        group.setAccess(accessList);
        group.setMembers(Collections.singletonList(username));
        propertyFilter.setGroups(Collections.singletonList(group));

        propertyFilter.parseObjectForSaving(testClass, testClass2, username);

        Assert.assertEquals(testString2,testClass2.getId());
        Assert.assertEquals(testBD1, testClass2.getTestBD());
        Assert.assertEquals(3, testClass2.getStringList().size());
        Assert.assertTrue(testClass2.getStringList().contains(listString1));
        Assert.assertTrue(testClass2.getStringList().contains(listString2));
        Assert.assertTrue(testClass2.getStringList().contains(listString3));
        Assert.assertTrue(testClass2.getSecondTestClasses().contains(secondTestClass1));
        Assert.assertTrue(testClass2.getSecondTestClasses().contains(secondTestClass2));
        Assert.assertTrue(testClass2.getSecondTestClasses().contains(secondTestClass3));
    }

    @Test(expected = FilterException.class)
    public void testNoDefinedAccess() throws IllegalAccessException, PropertyFilterException {
        FilterUtil.setDefaultAccessType(Access.Type.CREATE);
        FilterUtil.setDefaultPermissionType(Permission.Type.WRITE);
        List<Access> accessList = FilterUtil.getFullAccessList("uk.co.agware.filter.test.classes");
        Group group = new Group();
        group.setName(groupName);
        group.setAccess(accessList);
        group.setMembers(Collections.singletonList(username));
        propertyFilter.setGroups(Collections.singletonList(group));

        propertyFilter.parseObjectForSaving(new NoDefaultConstructor("1"), new NoDefaultConstructor("2"), username);
    }

    @Test(expected = PropertyFilterException.class)
    public void testNoGroup() throws IllegalAccessException, PropertyFilterException {
        FilterUtil.setDefaultAccessType(Access.Type.CREATE);
        FilterUtil.setDefaultPermissionType(Permission.Type.WRITE);
        List<Access> accessList = FilterUtil.getFullAccessList("uk.co.agware.filter.test.classes");
        Group group = new Group();
        group.setName(groupName);
        group.setAccess(accessList);
        group.setMembers(Collections.singletonList(username));
        propertyFilter.setGroups(Collections.singletonList(group));

        propertyFilter.parseObjectForSaving(new TestClass(), new TestClass(), "Some User");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullArg() throws IllegalAccessException {
        propertyFilter.parseObjectForSaving(null, null, username, groupName);
    }

    /* By setting ignore collections on save to true, the full collection should be copied over to the destination object regardless of permissions */
    @Test
    public void testIgnoredCollection() throws IllegalAccessException, PropertyFilterException {
        FilterUtil.setDefaultAccessType(Access.Type.CREATE);
        FilterUtil.setDefaultPermissionType(Permission.Type.WRITE);
        List<Access> accessList = FilterUtil.getFullAccessList("uk.co.agware.filter.test.classes");
        Group group = new Group();
        group.setName(groupName);
        group.setAccess(accessList);
        group.setMembers(Collections.singletonList(username));
        propertyFilter.setGroups(Collections.singletonList(group));

        List<SecondTestClass> secondTestClasses = new ArrayList<>(3);
        stc1 = new SecondTestClass("1", 123);
        secondTestClasses.add(stc1);
        stc2 = new SecondTestClass("2", 234);
        secondTestClasses.add(stc2);
        stc3 = new SecondTestClass("3", 345);
        secondTestClasses.add(stc3);
        TestClass tc3 = new TestClass();
        tc3.setSecondTestClasses(secondTestClasses);

        propertyFilter.filterCollectionsOnSave(false);
        TestClass tc = propertyFilter.parseObjectForSaving(tc3, testClass2, username);
        Assert.assertEquals(3, tc.getSecondTestClasses().size());
        Assert.assertTrue(tc.getSecondTestClasses().contains(stc1));
        Assert.assertTrue(tc.getSecondTestClasses().contains(stc2));
        Assert.assertTrue(tc.getSecondTestClasses().contains(stc3));
    }
}
