package uk.co.agware.filter.test;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import uk.co.agware.filter.objects.Access;
import uk.co.agware.filter.objects.Permission;
import uk.co.agware.filter.test.classes.NoDefaultConstructor;
import uk.co.agware.filter.test.classes.TestClass;
import uk.co.agware.filter.util.ClassUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 10/04/2016.
 */
public class TestClassUtil {

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
        Assert.assertNotNull(ClassUtil.instantiateObject(TestClass.class));
        Assert.assertNull(ClassUtil.instantiateObject(NoDefaultConstructor.class));
    }

    @Test
    public void testFieldReadable(){
        Assert.assertTrue(ClassUtil.isFieldReadable("testString", readWriteAccessTest));
        Assert.assertTrue(ClassUtil.isFieldReadable("testBD", readWriteAccessTest));
        Assert.assertFalse(ClassUtil.isFieldReadable("stringList", readWriteAccessTest));
    }

    @Test
    public void testFieldWritable(){
        Assert.assertTrue(ClassUtil.isFieldWritable("testBD", readWriteAccessTest));
        Assert.assertFalse(ClassUtil.isFieldWritable("testString", readWriteAccessTest));
        Assert.assertFalse(ClassUtil.isFieldWritable("stringList", readWriteAccessTest));
    }

    @Test
    public void testGetAllFieldsFromClass(){
        Set<Field> fields = ClassUtil.getAllFields(TestClass.class);
        Assert.assertTrue(fields.size() == 4);
    }

    @Test
    public void testGetAllFieldsFromObject(){
        Set<Field> fields = ClassUtil.getAllFields(new TestClass());
        Assert.assertTrue(fields.size() == 4);
    }

    @Test
    public void testGetAllClasses(){
        List<Class> classes = ClassUtil.getAllClasses("uk.co.agware.filter.test.classes");
        Assert.assertTrue(classes.size() == 3);
        List<Class> nonHiddenClasses = ClassUtil.getAllNonHiddenClasses(classes);
        Assert.assertTrue(nonHiddenClasses.size() == 2);
    }

    @Test
    public void testGetAllNonHiddenObjects(){
        List<Access> accessList = ClassUtil.getFullAccessList("uk.co.agware.filter.test.classes");
        Assert.assertTrue(accessList.size() == 2);
    }

    @Test
    public void testGetDefaultAccessForClass(){
        Access access = ClassUtil.createDefaultAccessFromClass(TestClass.class);
        Assert.assertEquals(access.getAccess(), Access.Type.NO_ACCESS);

        ClassUtil.setDefaultAccessType(Access.Type.CREATE);
        access = ClassUtil.createDefaultAccessFromClass(TestClass.class);
        Assert.assertEquals(access.getAccess(), Access.Type.CREATE);

        Assert.assertEquals(access.getPermissions().size(), 4);
        for(Permission p : access.getPermissions()){
            if (p.getPropertyName().equals("testString")){
                Assert.assertEquals(p.getPermission(), Permission.Type.READ);
            }
            else {
                Assert.assertEquals(p.getPermission(), Permission.Type.NO_ACCESS);
            }
        }

        ClassUtil.setDefaultPermissionType(Permission.Type.WRITE);
        access = ClassUtil.createDefaultAccessFromClass(TestClass.class);
        for(Permission p : access.getPermissions()){
            if (p.getPropertyName().equals("testString")){
                Assert.assertEquals(p.getPermission(), Permission.Type.READ);
            }
            else {
                Assert.assertEquals(p.getPermission(), Permission.Type.WRITE);
            }
        }
    }

    @Test
    public void testCapitalizeFirst(){
        Assert.assertNull(ClassUtil.capitalizeFirst(null));
        Assert.assertNotNull(ClassUtil.capitalizeFirst(""));
        Assert.assertEquals(ClassUtil.capitalizeFirst("a"), "A");
        Assert.assertEquals(ClassUtil.capitalizeFirst("A"), "A");
        Assert.assertEquals(ClassUtil.capitalizeFirst("1"), "1");
        Assert.assertEquals(ClassUtil.capitalizeFirst("hello"), "Hello");
        Assert.assertEquals(ClassUtil.capitalizeFirst("hello world"), "Hello world");
    }

    @Test
    public void testBuildDisplayName(){
        Assert.assertEquals(ClassUtil.buildDisplayName("hello"), "Hello");
        Assert.assertEquals(ClassUtil.buildDisplayName("Hello"), "Hello");
        Assert.assertEquals(ClassUtil.buildDisplayName("helloWorld"), "Hello World");
    }
}
