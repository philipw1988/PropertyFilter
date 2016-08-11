package uk.co.agware.filter.test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import uk.co.agware.filter.PropertyFilter;
import uk.co.agware.filter.PropertyFilterBuilder;
import uk.co.agware.filter.data.AccessType;
import uk.co.agware.filter.data.PermissionType;
import uk.co.agware.filter.exceptions.FilterException;
import uk.co.agware.filter.exceptions.GroupNotFoundException;
import uk.co.agware.filter.exceptions.PropertyFilterException;
import uk.co.agware.filter.impl.AccessImpl;
import uk.co.agware.filter.impl.DefaultClassFactory;
import uk.co.agware.filter.impl.GroupImpl;
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
public class TestPropertyFilterReturn {

    private String testString1 = "Test String";
    private BigDecimal testBD1 = BigDecimal.valueOf(37);
    private String listString1 = "First";
    private String listString2 = "Second";
    private String listString3 = "Not Fourth";

    private TestClass testClass;
    private SecondTestClass secondTestClass1;
    private String secondTestName1 = "Second Test";
    private String secondTestSecret1 = "Secret 1";
    private Integer secondTestInt1 = 2;
    private SecondTestClass secondTestClass2;
    private String secondTestName2 = "Second Test 2";
    private String secondTestSecret2 = "Secret 2";
    private Integer secondTestInt2 = 12;
    private SecondTestClass secondTestClass3;
    private String secondTestName3 = "Second Test 3";
    private String secondTestSecret3 = "Secret 3";
    private Integer secondTestInt3 = 22;

    private PropertyFilter propertyFilter;
    private FilterUtil filterUtil;
    private String username = "test";
    private String groupName = "Test Group";

    @Before
    public void setUp(){
        List<SecondTestClass> secondTestClasses = new ArrayList<>(3);
        secondTestClass1 = new SecondTestClass(secondTestName1, secondTestInt1, secondTestSecret1);
        secondTestClasses.add(secondTestClass1);
        secondTestClass2 = new SecondTestClass(secondTestName2, secondTestInt2, secondTestSecret2);
        secondTestClasses.add(secondTestClass2);
        secondTestClass3 = new SecondTestClass(secondTestName3, secondTestInt3, secondTestSecret3);
        secondTestClasses.add(secondTestClass3);

        testClass = new TestClass();
        testClass.setId(testString1);
        testClass.setTestBD(testBD1);
        testClass.setSecondTestClasses(secondTestClasses);
        testClass.setStringList(new ArrayList<>(Arrays.asList(listString1, listString2, listString3)));

        filterUtil = new FilterUtil(new DefaultClassFactory());
    }

    @Test
    public void testReturnNoAccess() throws PropertyFilterException {
        propertyFilter = new PropertyFilterBuilder().filterUtil(filterUtil).build();
        filterUtil.setDefaultAccessType(AccessType.NO_ACCESS);
        filterUtil.setDefaultPermissionType(PermissionType.NO_ACCESS);
        List<AccessImpl> accessList = filterUtil.getFullAccessList("uk.co.agware.filter.test.classes");
        GroupImpl group = new GroupImpl();
        group.setName(groupName);
        group.setAccess(accessList);
        group.setMembers(Collections.singletonList(username));
        propertyFilter.setGroups(Collections.singletonList(group));

        Object o = propertyFilter.parseObjectForReturn(testClass, username);
        Assert.assertNull(o);
    }

    @Test
    public void testReturnNoAccessWritePermission() throws PropertyFilterException {
        propertyFilter = new PropertyFilterBuilder().filterUtil(filterUtil).build();
        filterUtil.setDefaultAccessType(AccessType.NO_ACCESS);
        filterUtil.setDefaultPermissionType(PermissionType.WRITE);
        List<AccessImpl> accessList = filterUtil.getFullAccessList("uk.co.agware.filter.test.classes");
        GroupImpl group = new GroupImpl();
        group.setName(groupName);
        group.setAccess(accessList);
        group.setMembers(Collections.singletonList(username));
        propertyFilter.setGroups(Collections.singletonList(group));

        Object o = propertyFilter.parseObjectForReturn(testClass, username);
        Assert.assertNull(o);
    }

