package uk.co.agware.filter.test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import uk.co.agware.filter.PropertyFilter;
import uk.co.agware.filter.objects.Access;
import uk.co.agware.filter.objects.Permission;
import uk.co.agware.filter.objects.SecurityGroup;
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
        SecurityGroup group = new SecurityGroup();
        group.setName(groupName);
        group.setAccess(accessList);
        group.setMembers(Collections.singletonList(username));
        propertyFilter.load(Collections.singletonList(group));
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
        Assert.assertEquals(storedGroup.keySet().size(), 2);

        for(Map.Entry<String, Access> e : propertyFilter.getGroup(propertyFilter.getUsersGroup(username)).entrySet()){
            Assert.assertEquals(e.getValue().getAccess(), Access.Type.NO_ACCESS);
        }
    }

    @Test
    public void testGetAvailableClasses(){
        Assert.assertEquals(propertyFilter.getAccessibleClasses(username).size(), 0);

        FilterUtil.setDefaultAccessType(Access.Type.CREATE);
        List<Access> accessList = FilterUtil.getFullAccessList("uk.co.agware.filter.test.classes");
        SecurityGroup group = new SecurityGroup();
        group.setName(groupName);
        group.setAccess(accessList);
        group.setMembers(Collections.singletonList(username));
        propertyFilter.load(Collections.singletonList(group));

        Assert.assertEquals(propertyFilter.getAccessibleClasses(username).size(), 2);

        FilterUtil.setDefaultAccessType(Access.Type.READ);
        accessList = FilterUtil.getFullAccessList("uk.co.agware.filter.test.classes");
        group = new SecurityGroup();
        group.setName(groupName);
        group.setAccess(accessList);
        group.setMembers(Collections.singletonList(username));
        propertyFilter.load(Collections.singletonList(group));

        Assert.assertEquals(propertyFilter.getAccessibleClasses(username).size(), 2);

        FilterUtil.setDefaultAccessType(Access.Type.UPDATE);
        accessList = FilterUtil.getFullAccessList("uk.co.agware.filter.test.classes");
        group = new SecurityGroup();
        group.setName(groupName);
        group.setAccess(accessList);
        group.setMembers(Collections.singletonList(username));
        propertyFilter.load(Collections.singletonList(group));

        Assert.assertEquals(propertyFilter.getAccessibleClasses(username).size(), 2);
    }

    @Test
    public void testGetAccessibleFieldsFromObject(){
        List<Permission> permissions = propertyFilter.getAccessibleFields(new TestClass(), username);
        Assert.assertEquals(permissions.size(), 4);
        permissions = propertyFilter.getAccessibleFields(new SecondTestClass(), username);
        Assert.assertEquals(permissions.size(), 2);
    }

    @Test
    public void testGetAccessibleFieldsFromClass(){
        List<Permission> permissions = propertyFilter.getAccessibleFields(TestClass.class, username);
        Assert.assertEquals(permissions.size(), 4);
        permissions = propertyFilter.getAccessibleFields(SecondTestClass.class, username);
        Assert.assertEquals(permissions.size(), 2);
    }

    @Test
    public void testGetAccessibleFieldsFromClassName(){
        List<Permission> permissions = propertyFilter.getAccessibleFields(TestClass.class.getName(), username);
        Assert.assertEquals(permissions.size(), 4);
        permissions = propertyFilter.getAccessibleFields(SecondTestClass.class.getName(), username);
        Assert.assertEquals(permissions.size(), 2);
    }

    @Test
    public void testGetAccessFromObject(){
        Access access = propertyFilter.getAccessFromObject(new TestClass(), username);
        Assert.assertEquals(access.getObjectClass(), TestClass.class.getName());
    }

    @Test
    public void testGetAccessFromClass(){
        Access access = propertyFilter.getAccessFromClass(TestClass.class, username);
        Assert.assertEquals(access.getObjectClass(), TestClass.class.getName());
    }

    @Test
    public void testGetAccessFromClassName(){
        Access access = propertyFilter.getAccessFromClassName(TestClass.class.getName(), username);
        Assert.assertEquals(access.getObjectClass(), TestClass.class.getName());
    }
}
