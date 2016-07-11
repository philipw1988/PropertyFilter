package uk.co.agware.filter.test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import uk.co.agware.filter.PropertyFilter;
import uk.co.agware.filter.PropertyFilterBuilder;
import uk.co.agware.filter.data.Access;
import uk.co.agware.filter.data.AccessType;
import uk.co.agware.filter.data.Permission;
import uk.co.agware.filter.data.PermissionType;
import uk.co.agware.filter.exceptions.PropertyFilterException;
import uk.co.agware.filter.impl.AccessImpl;
import uk.co.agware.filter.impl.DefaultClassFactory;
import uk.co.agware.filter.impl.GroupImpl;
import uk.co.agware.filter.test.classes.*;
import uk.co.agware.filter.util.FilterUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 9/04/2016.
 */
public class TestPropertyFilter {

    private PropertyFilter propertyFilter;
    private FilterUtil filterUtil;

    private String username = "test";
    private String groupName = "Test Group";

    @Before
    public void setUp(){
        filterUtil = new FilterUtil(new DefaultClassFactory());
        propertyFilter = new PropertyFilterBuilder().filterUtil(filterUtil).build();

        filterUtil.setDefaultAccessType(AccessType.NO_ACCESS);
        filterUtil.setDefaultPermissionType(PermissionType.NO_ACCESS);
        List<Access> accessList = filterUtil.getFullAccessList("uk.co.agware.filter.test.classes");
        GroupImpl group = new GroupImpl();
        group.setName(groupName);
        group.setAccess(accessList);
        group.setMembers(Collections.singletonList(username));
        propertyFilter.setGroups(Collections.singletonList(group));
    }

    @Test
    public void testFilteringSettings(){
        // Tests Default
        Assert.assertTrue(propertyFilter.filterCollectionsOnLoad());
        Assert.assertTrue(propertyFilter.filterCollectionsOnSave());
        propertyFilter.filterCollectionsOnSave(false);
        propertyFilter.filterCollectionsOnLoad(false);
        Assert.assertFalse(propertyFilter.filterCollectionsOnLoad());
        Assert.assertFalse(propertyFilter.filterCollectionsOnSave());
    }

    @Test
    public void testGetUsersGroup() throws PropertyFilterException {
        Assert.assertEquals(groupName, propertyFilter.getUsersGroup(username));
        Assert.assertEquals(groupName, propertyFilter.getUsersGroup("TEST"));
        Assert.assertEquals(groupName, propertyFilter.getUsersGroup("Test"));
        Assert.assertEquals(groupName, propertyFilter.getUsersGroup("TesT"));
    }

    @Test
    public void testCollectionClasses(){
        Assert.assertTrue(propertyFilter.ignoredClassesContains(String.class));
        propertyFilter.addIgnoredClass(Short.class);
        Assert.assertTrue(propertyFilter.ignoredClassesContains(Short.class));
        Assert.assertTrue(propertyFilter.removeCollectionClass(Short.class));
        Assert.assertFalse(propertyFilter.ignoredClassesContains(Short.class));
    }

    @Test
    public void testLoad() throws PropertyFilterException {
        Assert.assertEquals(propertyFilter.getUsersGroup(username), groupName);
        Map<String, Access> storedGroup = propertyFilter.getGroup(propertyFilter.getUsersGroup(username));
        Assert.assertEquals(5, storedGroup.keySet().size());
        Assert.assertTrue(storedGroup.keySet().contains(TestClass.class.getName()));
        Assert.assertTrue(storedGroup.keySet().contains(NoPublicConstructor.class.getName()));
        Assert.assertTrue(storedGroup.keySet().contains(NoDefaultConstructor.class.getName()));
        Assert.assertTrue(storedGroup.keySet().contains(SecondTestClass.class.getName()));
        Assert.assertTrue(storedGroup.keySet().contains(NoAccessClass.class.getName()));
    }

