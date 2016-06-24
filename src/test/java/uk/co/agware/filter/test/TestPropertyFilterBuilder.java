package uk.co.agware.filter.test;

import org.junit.Assert;
import org.junit.Test;
import uk.co.agware.filter.PropertyFilter;
import uk.co.agware.filter.PropertyFilterBuilder;
import uk.co.agware.filter.test.classes.IgnoredClass;
import uk.co.agware.filter.test.classes.TestClass;
import uk.co.agware.filter.util.FilterUtil;

import java.util.Arrays;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 25/06/2016.
 */
public class TestPropertyFilterBuilder {

    @Test(expected = IllegalArgumentException.class)
    public void testNullUtil(){
        new PropertyFilterBuilder().build();
    }

    @Test
    public void testSetUtil(){
        FilterUtil filterUtil = new FilterUtil(null);
        PropertyFilter filter = new PropertyFilterBuilder().filterUtil(filterUtil).build();
        Assert.assertEquals(filterUtil, filter.getFilterUtil());
    }

    @Test
    public void testAddIgnoredClass(){
        PropertyFilter filter = new PropertyFilterBuilder()
                .filterUtil(new FilterUtil(null))
                .addIgnoredClass(IgnoredClass.class)
                .build();
        Assert.assertTrue(filter.ignoredClassesContains(IgnoredClass.class));
    }

    @Test
    public void testAddMultipleIgnoredClasses(){
        PropertyFilter filter = new PropertyFilterBuilder()
                .filterUtil(new FilterUtil(null))
                .addIgnoredClass(IgnoredClass.class)
                .addIgnoredClass(TestClass.class)
                .build();
        Assert.assertTrue(filter.ignoredClassesContains(IgnoredClass.class));
        Assert.assertTrue(filter.ignoredClassesContains(TestClass.class));
    }

    @Test
    public void testAddIgnoredList(){
        PropertyFilter filter = new PropertyFilterBuilder()
                .filterUtil(new FilterUtil(null))
                .addIgnoredClasses(Arrays.asList(IgnoredClass.class, TestClass.class))
                .build();
        Assert.assertTrue(filter.ignoredClassesContains(IgnoredClass.class));
        Assert.assertTrue(filter.ignoredClassesContains(TestClass.class));
    }

    @Test
    public void testSetIgnoreSave(){
        PropertyFilter filter = new PropertyFilterBuilder()
                .filterUtil(new FilterUtil(null))
                .filterCollectionsOnSave(false)
                .build();
        Assert.assertFalse(filter.filterCollectionsOnSave());
    }

    @Test
    public void testSetIgnoreLoad(){
        PropertyFilter filter = new PropertyFilterBuilder()
                .filterUtil(new FilterUtil(null))
                .filterCollectionsOnLoad(false)
                .build();
        Assert.assertFalse(filter.filterCollectionsOnLoad());
    }
}
