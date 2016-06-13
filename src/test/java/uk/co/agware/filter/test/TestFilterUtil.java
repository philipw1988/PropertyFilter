package uk.co.agware.filter.test;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import uk.co.agware.filter.exceptions.FilterException;
import uk.co.agware.filter.objects.Access;
import uk.co.agware.filter.objects.Permission;
import uk.co.agware.filter.test.classes.NoAccessClass;
import uk.co.agware.filter.test.classes.NoDefaultConstructor;
import uk.co.agware.filter.test.classes.TestClass;
import uk.co.agware.filter.util.FilterUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 10/04/2016.
 */
public class TestFilterUtil {

    private static Access readWriteAccessTest;

    @BeforeClass
    public static void setUpBeforeClass(){
        List<Permission> permissions = new ArrayList<>();
        Permission readPermission = new Permission();
        readPermission.setPropertyName("testString");
        readPermission.setPermission(Permission.Type.READ);
        permissions.add(readPermission);
        Permission writePermission = new Permission();
        writePermission.setPermission(Permission.Type.WRITE);
        writePermission.setPropertyName("testBD");
        permissions.add(writePermission);
        Permission noAccessPermission = new Permission();
        noAccessPermission.setPermission(Permission.Type.NO_ACCESS);
        noAccessPermission.setPropertyName("stringList");
        permissions.add(noAccessPermission);

        readWriteAccessTest = new Access();
        readWriteAccessTest.setAccess(Access.Type.CREATE);
        readWriteAccessTest.setObjectClass("TestClass");
        readWriteAccessTest.setPermissions(permissions);
    }

    @Test
    public void testInstantiateObject(){
        Assert.assertNotNull(FilterUtil.instantiateObject(TestClass.class));
    }

    @Test(expected = FilterException.class)
    public void testInstantiateFails(){
        Assert.assertNull(FilterUtil.instantiateObject(NoDefaultConstructor.class));
    }

    @Test
    public void testFieldReadable(){
        Assert.assertTrue(FilterUtil.isFieldReadable("testString", readWriteAccessTest));
        Assert.assertTrue(FilterUtil.isFieldReadable("testBD", readWriteAccessTest));
        Assert.assertFalse(FilterUtil.isFieldReadable("stringList", readWriteAccessTest));
    }

    @Test
    public void testFieldWritable(){
        Assert.assertTrue(FilterUtil.isFieldWritable("testBD", readWriteAccessTest));
        Assert.assertFalse(FilterUtil.isFieldWritable("testString", readWriteAccessTest));
        Assert.assertFalse(FilterUtil.isFieldWritable("stringList", readWriteAccessTest));
    }

    @Test
    public void testGetAllFieldsFromClass(){
        Set<Field> fields = FilterUtil.getAllFields(TestClass.class);
        Assert.assertTrue(fields.size() == 4);
    }

    @Test
    public void testGetAllFieldsFromObject(){
        Set<Field> fields = FilterUtil.getAllFields(new TestClass());
        Assert.assertTrue(fields.size() == 4);
    }

    @Test
    public void testGetAllClasses(){
        List<Class> classes = FilterUtil.getAllClasses("uk.co.agware.filter.test.classes");
        Assert.assertEquals(4, classes.size());
        List<Class> nonHiddenClasses = FilterUtil.getAllAvailableClasses(classes);
        Assert.assertEquals(3, nonHiddenClasses.size());
    }

    @Test
    public void testGetAllAvailableObjects(){
        List<Access> accessList = FilterUtil.getFullAccessList("uk.co.agware.filter.test.classes");
        Assert.assertEquals(3, accessList.size());
    }

    @Test
    public void testGetDefaultAccessForClass(){
        FilterUtil.setDefaultAccessType(Access.Type.NO_ACCESS);
        Access access = FilterUtil.createDefaultAccessFromClass(TestClass.class);
        Assert.assertEquals(access.getAccess(), Access.Type.NO_ACCESS);

        FilterUtil.setDefaultAccessType(Access.Type.CREATE);
        access = FilterUtil.createDefaultAccessFromClass(TestClass.class);
        Assert.assertEquals(access.getAccess(), Access.Type.CREATE);

        Assert.assertEquals(access.getPermissions().size(), 4);
        for(Permission p : access.getPermissions()){
            if (p.getPropertyName().equals("id")){
                Assert.assertEquals(p.getPermission(), Permission.Type.READ);
            }
            else {
                Assert.assertEquals(p.getPermission(), Permission.Type.NO_ACCESS);
            }
        }

        FilterUtil.setDefaultPermissionType(Permission.Type.WRITE);
        access = FilterUtil.createDefaultAccessFromClass(TestClass.class);
        for(Permission p : access.getPermissions()){
            if (p.getPropertyName().equals("id")){
                Assert.assertEquals(p.getPermission(), Permission.Type.READ);
            }
            else {
                Assert.assertEquals(p.getPermission(), Permission.Type.WRITE);
            }
        }
    }

    @Test
    public void testCapitalizeFirst(){
        Assert.assertNull(FilterUtil.capitalizeFirst(null));
        Assert.assertNotNull(FilterUtil.capitalizeFirst(""));
        Assert.assertEquals(FilterUtil.capitalizeFirst("a"), "A");
        Assert.assertEquals(FilterUtil.capitalizeFirst("A"), "A");
        Assert.assertEquals(FilterUtil.capitalizeFirst("1"), "1");
        Assert.assertEquals(FilterUtil.capitalizeFirst("hello"), "Hello");
        Assert.assertEquals(FilterUtil.capitalizeFirst("hello world"), "Hello world");
    }

    @Test
    public void testBuildDisplayName(){
        Assert.assertEquals(FilterUtil.buildDisplayName("hello"), "Hello");
        Assert.assertEquals(FilterUtil.buildDisplayName("Hello"), "Hello");
        Assert.assertEquals(FilterUtil.buildDisplayName("helloWorld"), "Hello World");
    }

    @Test
    public void testBuildNoAccessFromClass(){
        Access access = FilterUtil.buildBaseAccess(NoAccessClass.class);
        Assert.assertEquals(Access.Type.NO_ACCESS, access.getAccess());
        Assert.assertFalse(access.isModifiable());
    }

    @Test(expected = FilterException.class)
    public void testFailWhenNoAnnotation(){
        FilterUtil.buildBaseAccess(NoDefaultConstructor.class);
    }

    @Test
    public void testDisplayName(){
        Access access = FilterUtil.buildBaseAccess(TestClass.class);
        Assert.assertEquals("Test Class", access.getDisplayName());
    }

    @Test
    public void testDefaultDisplayName(){
        Access access = FilterUtil.buildBaseAccess(NoAccessClass.class);
        Assert.assertEquals(NoAccessClass.class.getName(), access.getDisplayName());
    }
}
