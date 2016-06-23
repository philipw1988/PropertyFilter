package uk.co.agware.filter.test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import uk.co.agware.filter.exceptions.FilterException;
import uk.co.agware.filter.objects.Access;
import uk.co.agware.filter.objects.Permission;
import uk.co.agware.filter.test.classes.*;
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

    @Before
    public void setUp(){
        FilterUtil.setDefaultAccessType(Access.Type.NO_ACCESS); // Just check we went back to default
    }

    @Test
    public void testInstantiateObject(){
        Assert.assertNotNull(FilterUtil.instantiateObject(TestClass.class));
    }

    @Test(expected = FilterException.class)
    public void testNoDefaultConstructor(){
        Assert.assertNull(FilterUtil.instantiateObject(NoDefaultConstructor.class));
    }

    @Test(expected = FilterException.class)
    public void testPrivateConstructor(){
        Assert.assertNull(FilterUtil.instantiateObject(NoPublicConstructor.class));
    }

    @Test(expected = FilterException.class)
    public void testThrowingConstructor(){
        Assert.assertNull(FilterUtil.instantiateObject(ThrowsErrorDuringConstruct.class));
    }

    @Test
    public void checkNullSafe(){
        Assert.assertNotNull(FilterUtil.nullSafe(new ArrayList<>()));
        Assert.assertNotNull(FilterUtil.nullSafe(null));
    }

    @Test
    public void testFieldReadable(){
        Assert.assertTrue(FilterUtil.isFieldReadable("testString", readWriteAccessTest));
        Assert.assertTrue(FilterUtil.isFieldReadable("testBD", readWriteAccessTest));
        Assert.assertFalse(FilterUtil.isFieldReadable("stringList", readWriteAccessTest));
    }

    @Test(expected = FilterException.class)
    public void testNonDefinedReadPermission(){
        Assert.assertNull(FilterUtil.isFieldReadable("notARealFiled", readWriteAccessTest));
    }

    @Test
    public void testFieldWritable(){
        Assert.assertTrue(FilterUtil.isFieldWritable("testBD", readWriteAccessTest));
        Assert.assertFalse(FilterUtil.isFieldWritable("testString", readWriteAccessTest));
        Assert.assertFalse(FilterUtil.isFieldWritable("stringList", readWriteAccessTest));
    }

    @Test(expected = FilterException.class)
    public void testNonDefinedWritePermission(){
        Assert.assertNull(FilterUtil.isFieldWritable("notARealFiled", readWriteAccessTest));
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
        Assert.assertEquals(7, classes.size());
        List<Class> availableClasses = FilterUtil.getAllAvailableClasses(classes);
        Assert.assertEquals(5, availableClasses.size());
    }

    @Test
    public void testGetAllIgnoredClasses(){
        List<Class> classes = FilterUtil.getAllIgnoredClasses("uk.co.agware.filter.test.classes");
        Assert.assertEquals(1, classes.size());
        Assert.assertTrue(classes.contains(IgnoredClass.class));
    }

    @Test
    public void testGetAllAvailableObjects(){
        List<Access> accessList = FilterUtil.getFullAccessList("uk.co.agware.filter.test.classes");
        Assert.assertEquals(5, accessList.size());
    }

    @Test
    public void testBuildBaseAccessForClassNoAccess(){
        FilterUtil.setDefaultAccessType(Access.Type.NO_ACCESS);
        Access access = FilterUtil.buildBaseAccess(TestClass.class);
        Assert.assertTrue(access.isModifiable());
        Assert.assertEquals(Access.Type.NO_ACCESS, access.getAccess());
        access = FilterUtil.buildBaseAccess(NoAccessClass.class);
        Assert.assertEquals(Access.Type.NO_ACCESS, access.getAccess());
        Assert.assertFalse(access.isModifiable());
    }

    @Test
    public void testBuildBaseAccessForClassCreate(){
        FilterUtil.setDefaultAccessType(Access.Type.CREATE);
        Access access = FilterUtil.buildBaseAccess(TestClass.class);
        Assert.assertEquals(Access.Type.CREATE, access.getAccess());
        Assert.assertTrue(access.isModifiable());
        access = FilterUtil.buildBaseAccess(NoAccessClass.class);
        Assert.assertEquals(Access.Type.NO_ACCESS, access.getAccess());
        Assert.assertFalse(access.isModifiable());
    }

    @Test
    public void testBuildBaseAccessForClassRead(){
        FilterUtil.setDefaultAccessType(Access.Type.READ);
        Access access = FilterUtil.buildBaseAccess(TestClass.class);
        Assert.assertTrue(access.isModifiable());
        Assert.assertEquals(Access.Type.READ, access.getAccess());
        access = FilterUtil.buildBaseAccess(NoAccessClass.class);
        Assert.assertEquals(Access.Type.NO_ACCESS, access.getAccess());
        Assert.assertFalse(access.isModifiable());
    }

    @Test
    public void testBuildBaseAccessForClassUpdate(){
        FilterUtil.setDefaultAccessType(Access.Type.UPDATE);
        Access access = FilterUtil.buildBaseAccess(TestClass.class);
        Assert.assertTrue(access.isModifiable());
        Assert.assertEquals(Access.Type.UPDATE, access.getAccess());
        access = FilterUtil.buildBaseAccess(NoAccessClass.class);
        Assert.assertEquals(Access.Type.NO_ACCESS, access.getAccess());
        Assert.assertFalse(access.isModifiable());
    }

    @Test
    public void testPermissionAnnotationsWithNoAccess(){
        FilterUtil.setDefaultPermissionType(Permission.Type.NO_ACCESS);
        Access access = FilterUtil.createDefaultAccessFromClass(NoAccessClass.class);
        Assert.assertTrue(access.getPermissions().contains(new Permission("id", Permission.Type.NO_ACCESS, false)));
        Assert.assertTrue(access.getPermissions().contains(new Permission("name", Permission.Type.READ, false)));
        Assert.assertTrue(access.getPermissions().contains(new Permission("email", Permission.Type.WRITE, false)));
        Assert.assertTrue(access.getPermissions().contains(new Permission("other", Permission.Type.NO_ACCESS, true)));
    }

    @Test
    public void testPermissionAnnotationsWithRead(){
        FilterUtil.setDefaultPermissionType(Permission.Type.READ);
        Access access = FilterUtil.createDefaultAccessFromClass(NoAccessClass.class);
        Assert.assertTrue(access.getPermissions().contains(new Permission("id", Permission.Type.NO_ACCESS, false)));
        Assert.assertTrue(access.getPermissions().contains(new Permission("name", Permission.Type.READ, false)));
        Assert.assertTrue(access.getPermissions().contains(new Permission("email", Permission.Type.WRITE, false)));
        Assert.assertTrue(access.getPermissions().contains(new Permission("other", Permission.Type.READ, true)));
    }

    @Test
    public void testPermissionAnnotationsWithWrite(){
        FilterUtil.setDefaultPermissionType(Permission.Type.WRITE);
        Access access = FilterUtil.createDefaultAccessFromClass(NoAccessClass.class);
        Assert.assertTrue(access.getPermissions().contains(new Permission("id", Permission.Type.NO_ACCESS, false)));
        Assert.assertTrue(access.getPermissions().contains(new Permission("name", Permission.Type.READ, false)));
        Assert.assertTrue(access.getPermissions().contains(new Permission("email", Permission.Type.WRITE, false)));
        Assert.assertTrue(access.getPermissions().contains(new Permission("other", Permission.Type.WRITE, true)));
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
        FilterUtil.buildBaseAccess(ThrowsErrorDuringConstruct.class);
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