    @Test
    public void testGetAccess() throws PropertyFilterException {
        Access access = propertyFilter.getAccess(TestClass.class.getName(), username);
        Assert.assertEquals(new AccessImpl(TestClass.class.getName(), AccessType.NO_ACCESS, true), access);

        access = propertyFilter.getAccess(SecondTestClass.class.getName(), username);
        Assert.assertEquals(new AccessImpl(SecondTestClass.class.getName(), AccessType.READ, false), access);

        access = propertyFilter.getAccess(NoPublicConstructor.class.getName(), username);
        Assert.assertEquals(new AccessImpl(NoPublicConstructor.class.getName(), AccessType.CREATE, false), access);

        access = propertyFilter.getAccess(NoDefaultConstructor.class.getName(), username);
        Assert.assertEquals(new AccessImpl(NoDefaultConstructor.class.getName(), AccessType.UPDATE, false), access);
    }

    @Test
    public void testGetAvailableClasses(){
        Assert.assertEquals(3, propertyFilter.getAccessibleClassesForGroup(groupName).size()); // The three objects with hardcoded values
    }

    @Test
    public void testGetAccessibleClassesFromGroup() throws PropertyFilterException {
        filterUtil.setDefaultAccessType(AccessType.CREATE);
        List<Access> accessList = filterUtil.getFullAccessList("uk.co.agware.filter.test.classes");
        GroupImpl group = new GroupImpl();
        group.setName(groupName);
        group.setAccess(accessList);
        group.setMembers(Collections.singletonList(username));
        propertyFilter.setGroups(Collections.singletonList(group));

        String groupName = propertyFilter.getUsersGroup(username);
        List<String> availableClasses = propertyFilter.getAccessibleClassesForGroup(groupName);
        Assert.assertEquals(4, availableClasses.size());
    }

    @Test
    public void testGetAccessibleFieldsFromClassName() throws PropertyFilterException {
        List<Permission> permissions = propertyFilter.getAccessibleFieldsForGroup(TestClass.class.getName(), groupName);
        Assert.assertEquals(1, permissions.size());
        permissions = propertyFilter.getAccessibleFieldsForGroup(SecondTestClass.class.getName(), groupName);
        Assert.assertEquals(1, permissions.size());
    }

    @Test
    public void testGetAccessibleFieldsFromDisplayName() throws PropertyFilterException {
        List<Permission> permissions = propertyFilter.getAccessibleFieldsForGroup("Test Class", groupName);
        Assert.assertEquals(1, permissions.size());
        permissions = propertyFilter.getAccessibleFieldsForGroup(SecondTestClass.class.getName(), groupName);
        Assert.assertEquals(1, permissions.size());
    }

    @Test
    public void testGetAccessibleFieldsFromGroup() throws PropertyFilterException {
        String groupName = propertyFilter.getUsersGroup(username);
        List<Permission> permissions = propertyFilter.getAccessibleFieldsForGroup(TestClass.class.getName(), groupName);
        Assert.assertEquals(1, permissions.size());
    }

    @Test
    public void testGetAccessFromClassName() throws PropertyFilterException {
        Access access = propertyFilter.getAccess(TestClass.class.getName(), username);
        Assert.assertEquals(TestClass.class.getName(), access.getObjectClass());
    }

    @Test
    public void testGetAccessFromDisplayName() throws PropertyFilterException {
        Access access = propertyFilter.getAccess("Test Class", username);
        Assert.assertEquals(TestClass.class.getName(), access.getObjectClass());
    }

    @Test
    public void testGetAccessFromClassNameFromGroup() throws PropertyFilterException {
        Access access = propertyFilter.getAccessForGroup(TestClass.class.getName(), propertyFilter.getUsersGroup(username));
        Assert.assertEquals(TestClass.class.getName(), access.getObjectClass());
    }

    @Test
    public void testAddUserToGroup() throws PropertyFilterException {
        propertyFilter.addUserToGroup("Test Username", groupName);
        Assert.assertEquals(groupName, propertyFilter.getUsersGroup("Test Username"));
    }

    @Test
    public void testGetGroupMap(){
        Map<String, String> map = propertyFilter.getUserToGroupMap();
        Assert.assertNotNull(map);
        Assert.assertEquals(1, map.size());
        Assert.assertEquals(groupName, map.get(username.toUpperCase())); // Username map uppercases the usernames
    }
}
