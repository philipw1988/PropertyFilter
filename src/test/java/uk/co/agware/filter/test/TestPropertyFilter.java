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

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 9/04/2016.
 */
public class TestPropertyFilter {

    private PropertyFilter propertyFilter;

    private String username = "test";
    private String groupName = "Test Group";

    @Before
    public void setUp(){
        propertyFilter = new PropertyFilter();

        FilterUtil.setDefaultAccessType(Access.Type.NO_ACCESS);
        List<Access> accessList = FilterUtil.getFullAccessList("uk.co.agware.filter.test.classes");
        Group group = new Group();
        group.setName(groupName);
        group.setAccess(accessList);
        group.setMembers(Collections.singletonList(username));
        propertyFilter.setGroups(Collections.singletonList(group));
    }

    @Test
    public void testGetUsersGroup(){
        Assert.assertEquals(groupName, propertyFilter.getUsersGroup(username));
        Assert.assertEquals(groupName, propertyFilter.getUsersGroup("TEST"));
        Assert.assertEquals(groupName, propertyFilter.getUsersGroup("Test"));
        Assert.assertEquals(groupName, propertyFilter.getUsersGroup("TesT"));
    }

    @Test
    public void testCollectionClasses(){
        Assert.assertTrue(propertyFilter.collectionClassesContains(String.class));
        propertyFilter.addCollectionClass(Short.class);
        Assert.assertTrue(propertyFilter.collectionClassesContains(Short.class));
        Assert.assertTrue(propertyFilter.removeCollectionClass(Short.class));
        Assert.assertFalse(propertyFilter.collectionClassesContains(Short.class));
    }

    @Test
    public void testLoad(){
        Assert.assertEquals(propertyFilter.getUsersGroup(username), groupName);
        Map<String, Access> storedGroup = propertyFilter.getGroup(propertyFilter.getUsersGroup(username));
        Assert.assertEquals(2, storedGroup.keySet().size());

        for(Map.Entry<String, Access> e : propertyFilter.getGroup(propertyFilter.getUsersGroup(username)).entrySet()){
            Assert.assertEquals(Access.Type.NO_ACCESS, e.getValue().getAccess());
        }
    }

    @Test
    public void testGetAvailableClasses(){
        Assert.assertEquals(0, propertyFilter.getAccessibleClasses(username).size());

        FilterUtil.setDefaultAccessType(Access.Type.CREATE);
        List<Access> accessList = FilterUtil.getFullAccessList("uk.co.agware.filter.test.classes");
        Group group = new Group();
        group.setName(groupName);
        group.setAccess(accessList);
        group.setMembers(Collections.singletonList(username));
        propertyFilter.setGroups(Collections.singletonList(group));

        Assert.assertEquals(2, propertyFilter.getAccessibleClasses(username).size());

        FilterUtil.setDefaultAccessType(Access.Type.READ);
        accessList = FilterUtil.getFullAccessList("uk.co.agware.filter.test.classes");
        group = new Group();
        group.setName(groupName);
        group.setAccess(accessList);
        group.setMembers(Collections.singletonList(username));
        propertyFilter.setGroups(Collections.singletonList(group));

        Assert.assertEquals(2, propertyFilter.getAccessibleClasses(username).size());

        FilterUtil.setDefaultAccessType(Access.Type.UPDATE);
        accessList = FilterUtil.getFullAccessList("uk.co.agware.filter.test.classes");
        group = new Group();
        group.setName(groupName);
        group.setAccess(accessList);
        group.setMembers(Collections.singletonList(username));
        propertyFilter.setGroups(Collections.singletonList(group));

        Assert.assertEquals(2, propertyFilter.getAccessibleClasses(username).size());
    }

