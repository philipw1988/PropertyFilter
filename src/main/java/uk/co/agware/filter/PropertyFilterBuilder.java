package uk.co.agware.filter;

import uk.co.agware.filter.impl.DefaultClassFactory;
import uk.co.agware.filter.util.FilterUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 25/06/2016.
 */
public class PropertyFilterBuilder {

    private List<Class> ignoredClasses = new ArrayList<>();
    private FilterUtil filterUtil = new FilterUtil(new DefaultClassFactory());
    private boolean filterCollectionsOnSave = true;
    private boolean filterCollectionsOnLoad = true;

    public PropertyFilterBuilder(){}

    public PropertyFilterBuilder filterUtil(FilterUtil filterUtil){
        this.filterUtil = filterUtil;
        return this;
    }

    public PropertyFilterBuilder addIgnoredClass(Class clazz){
        if(!ignoredClasses.contains(clazz)){
            ignoredClasses.add(clazz);
        }
        return this;
    }

    public PropertyFilterBuilder addIgnoredClasses(List<Class> classes){
        ignoredClasses.addAll(classes.stream().filter(c -> !ignoredClasses.contains(c)).collect(Collectors.toList()));
        return this;
    }

    public PropertyFilterBuilder filterCollectionsOnSave(boolean filter){
        this.filterCollectionsOnSave = filter;
        return this;
    }

    public PropertyFilterBuilder filterCollectionsOnLoad(boolean filter){
        this.filterCollectionsOnLoad = filter;
        return this;
    }

    public PropertyFilter build(){
        PropertyFilter propertyFilter = new PropertyFilter(filterUtil);
        propertyFilter.filterCollectionsOnSave(filterCollectionsOnSave);
        propertyFilter.filterCollectionsOnLoad(filterCollectionsOnLoad);
        ignoredClasses.forEach(propertyFilter::addIgnoredClass);
        return propertyFilter;
    }
}
