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
import uk.co.agware.filter.util.ClassUtil;

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
    private Integer secondTestInt1 = 2;
    private SecondTestClass secondTestClass2;
    private String secondTestName2 = "Second Test 2";
    private Integer secondTestInt2 = 12;
    private SecondTestClass secondTestClass3;
    private String secondTestName3 = "Second Test 3";
    private Integer secondTestInt3 = 22;

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
    }

    @Test
    public void testReturnNoAccess(){
        propertyFilter = new PropertyFilter();
        ClassUtil.setDefaultAccessType(Access.Type.NO_ACCESS);
        ClassUtil.setDefaultPermissionType(Permission.Type.NO_ACCESS);
        List<Access> accessList = ClassUtil.getFullAccessList("uk.co.agware.filter.test.classes");
        SecurityGroup group = new SecurityGroup();
        group.setName(groupName);
        group.setAccess(accessList);
        group.setMembers(Collections.singletonList(username));
        propertyFilter.load(Collections.singletonList(group));

        Object o = propertyFilter.parseObjectForReturn(testClass, username);
        Assert.assertNull(o);
    }

    @Test
    public void testReturnNoAccessWritePermission(){
        propertyFilter = new PropertyFilter();
        ClassUtil.setDefaultAccessType(Access.Type.NO_ACCESS);
        ClassUtil.setDefaultPermissionType(Permission.Type.WRITE);
        List<Access> accessList = ClassUtil.getFullAccessList("uk.co.agware.filter.test.classes");
        SecurityGroup group = new SecurityGroup();
        group.setName(groupName);
        group.setAccess(accessList);
        group.setMembers(Collections.singletonList(username));
        propertyFilter.load(Collections.singletonList(group));

        Object o = propertyFilter.parseObjectForReturn(testClass, username);
        Assert.assertNull(o);
    }

    @Test
    public void testReturnReadAccessNoPermission(){
        propertyFilter = new PropertyFilter();
        ClassUtil.setDefaultAccessType(Access.Type.READ);
        ClassUtil.setDefaultPermissionType(Permission.Type.NO_ACCESS);
        List<Access> accessList = ClassUtil.getFullAccessList("uk.co.agware.filter.test.classes");
        SecurityGroup group = new SecurityGroup();
        group.setName(groupName);
        group.setAccess(accessList);
        group.setMembers(Collections.singletonList(username));
        propertyFilter.load(Collections.singletonList(group));

        TestClass t = propertyFilter.parseObjectForReturn(testClass, username);
        Assert.assertNotNull(t);
        Assert.assertNull(t.getTestBD());
        Assert.assertNull(t.getSecondTestClasses());
        Assert.assertNull(t.getStringList());
    }

    @Test
    public void testReturnReadAccessReadPermission(){
        propertyFilter = new PropertyFilter();
        ClassUtil.setDefaultAccessType(Access.Type.READ);
        ClassUtil.setDefaultPermissionType(Permission.Type.READ);
        List<Access> accessList = ClassUtil.getFullAccessList("uk.co.agware.filter.test.classes");
        SecurityGroup group = new SecurityGroup();
        group.setName(groupName);
        group.setAccess(accessList);
        group.setMembers(Collections.singletonList(username));
        propertyFilter.load(Collections.singletonList(group));

        TestClass t = propertyFilter.parseObjectForReturn(testClass, username);
        Assert.assertNotNull(t);
        Assert.assertEquals(t.getTestBD(), testBD1);
        for(String s : t.getStringList()){
            Assert.assertTrue(testClass.getStringList().contains(s));
        }
        for(SecondTestClass s : t.getSecondTestClasses()){
            Assert.assertTrue(t.getSecondTestClasses().contains(s));
        }
    }
}
