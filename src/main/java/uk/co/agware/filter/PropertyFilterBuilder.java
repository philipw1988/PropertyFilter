package uk.co.agware.filter;

import uk.co.agware.filter.impl.DefaultClassFactory;
import uk.co.agware.filter.util.FilterUtil;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Used for building a PropertyFilter object, comes with default values set which can be overridden.
 * All filter options are set to {@code true} by default and the {@link FilterUtil} comes
 * initialized with a {@link DefaultClassFactory}.
 *
 * Created by Philip Ward <Philip.Ward@agware.com> on 25/06/2016.
 */
public class PropertyFilterBuilder {

    private Set<Class<?>> ignoredClasses = new HashSet<>();
    private FilterUtil filterUtil = new FilterUtil(new DefaultClassFactory());
    private boolean filterCollectionsOnLoad = true;
    private boolean filterRelationsOnLoad = true;
    private boolean filterCollectionsOnSave = true;
    private boolean filterRelationsOnSave = true;

    /** Default Constructor */
    public PropertyFilterBuilder(){}

    /**
     * Builds the builder with a {@link FilterUtil},
     * can be used as a quick shortcut if the rest of the defaults are
     * correct.
     *
     * @param filterUtil The FilterUtil for the PropertyFilter
     */
    public PropertyFilterBuilder(FilterUtil filterUtil) {
        this.filterUtil = filterUtil;
    }

    /**
     * Sets the {@link FilterUtil} to be used by {@link PropertyFilter}
     * built by this class.
     *
     * @param filterUtil The {@link FilterUtil} to be used
     * @return Returns itself
     */
    public PropertyFilterBuilder filterUtil(FilterUtil filterUtil){
        this.filterUtil = filterUtil;
        return this;
    }

    /**
     * Adds an additional class to the list of classes that the {@link PropertyFilter}
     * will not try to filter the fields of.
     *
     * @param clazz The class to be ignored
     * @return Returns itself
     */
    public PropertyFilterBuilder addIgnoredClass(Class<?> clazz){
        if(!ignoredClasses.contains(clazz)){
            ignoredClasses.add(clazz);
        }
        return this;
    }

    /**
     * Adds a {@link Collection} of classes to be ignored by the {@link PropertyFilter}
     *
     * @param classes A collection of Classes to be ignored
     * @return Returns itself
     */
    public PropertyFilterBuilder addIgnoredClasses(Collection<Class<?>> classes){
        ignoredClasses.addAll(classes);
        return this;
    }

    /**
     * Sets whether or not the {@link PropertyFilter} should parse the value
     * of entities that exist in collections on the entity passed in.
     *
     * @param doFilter Sets the filter rule value
     * @return Returns itself
     */
    public PropertyFilterBuilder filterCollectionsOnLoad(boolean doFilter){
        this.filterCollectionsOnLoad = doFilter;
        return this;
    }

    /**
     * Sets whether the property filter should attempt to filter relations
     * that it finds on the entity it is passed. Relations are classes as
     * any entity that the filter hasn't been told it should ignore.
     *
     * @param doFilter The filter rule value
     * @return Returns itself
     */
    public PropertyFilterBuilder filterRelationsOnLoad(boolean doFilter){
        this.filterRelationsOnLoad = doFilter;
        return this;
    }

    /**
     * Sets whether or not the {@link PropertyFilter} should attempt to
     * filter the values found in collections contained within the entity
     * it is passed.
     *
     * @param doFilter The filter rule value
     * @return Returns itself
     */
    public PropertyFilterBuilder filterCollectionsOnSave(boolean doFilter){
        this.filterCollectionsOnSave = doFilter;
        return this;
    }

    /**
     * Sets whether or not the {@link PropertyFilter} should attempt
     * to filter related entities that it finds while parsing an object
     * for saving. These are defined as any class which is not in its ignored
     * classes collection.
     *
     * @param doFilter The filter rule value
     * @return Returns itself
     */
    public PropertyFilterBuilder filterRelationsOnSave(boolean doFilter){
        this.filterRelationsOnSave = doFilter;
        return this;
    }

    /**
     * Returns a {@link PropertyFilter} built with the values defined in this builder
     * @return An initialized {@link PropertyFilter}
     */
    public PropertyFilter build(){
        return new PropertyFilter(filterUtil,
                ignoredClasses,
                filterCollectionsOnLoad,
                filterRelationsOnLoad,
                filterCollectionsOnSave,
                filterRelationsOnSave);
    }
}
