package uk.co.agware.filter.test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import uk.co.agware.filter.PropertyFilter;
import uk.co.agware.filter.PropertyFilterBuilder;
import uk.co.agware.filter.persistence.FilterRepository;
import uk.co.agware.filter.service.FilterService;
import uk.co.agware.filter.service.ServiceBuilder;
import uk.co.agware.filter.util.FilterUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 24/06/2016.
 */
public class TestServiceBuilder extends Mockito {

    private PropertyFilter mockPropertyFilter;
    private PropertyFilter propertyFilter;
    private FilterUtil filterUtil;

    @Before
    public void setUp(){
        mockPropertyFilter = mock(PropertyFilter.class);
        filterUtil = mock(FilterUtil.class);
        
        propertyFilter = new PropertyFilterBuilder().filterUtil(filterUtil).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullPropertyFilter(){
        new ServiceBuilder(null).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoPackagesAdded(){
        new ServiceBuilder(mockPropertyFilter).build();
    }

    @Test
    public void testAssignRepository(){
        FilterRepository repository = mock(FilterRepository.class);
        FilterService service = new ServiceBuilder(propertyFilter)
                .addPackageToScan("test.package")
                .withRepository(repository)
                .build();
        service.init();
        verify(repository).initGroups();
    }

    @Test
    public void testPackageScan(){
        FilterService service = new ServiceBuilder(propertyFilter)
                .addPackageToScan("test.package")
                .build();
        service.init();
        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        verify(filterUtil).getFullAccessList(pathCaptor.capture());
        String path = pathCaptor.getValue();
        Assert.assertNotNull(path);
        Assert.assertEquals("test.package", path);
    }

    @Test
    public void testMultiPackageScan(){
        FilterService service = new ServiceBuilder(propertyFilter)
                .addPackageToScan("test.package")
                .addPackageToScan("another.package")
                .build();
        service.init();
        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        verify(filterUtil, times(2)).getFullAccessList(pathCaptor.capture());
        List<String> paths = pathCaptor.getAllValues();
        Assert.assertNotNull(paths);
        Assert.assertEquals(2, paths.size());
        Assert.assertTrue(paths.contains("test.package"));
        Assert.assertTrue(paths.contains("another.package"));
    }

    @Test
    public void testStaticGroupAssignment(){
        PropertyFilter filter = propertyFilter;
        FilterService service = new ServiceBuilder(filter)
                .addPackageToScan("test.package")
                .addStaticGroupAllocation("test", "group")
                .build();
        service.init();
        Assert.assertEquals("group", filter.getUsersGroup("test"));
    }

    @Test
    public void testMultipleStaticGroupAssignment(){
        PropertyFilter filter = propertyFilter;
        FilterService service = new ServiceBuilder(filter)
                .addPackageToScan("test.package")
                .addStaticGroupAllocation("test", "group")
                .addStaticGroupAllocation("test2", "group")
                .addStaticGroupAllocation("test3", "group2")
                .build();
        service.init();
        Assert.assertEquals("group", filter.getUsersGroup("test"));
        Assert.assertEquals("group", filter.getUsersGroup("test2"));
        Assert.assertEquals("group2", filter.getUsersGroup("test3"));
    }

    @Test
    public void testStaticGroupAssignmentWithMap(){
        PropertyFilter filter = propertyFilter;
        Map<String, String> map = new HashMap<>();
        map.put("test", "group");
        map.put("test2", "group");
        map.put("test3", "group2");
        FilterService service = new ServiceBuilder(filter)
                .addPackageToScan("test.package")
                .addStaticGroupAllocations(map)
                .build();
        service.init();

        Assert.assertEquals("group", filter.getUsersGroup("test"));
        Assert.assertEquals("group", filter.getUsersGroup("test2"));
        Assert.assertEquals("group2", filter.getUsersGroup("test3"));
    }
}
