package uk.co.agware.filter.test;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import uk.co.agware.filter.data.AccessType;
import uk.co.agware.filter.data.IAccess;
import uk.co.agware.filter.data.IPermission;
import uk.co.agware.filter.data.PermissionType;
import uk.co.agware.filter.impl.AccessImpl;
import uk.co.agware.filter.impl.DefaultClassFactory;
import uk.co.agware.filter.impl.PermissionImpl;
import uk.co.agware.filter.test.classes.TestClass;

import java.util.Collections;
import java.util.List;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 24/06/2016.
 */
public class TestDefaultClasses extends Assert {

    private static DefaultClassFactory defaultClassFactory;

    @BeforeClass
    public static void setUp(){
        defaultClassFactory = new DefaultClassFactory();
    }

    @Test
    public void testCreateNewAccess(){
        IAccess access = defaultClassFactory.createAccessClass();
        assertNotNull(access);
        assertEquals(AccessImpl.class, access.getClass());
    }

    @Test
    public void testCopyAccess(){
        AccessImpl impl1 = defaultClassFactory.createAccessClass();
        impl1.setDisplayName("Impl 1");
        impl1.setObjectClass(TestClass.class.getName());
        impl1.setModifiable(true);
        impl1.setAccess(AccessType.CREATE);
        impl1.setPermissions(Collections.emptyList());

        AccessImpl newImpl = defaultClassFactory.copyAccessClass(impl1);
        assertEquals(impl1, newImpl);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCopyNullAccess(){
        defaultClassFactory.copyAccessClass(null);
    }

    /* Testing the permissions are a new copied list rather than the same list referenced twice */
    @Test
    public void testPermissionsCopy(){
        List<PermissionImpl> permissions = Collections.singletonList(new PermissionImpl("property", PermissionType.READ, false));
        AccessImpl impl1 = defaultClassFactory.createAccessClass();
        impl1.setPermissions(permissions);

        AccessImpl newAccess = defaultClassFactory.copyAccessClass(impl1);
        assertEquals(1, newAccess.getPermissions().size());
        assertFalse(impl1.getPermissions() == newAccess.getPermissions());
        assertEquals(impl1.getPermissions().get(0), newAccess.getPermissions().get(0));
        assertFalse(impl1.getPermissions().get(0) == newAccess.getPermissions().get(0));
    }

    @Test
    public void testCreatePermission(){
        IPermission permission = defaultClassFactory.createPermissionClass();
        assertNotNull(permission);
        assertEquals(PermissionImpl.class, permission.getClass());
    }

    @Test
    public void testCopyPermission(){
        PermissionImpl perm1 = defaultClassFactory.createPermissionClass();
        perm1.setDisplayName("Perm 1");
        perm1.setPropertyName("field");
        perm1.setModifiable(true);
        perm1.setPermission(PermissionType.READ);

        PermissionImpl newPerm = defaultClassFactory.copyPermissionClass(perm1);
        assertEquals(perm1, newPerm);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCopyNullPermission(){
        defaultClassFactory.copyPermissionClass(null);
    }
}