    @Test
    public void testReturnReadAccessNoPermission() throws PropertyFilterException {
        propertyFilter = new PropertyFilterBuilder().filterUtil(filterUtil).build();
        filterUtil.setDefaultAccessType(AccessType.READ);
        filterUtil.setDefaultPermissionType(PermissionType.NO_ACCESS);
        List<AccessImpl> accessList = filterUtil.getFullAccessList("uk.co.agware.filter.test.classes");
        GroupImpl group = new GroupImpl();
        group.setName(groupName);
        group.setAccess(accessList);
        group.setMembers(Collections.singletonList(username));
        propertyFilter.setGroups(Collections.singletonList(group));

        TestClass t = propertyFilter.parseObjectForReturn(testClass, username);
        Assert.assertNotNull(t);
        Assert.assertNull(t.getTestBD());
        Assert.assertNull(t.getSecondTestClasses());
        Assert.assertNull(t.getStringList());
    }

    @Test
    public void testReturnReadAccessReadPermission() throws PropertyFilterException {
        propertyFilter = new PropertyFilterBuilder().filterUtil(filterUtil).build();
        filterUtil.setDefaultAccessType(AccessType.READ);
        filterUtil.setDefaultPermissionType(PermissionType.READ);
        List<AccessImpl> accessList = filterUtil.getFullAccessList("uk.co.agware.filter.test.classes");
        GroupImpl group = new GroupImpl();
        group.setName(groupName);
        group.setAccess(accessList);
        group.setMembers(Collections.singletonList(username));
        propertyFilter.setGroups(Collections.singletonList(group));

        TestClass t = propertyFilter.parseObjectForReturn(testClass, username);
        Assert.assertNotNull(t);
        Assert.assertEquals(testBD1, t.getTestBD());
        // Check both lists contain the same values
        Assert.assertArrayEquals(testClass.getStringList().toArray(), t.getStringList().toArray());
        Assert.assertArrayEquals(testClass.getSecondTestClasses().toArray(), t.getSecondTestClasses().toArray());
    }

    @Test(expected = FilterException.class)
    public void testUserHasNoGroup() throws PropertyFilterException {
        propertyFilter = new PropertyFilterBuilder().filterUtil(filterUtil).build();
        filterUtil.setDefaultAccessType(AccessType.READ);
        filterUtil.setDefaultPermissionType(PermissionType.READ);
        List<AccessImpl> accessList = filterUtil.getFullAccessList("uk.co.agware.filter.test.classes");
        GroupImpl group = new GroupImpl();
        group.setName(groupName);
        group.setAccess(accessList);
        group.setMembers(Collections.singletonList(username));
        propertyFilter.setGroups(Collections.singletonList(group));

        propertyFilter.parseObjectForReturn(new TestClass(), "A user without a group");
    }

    @Test(expected = GroupNotFoundException.class)
    public void testUnknownGroupName() throws PropertyFilterException {
        propertyFilter = new PropertyFilterBuilder().filterUtil(filterUtil).build();
        filterUtil.setDefaultAccessType(AccessType.READ);
        filterUtil.setDefaultPermissionType(PermissionType.READ);
        List<AccessImpl> accessList = filterUtil.getFullAccessList("uk.co.agware.filter.test.classes");
        GroupImpl group = new GroupImpl();
        group.setName(groupName);
        group.setAccess(accessList);
        group.setMembers(Collections.singletonList(username));
        propertyFilter.setGroups(Collections.singletonList(group));

        propertyFilter.parseObjectForReturn(new TestClass(), username, "Silly Group Name");
    }

    /* Even though the "secret" field is @NoAccess on SecondTestClass, it should be copied over since we're not filtering collections */
    @Test
    public void testIgnoreFilterCollection() throws PropertyFilterException {
        propertyFilter = new PropertyFilterBuilder()
                .filterUtil(filterUtil)
                .filterCollectionsOnLoad(false)
                .build();

        filterUtil.setDefaultAccessType(AccessType.CREATE);
        filterUtil.setDefaultPermissionType(PermissionType.WRITE);
        List<AccessImpl> accessList = filterUtil.getFullAccessList("uk.co.agware.filter.test.classes");
        GroupImpl group = new GroupImpl();
        group.setName(groupName);
        group.setAccess(accessList);
        group.setMembers(Collections.singletonList(username));
        propertyFilter.setGroups(Collections.singletonList(group));

        TestClass result = propertyFilter.parseObjectForReturn(testClass, username);
        Assert.assertEquals(3, result.getSecondTestClasses().size());
        Assert.assertNotNull(result.getSecondTestClasses().get(0));
        Assert.assertNotNull(result.getSecondTestClasses().get(1));
        Assert.assertNotNull(result.getSecondTestClasses().get(2));
    }
}
