package uk.co.agware.filter.test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import uk.co.agware.filter.PropertyFilter;
import uk.co.agware.filter.data.*;
import uk.co.agware.filter.impl.AccessImpl;
import uk.co.agware.filter.impl.DefaultClassFactory;
import uk.co.agware.filter.impl.GroupImpl;
import uk.co.agware.filter.impl.PermissionImpl;
import uk.co.agware.filter.persistence.FilterRepository;
import uk.co.agware.filter.service.FilterService;
import uk.co.agware.filter.service.ServiceBuilder;
import uk.co.agware.filter.test.classes.IgnoredClass;
import uk.co.agware.filter.test.classes.SecondTestClass;
import uk.co.agware.filter.test.classes.TestClass;
import uk.co.agware.filter.util.FilterUtil;

import java.util.*;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 24/06/2016.
 */
public class TestFilterService extends Mockito {

    @Captor
    private ArgumentCaptor<List<GroupImpl>> groupsCaptor;

    private FilterUtil filterUtil;
    private PropertyFilter propertyFilter;
    private FilterRepository filterRepository;
    private static FilterService filterService;

    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);

        filterUtil = mock(FilterUtil.class);
        propertyFilter = mock(PropertyFilter.class);
        when(propertyFilter.getFilterUtil()).thenReturn(filterUtil);
        filterRepository = mock(FilterRepository.class);
        when(filterRepository.initGroups()).thenReturn(Collections.singletonList(getTestGroup()));
        when(filterRepository.getGroups()).thenReturn(Collections.singletonList(getTestGroup()));

        Map<String, String> extraGroupMappings = new HashMap<>();
        extraGroupMappings.put("user 1", "group 1");

        filterService = new ServiceBuilder(propertyFilter)
                .withRepository(filterRepository)
                .addPackageToScan("uk.co.agware.filter.test")
                .addStaticGroupAllocations(extraGroupMappings)
                .build();
    }

    @Test
    public void testInitCallsToRepository(){
        filterService.init();
        verify(filterRepository).initGroups();
        verify(filterRepository).save(anyObject());
    }

    @Test
    public void testInitAddsIgnoredClasses(){
        when(filterUtil.getAllIgnoredClasses(anyString())).thenReturn(Collections.singletonList(IgnoredClass.class));
        filterService.init();
        verify(propertyFilter).addIgnoredClass(IgnoredClass.class);
    }

    @Test
    public void testInitAddsExtraMappings(){
        filterService.init();
        verify(propertyFilter).addUserToGroup("user 1", "group 1");
    }

    @Test
    public void testInitSetsGroups(){
        filterService.init();
        verify(propertyFilter).setGroups(groupsCaptor.capture());
        List<GroupImpl> groups = groupsCaptor.getValue();
        Assert.assertEquals(1, groups.size());
    }

    @Test
    public void testAddNewAccess(){
        // When init is called, the service will get an access list with an additional access object over the test group
        when(filterUtil.getFullAccessList(anyString())).thenReturn(getTestGroupWithExtraAccess().getAccess());
        filterService.init();
        ArgumentCaptor<Group> groupArgument = ArgumentCaptor.forClass(Group.class);
        verify(filterRepository).save(groupArgument.capture());
        Group savedGroup = groupArgument.getValue();
        Assert.assertNotNull(savedGroup);
        Assert.assertEquals(3, savedGroup.getAccess().size()); // Should have an extra access over the original one
        Assert.assertTrue(savedGroup.getAccess().contains(new AccessImpl(TestClass.class.getName(), AccessType.NO_ACCESS, true)));
        Assert.assertTrue(savedGroup.getAccess().contains(new AccessImpl(SecondTestClass.class.getName(), AccessType.READ, false)));
        Assert.assertTrue(savedGroup.getAccess().contains(new AccessImpl("new Object", AccessType.CREATE, false)));
    }

    @Test
    public void testRemoveOldAccess(){
        // The saved group will return with an extra access object
        when(filterRepository.initGroups()).thenReturn(Collections.singletonList(getTestGroupWithExtraAccess()));
        // When init is called, the service will get an access missing an access
        when(filterUtil.getFullAccessList(anyString())).thenReturn(getTestGroup().getAccess());
        filterService.init();
        ArgumentCaptor<Group> groupArgument = ArgumentCaptor.forClass(Group.class);
        verify(filterRepository).save(groupArgument.capture());
        Group savedGroup = groupArgument.getValue();
        Assert.assertNotNull(savedGroup);
        Assert.assertEquals(2, savedGroup.getAccess().size()); // Should have an extra access over the original one
        Assert.assertTrue(savedGroup.getAccess().contains(new AccessImpl(TestClass.class.getName(), AccessType.NO_ACCESS, true)));
        Assert.assertTrue(savedGroup.getAccess().contains(new AccessImpl(SecondTestClass.class.getName(), AccessType.READ, false)));
    }

    @Test
    public void testAddNewPermission(){
        // When init is called, the service will get an access list with an additional permission for TestClass
        when(filterUtil.getFullAccessList(anyString())).thenReturn(getTestGroupWithExtraPermission().getAccess());
        filterService.init();
        ArgumentCaptor<GroupImpl> groupArgument = ArgumentCaptor.forClass(GroupImpl.class);
        verify(filterRepository).save(groupArgument.capture());
        GroupImpl savedGroup = groupArgument.getValue();
        Assert.assertNotNull(savedGroup);
        Assert.assertEquals(2, savedGroup.getAccess().size());
        Assert.assertTrue(savedGroup.getAccess().contains(new AccessImpl(TestClass.class.getName(), AccessType.NO_ACCESS, true)));

        AccessImpl access = savedGroup.getAccess().get(savedGroup.getAccess().indexOf(new AccessImpl(TestClass.class.getName(), AccessType.NO_ACCESS, true)));
        Assert.assertNotNull(access);
        Assert.assertEquals(5, access.getPermissions().size());
        Permission permission = access.getPermissions().stream().filter(p -> p.getPropertyName().equals("newField")).findFirst().get();
        Assert.assertNotNull(permission);
        Assert.assertEquals("Display", permission.getDisplayName());
        Assert.assertEquals(PermissionType.WRITE, permission.getPermission());
        Assert.assertTrue(permission.isModifiable());
    }

    @Test
    public void testRemoveOldPermission(){
        // When init is called, the service will get an access list with an additional permission for TestClass
        when(filterRepository.initGroups()).thenReturn(Collections.singletonList(getTestGroupWithExtraPermission()));
        // Filter util will return a class without the extra permission
        when(filterUtil.getFullAccessList(anyString())).thenReturn(getTestGroup().getAccess());
        filterService.init();
        ArgumentCaptor<GroupImpl> groupArgument = ArgumentCaptor.forClass(GroupImpl.class);
        verify(filterRepository).save(groupArgument.capture());
        GroupImpl savedGroup = groupArgument.getValue();
        Assert.assertNotNull(savedGroup);
        Assert.assertEquals(2, savedGroup.getAccess().size());

        AccessImpl access = savedGroup.getAccess().get(savedGroup.getAccess().indexOf(new AccessImpl(TestClass.class.getName(), AccessType.NO_ACCESS, true)));
        Assert.assertNotNull(access);
        Assert.assertEquals(4, access.getPermissions().size());
        Permission permission = access.getPermissions().stream().filter(p -> p.getPropertyName().equals("newField")).findFirst().orElse(null);
        Assert.assertNull(permission);
    }

    private Group getTestGroup(){
        Group group = new GroupImpl();
        group.setName("Test Group");
        group.setMembers(Collections.singletonList("Test User"));
        group.setAccess(getTestAccessList());
        return group;
    }

    private List<AccessImpl> getTestAccessList(){
        FilterUtil filterUtil = new FilterUtil(new DefaultClassFactory());
        List<AccessImpl> accessList = new ArrayList<>();
        accessList.add(filterUtil.createDefaultAccessFromClass(TestClass.class));
        accessList.add(filterUtil.createDefaultAccessFromClass(SecondTestClass.class));
        return accessList;
    }

    // Create a group with an extra access value over the normal test group
    private Group getTestGroupWithExtraAccess(){
        Group groupWithExtra = getTestGroup();
        Access extraAccess = new AccessImpl();
        extraAccess.setObjectClass("new Object");
        extraAccess.setDisplayName("Display Name");
        extraAccess.setModifiable(false);
        extraAccess.setAccess(AccessType.CREATE);
        groupWithExtra.getAccess().add(extraAccess);
        return groupWithExtra;
    }

    private Group getTestGroupWithExtraPermission(){
        GroupImpl group = (GroupImpl) getTestGroup();
        Access accessToChange = group.getAccess().stream().filter(access -> access.getObjectClass().equals(TestClass.class.getName())).findFirst().get();
        Permission permission = new PermissionImpl();
        permission.setModifiable(true);
        permission.setDisplayName("Display");
        permission.setPermission(PermissionType.WRITE);
        permission.setPropertyName("newField");
        accessToChange.getPermissions().add(permission);
        return group;
    }
}
