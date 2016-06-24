package uk.co.agware.filter.test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import uk.co.agware.filter.data.AccessType;
import uk.co.agware.filter.data.IAccess;
import uk.co.agware.filter.data.PermissionType;
import uk.co.agware.filter.exceptions.FilterException;
import uk.co.agware.filter.impl.AccessImpl;
import uk.co.agware.filter.impl.DefaultClassFactory;
import uk.co.agware.filter.impl.PermissionImpl;
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

    private static AccessImpl readWriteAccessTest;
    private FilterUtil filterUtil;

    @BeforeClass
    public static void setUpBeforeClass(){
        List<PermissionImpl> permissions = new ArrayList<>();
        PermissionImpl readPermission = new PermissionImpl();
        readPermission.setPropertyName("testString");
        readPermission.setPermission(PermissionType.READ);
        permissions.add(readPermission);
        PermissionImpl writePermission = new PermissionImpl();
        writePermission.setPermission(PermissionType.WRITE);
        writePermission.setPropertyName("testBD");
        permissions.add(writePermission);
        PermissionImpl noAccessPermission = new PermissionImpl();
        noAccessPermission.setPermission(PermissionType.NO_ACCESS);
        noAccessPermission.setPropertyName("stringList");
        permissions.add(noAccessPermission);

        readWriteAccessTest = new AccessImpl();
        readWriteAccessTest.setAccess(AccessType.CREATE);
        readWriteAccessTest.setObjectClass("TestClass");
        readWriteAccessTest.setPermissions(permissions);
    }

    @Before
    public void setUp(){
        filterUtil = new FilterUtil(new DefaultClassFactory());
        filterUtil.setDefaultAccessType(AccessType.NO_ACCESS); // Just check we went back to default
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
        Assert.assertTrue(filterUtil.isFieldReadable("testString", readWriteAccessTest));
        Assert.assertTrue(filterUtil.isFieldReadable("testBD", readWriteAccessTest));
        Assert.assertFalse(filterUtil.isFieldReadable("stringList", readWriteAccessTest));
    }

    @Test(expected = FilterException.class)
    public void testNonDefinedReadPermission(){
        Assert.assertNull(filterUtil.isFieldReadable("notARealFiled", readWriteAccessTest));
    }

    @Test
    public void testFieldWritable(){
        Assert.assertTrue(filterUtil.isFieldWritable("testBD", readWriteAccessTest));
        Assert.assertFalse(filterUtil.isFieldWritable("testString", readWriteAccessTest));
        Assert.assertFalse(filterUtil.isFieldWritable("stringList", readWriteAccessTest));
    }

    @Test(expected = FilterException.class)
    public void testNonDefinedWritePermission(){
        Assert.assertNull(filterUtil.isFieldWritable("notARealFiled", readWriteAccessTest));
    }

    @Test
    public void testGetAllFieldsFromClass(){
        Set<Field> fields = filterUtil.getAllFields(TestClass.class);
        Assert.assertTrue(fields.size() == 4);
    }

    @Test
    public void testGetAllFieldsFromObject(){
        Set<Field> fields = filterUtil.getAllFields(new TestClass());
        Assert.assertTrue(fields.size() == 4);
    }

    @Test
    public void testGetAllClasses(){
        List<Class> classes = filterUtil.getAllClasses("uk.co.agware.filter.test.classes");
        Assert.assertEquals(7, classes.size());
        List<Class> availableClasses = filterUtil.getAllAvailableClasses(classes);
        Assert.assertEquals(5, availableClasses.size());
    }

    @Test
    public void testGetAllIgnoredClasses(){
        List<Class> classes = filterUtil.getAllIgnoredClasses("uk.co.agware.filter.test.classes");
        Assert.assertEquals(1, classes.size());
        Assert.assertTrue(classes.contains(IgnoredClass.class));
    }

    @Test
    public void testGetAllAvailableObjects(){
        List<IAccess> accessList = filterUtil.getFullAccessList("uk.co.agware.filter.test.classes");
        Assert.assertEquals(5, accessList.size());
    }

    @Test
    public void testBuildBaseAccessForClassNoAccess(){
        filterUtil.setDefaultAccessType(AccessType.NO_ACCESS);
        IAccess access = filterUtil.buildBaseAccess(TestClass.class);
        Assert.assertTrue(access.isModifiable());
        Assert.assertEquals(AccessType.NO_ACCESS, access.getAccess());
        access = filterUtil.buildBaseAccess(NoAccessClass.class);
        Assert.assertEquals(AccessType.NO_ACCESS, access.getAccess());
        Assert.assertFalse(access.isModifiable());
    }

    @Test
    public void testBuildBaseAccessForClassCreate(){
        filterUtil.setDefaultAccessType(AccessType.CREATE);
        IAccess access = filterUtil.buildBaseAccess(TestClass.class);
        Assert.assertEquals(AccessType.CREATE, access.getAccess());
        Assert.assertTrue(access.isModifiable());
        access = filterUtil.buildBaseAccess(NoAccessClass.class);
        Assert.assertEquals(AccessType.NO_ACCESS, access.getAccess());
        Assert.assertFalse(access.isModifiable());
    }

    @Test
    public void testBuildBaseAccessForClassRead(){
        filterUtil.setDefaultAccessType(AccessType.READ);
        IAccess access = filterUtil.buildBaseAccess(TestClass.class);
        Assert.assertTrue(access.isModifiable());
        Assert.assertEquals(AccessType.READ, access.getAccess());
        access = filterUtil.buildBaseAccess(NoAccessClass.class);
        Assert.assertEquals(AccessType.NO_ACCESS, access.getAccess());
        Assert.assertFalse(access.isModifiable());
    }

    @Test
    public void testBuildBaseAccessForClassUpdate(){
        filterUtil.setDefaultAccessType(AccessType.UPDATE);
        IAccess access = filterUtil.buildBaseAccess(TestClass.class);
        Assert.assertTrue(access.isModifiable());
        Assert.assertEquals(AccessType.UPDATE, access.getAccess());
        access = filterUtil.buildBaseAccess(NoAccessClass.class);
        Assert.assertEquals(AccessType.NO_ACCESS, access.getAccess());
        Assert.assertFalse(access.isModifiable());
    }

    @Test
    public void testPermissionAnnotationsWithNoAccess(){
        filterUtil.setDefaultPermissionType(PermissionType.NO_ACCESS);
        IAccess access = filterUtil.createDefaultAccessFromClass(NoAccessClass.class);
        Assert.assertTrue(access.getPermissions().contains(new PermissionImpl("id", PermissionType.NO_ACCESS, false)));
        Assert.assertTrue(access.getPermissions().contains(new PermissionImpl("name", PermissionType.READ, false)));
        Assert.assertTrue(access.getPermissions().contains(new PermissionImpl("email", PermissionType.WRITE, false)));
        Assert.assertTrue(access.getPermissions().contains(new PermissionImpl("other", PermissionType.NO_ACCESS, true)));
    }

    @Test
    public void testPermissionAnnotationsWithRead(){
        filterUtil.setDefaultPermissionType(PermissionType.READ);
        IAccess access = filterUtil.createDefaultAccessFromClass(NoAccessClass.class);
        Assert.assertTrue(access.getPermissions().contains(new PermissionImpl("id", PermissionType.NO_ACCESS, false)));
        Assert.assertTrue(access.getPermissions().contains(new PermissionImpl("name", PermissionType.READ, false)));
        Assert.assertTrue(access.getPermissions().contains(new PermissionImpl("email", PermissionType.WRITE, false)));
        Assert.assertTrue(access.getPermissions().contains(new PermissionImpl("other", PermissionType.READ, true)));
    }

    @Test
    public void testPermissionAnnotationsWithWrite(){
        filterUtil.setDefaultPermissionType(PermissionType.WRITE);
        IAccess access = filterUtil.createDefaultAccessFromClass(NoAccessClass.class);
        Assert.assertTrue(access.getPermissions().contains(new PermissionImpl("id", PermissionType.NO_ACCESS, false)));
        Assert.assertTrue(access.getPermissions().contains(new PermissionImpl("name", PermissionType.READ, false)));
        Assert.assertTrue(access.getPermissions().contains(new PermissionImpl("email", PermissionType.WRITE, false)));
        Assert.assertTrue(access.getPermissions().contains(new PermissionImpl("other", PermissionType.WRITE, true)));
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
        IAccess access = filterUtil.buildBaseAccess(NoAccessClass.class);
        Assert.assertEquals(AccessType.NO_ACCESS, access.getAccess());
        Assert.assertFalse(access.isModifiable());
    }

    @Test(expected = FilterException.class)
    public void testFailWhenNoAnnotation(){
        filterUtil.buildBaseAccess(ThrowsErrorDuringConstruct.class);
    }

    @Test
    public void testDisplayName(){
        IAccess access = filterUtil.buildBaseAccess(TestClass.class);
        Assert.assertEquals("Test Class", access.getDisplayName());
    }

    @Test
    public void testDefaultDisplayName(){
        IAccess access = filterUtil.buildBaseAccess(NoAccessClass.class);
        Assert.assertEquals(NoAccessClass.class.getName(), access.getDisplayName());
    }
}