    @Test
    public void testGetAccessibleClassesFromGroup(){
        FilterUtil.setDefaultAccessType(Access.Type.CREATE);
        List<Access> accessList = FilterUtil.getFullAccessList("uk.co.agware.filter.test.classes");
        Group group = new Group();
        group.setName(groupName);
        group.setAccess(accessList);
        group.setMembers(Collections.singletonList(username));
        propertyFilter.setGroups(Collections.singletonList(group));

        String groupName = propertyFilter.getUsersGroup(username);
        List<String> availableClasses = propertyFilter.getAccessibleClassesForGroup(groupName);
        Assert.assertEquals(2, availableClasses.size());
    }

    @Test
    public void testGetAccessibleFieldsFromObject(){
        List<Permission> permissions = propertyFilter.getAccessibleFields(new TestClass(), username);
        Assert.assertEquals(4, permissions.size());
        permissions = propertyFilter.getAccessibleFields(new SecondTestClass(), username);
        Assert.assertEquals(2, permissions.size());
    }

    @Test
    public void testGetAccessibleFieldsFromClass(){
        List<Permission> permissions = propertyFilter.getAccessibleFields(TestClass.class, username);
        Assert.assertEquals(4, permissions.size());
        permissions = propertyFilter.getAccessibleFields(SecondTestClass.class, username);
        Assert.assertEquals(2, permissions.size());
    }

    @Test
    public void testGetAccessibleFieldsFromClassName(){
        List<Permission> permissions = propertyFilter.getAccessibleFields(TestClass.class.getName(), username);
        Assert.assertEquals(4, permissions.size());
        permissions = propertyFilter.getAccessibleFields(SecondTestClass.class.getName(), username);
        Assert.assertEquals(2, permissions.size());
    }

    @Test
    public void testGetAccessibleFieldsFromGroup(){
        String groupName = propertyFilter.getUsersGroup(username);
        List<Permission> permissions = propertyFilter.getAccessibleFieldsForGroup(TestClass.class, groupName);
        Assert.assertEquals(4, permissions.size());
        permissions = propertyFilter.getAccessibleFieldsForGroup(TestClass.class.getName(), groupName);
        Assert.assertEquals(4, permissions.size());
        permissions = propertyFilter.getAccessibleFieldsForGroup(new TestClass(), groupName);
        Assert.assertEquals(4, permissions.size());
    }

    @Test
    public void testGetAccessFromObject(){
        Access access = propertyFilter.getAccess(new TestClass(), username);
        Assert.assertEquals(TestClass.class.getName(), access.getObjectClass());
    }

    @Test
    public void testGetAccessFromClass(){
        Access access = propertyFilter.getAccess(TestClass.class, username);
        Assert.assertEquals(TestClass.class.getName(), access.getObjectClass());
    }

    @Test
    public void testGetAccessFromClassName(){
        Access access = propertyFilter.getAccess(TestClass.class.getName(), username);
        Assert.assertEquals(TestClass.class.getName(), access.getObjectClass());
    }

    @Test
    public void testGetAccessFromObjectFromGroup(){
        Access access = propertyFilter.getAccessForGroup(new TestClass(), propertyFilter.getUsersGroup(username));
        Assert.assertEquals(TestClass.class.getName(), access.getObjectClass());
    }

    @Test
    public void testGetAccessFromClassFromGroup(){
        Access access = propertyFilter.getAccessForGroup(TestClass.class, propertyFilter.getUsersGroup(username));
        Assert.assertEquals(TestClass.class.getName(), access.getObjectClass());
    }

    @Test
    public void testGetAccessFromClassNameFromGroup(){
        Access access = propertyFilter.getAccessForGroup(TestClass.class.getName(), propertyFilter.getUsersGroup(username));
        Assert.assertEquals(TestClass.class.getName(), access.getObjectClass());
    }

    @Test
    public void testAddUserToGroup(){
        propertyFilter.addUserToGroup("Test Username", groupName);
        Assert.assertEquals(groupName, propertyFilter.getUsersGroup("Test Username"));
    }
}
