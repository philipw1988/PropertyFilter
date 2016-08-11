package uk.co.agware.filter.test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import uk.co.agware.filter.PropertyFilter;
import uk.co.agware.filter.PropertyFilterBuilder;
import uk.co.agware.filter.data.*;
import uk.co.agware.filter.exceptions.PropertyFilterException;
import uk.co.agware.filter.impl.AccessImpl;
import uk.co.agware.filter.impl.DefaultClassFactory;
import uk.co.agware.filter.impl.GroupImpl;
import uk.co.agware.filter.test.classes.*;
import uk.co.agware.filter.util.FilterUtil;

import java.util.Arrays;
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
        List<AccessImpl> accessList = filterUtil.getFullAccessList("uk.co.agware.filter.test.classes");
        GroupImpl group = new GroupImpl();
        group.setName(groupName);
        group.setAccess(accessList);
        group.setMembers(Collections.singletonList(username));
        propertyFilter.setGroups(Collections.singletonList(group));
    }

    @Test
    public void testGetUsersGroup() throws PropertyFilterException {
        Assert.assertEquals(groupName, propertyFilter.getUsersGroup(username));
        Assert.assertEquals(groupName, propertyFilter.getUsersGroup("TEST"));
        Assert.assertEquals(groupName, propertyFilter.getUsersGroup("Test"));
        Assert.assertEquals(groupName, propertyFilter.getUsersGroup("TesT"));
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
        Assert.assertEquals(3, propertyFilter.getAccessibleClasses(groupName).size()); // The three objects with hardcoded values
    }

    @Test
    public void testGetAccessibleClassesFromGroup() throws PropertyFilterException {
        filterUtil.setDefaultAccessType(AccessType.CREATE);
        List<AccessImpl> accessList = filterUtil.getFullAccessList("uk.co.agware.filter.test.classes");
        GroupImpl group = new GroupImpl();
        group.setName(groupName);
        group.setAccess(accessList);
        group.setMembers(Collections.singletonList(username));
        propertyFilter.setGroups(Collections.singletonList(group));

        String groupName = propertyFilter.getUsersGroup(username);
        List<String> availableClasses = propertyFilter.getAccessibleClasses(groupName);
        Assert.assertEquals(4, availableClasses.size());
    }

    @Test
    public void testGetAccessibleFieldsFromClassName() throws PropertyFilterException {
        List<Permission> permissions = (List<Permission>) propertyFilter.getAccessibleFields(TestClass.class.getName(), groupName);
        Assert.assertEquals(1, permissions.size());
        permissions = (List<Permission>) propertyFilter.getAccessibleFields(SecondTestClass.class.getName(), groupName);
        Assert.assertEquals(1, permissions.size());
    }

    @Test
    public void testGetAccessibleFieldsFromDisplayName() throws PropertyFilterException {
        List<Permission> permissions = (List<Permission>) propertyFilter.getAccessibleFields("Test Class", groupName);
        Assert.assertEquals(1, permissions.size());
        permissions = (List<Permission>) propertyFilter.getAccessibleFields(SecondTestClass.class.getName(), groupName);
        Assert.assertEquals(1, permissions.size());
    }

    @Test
    public void testGetAccessibleFieldsFromGroup() throws PropertyFilterException {
        String groupName = propertyFilter.getUsersGroup(username);
        List<Permission> permissions = (List<Permission>) propertyFilter.getAccessibleFields(TestClass.class.getName(), groupName);
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

    @Test
    public void testGetGroupMembership(){
        PropertyFilter propertyFilter = new PropertyFilterBuilder().build(); // Don't need to worry about anything else here
        GroupImpl group1 = new GroupImpl();
        group1.setName("Group 1");
        group1.setMembers(Arrays.asList("Member 1", "Member 2"));
        GroupImpl group2 = new GroupImpl();
        group2.setName("Group 2");
        group2.setMembers(Arrays.asList("Member 3", "Member 4"));
        propertyFilter.setGroups(Arrays.asList(group1, group2));

        // Check there are two groups returned
        Map<String, List<String>> groupMembership = propertyFilter.getGroupMembership();
        Assert.assertNotNull(groupMembership);
        Assert.assertEquals(2, groupMembership.size());

        // Check group 1 has the correct members (Usernames are uppercased in the back end)
        List<String> members = groupMembership.get("Group 1");
        Assert.assertNotNull(members);
        Assert.assertEquals(2, members.size());
        Assert.assertTrue(members.contains("MEMBER 1"));
        Assert.assertTrue(members.contains("MEMBER 2"));

        // Check group 2 has the correct members
        members = groupMembership.get("Group 2");
        Assert.assertNotNull(members);
        Assert.assertEquals(2, members.size());
        Assert.assertTrue(members.contains("MEMBER 3"));
        Assert.assertTrue(members.contains("MEMBER 4"));
    }
}
