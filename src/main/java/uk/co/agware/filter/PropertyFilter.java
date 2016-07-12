package uk.co.agware.filter;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.agware.filter.data.*;
import uk.co.agware.filter.exceptions.FilterException;
import uk.co.agware.filter.exceptions.GroupNotFoundException;
import uk.co.agware.filter.exceptions.PropertyFilterException;
import uk.co.agware.filter.util.FilterUtil;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 9/04/2016.
 */
//TODO Need to worry about arrays as well as collections
public class PropertyFilter {

    private final Logger logger = LoggerFactory.getLogger(PropertyFilter.class);
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Set<Class> ignoredClasses= new HashSet<>(Arrays.asList(String.class, Integer.class, int.class, Double.class, double.class, Float.class, float.class, BigDecimal.class, Boolean.class, boolean.class, Byte.class, byte.class, Date.class, LocalDate.class, LocalDateTime.class, BigInteger.class, Long.class, long.class)); // Not efficient, but a lazy way to do it in one line

    private final BiMap<String, String> displayToClassNames = HashBiMap.create();
    private final Map<String, Map<String, Access>> groups = new HashMap<>();
    private final Map<String, String> userToGroup = new HashMap<>();
    private boolean filterCollectionsOnSave;
    private boolean filterRelationsOnSave;
    private boolean filterCollectionOnLoad;
    private boolean filterRelationsOnLoad;

    private FilterUtil filterUtil;

    /* Package local constructor for use with the Builder */
    PropertyFilter(FilterUtil filterUtil,
                   Set<Class> ignoredClasses,
                   boolean filterCollectionOnLoad,
                   boolean filterRelationsOnLoad,
                   boolean filterCollectionsOnSave,
                   boolean filterRelationsOnSave) {
        this.filterUtil = filterUtil;
        this.ignoredClasses.addAll(ignoredClasses);
        this.filterCollectionOnLoad = filterCollectionOnLoad;
        this.filterRelationsOnLoad = filterRelationsOnLoad;
        this.filterCollectionsOnSave = filterCollectionsOnSave;
        this.filterRelationsOnSave = filterRelationsOnSave;
    }

    /**
     * Returns the {@link FilterUtil} used by this class.
     *
     * @return A reference to the {@link FilterUtil} used by the class
     */
    public FilterUtil getFilterUtil() {
        return filterUtil;
    }

    /**
     * Adds a new class to the set of ignored classes.
     *
     * @param clazz The class to add
     */
    public boolean addIgnoredClass(Class clazz){
        return ignoredClasses.add(clazz);
    }

    /**
     * Returns the current mapping of users to groups
     * @return The map of users in groups
     */
    public Map<String, String> getUserToGroupMap(){
        lock.readLock().lock();
        try {
            return new HashMap<>(userToGroup);
        }
        finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns a map of all the users in each group
     *
     * @return A map with the group names as keys and the users as a list
     */
    public Map<String, List<String>> getGroupMembership(){
        lock.readLock().lock();
        try {
            return userToGroup.keySet().stream().collect(Collectors.groupingBy(userToGroup::get));
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Refreshes the current group mapping, will overwrite the
     * exiting set of mappings with the new set.
     *
     * @param GroupList The groups to add to the mapping
     */
    public void setGroups(List<Group> GroupList) {
        lock.writeLock().lock();
        groups.clear();
        userToGroup.clear();
        for (Group g : FilterUtil.nullSafe(GroupList)) {
            Map<String, Access> accessMap = new HashMap<>();
            for (Access a : FilterUtil.nullSafe(g.getAccess())) {
                accessMap.put(a.getObjectClass(), a);
                String displayName = a.getDisplayName() == null || "".equals(a.getDisplayName()) ? a.getObjectClass() : a.getDisplayName();
                displayToClassNames.put(displayName, a.getObjectClass());
            }
            groups.put(g.getName(), accessMap);
            for (String s : FilterUtil.nullSafe(g.getMembers())) {
                userToGroup.put(s.toUpperCase(), g.getName());
            }
        }
        lock.writeLock().unlock();
    }

    /**
     * Returns the class mapping for a given group
     * @param key The group name
     * @return The class mapping for the given group
     */
    public Map<String, Access> getGroup(String key){
        lock.readLock().lock();
        try {
            Map<String, Access> group = groups.get(key);
            if(group != null){
                return new HashMap<>(group);
            }
            throw new GroupNotFoundException(key);
        }
        finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Adds a user to a group, will overwrite the existing
     * mapping if it already exists. Will return the previous
     * mapping if one existed.
     *
     * @param username The name of the user
     * @param group The name of the group
     */
    public String addUserToGroup(String username, String group){
        lock.writeLock().lock();
        try {
            return userToGroup.put(username.toUpperCase(), group);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Returns the group for a given user
     * @param username The user to find the group of
     * @return The name of the user's group, or null if no group is found
     * @throws PropertyFilterException if the user does not have a group
     */
    public String getUsersGroup(String username) throws PropertyFilterException {
        lock.readLock().lock();
        try {
            String group = userToGroup.get(username.toUpperCase());
            if(group == null){
                throw new PropertyFilterException(String.format("User %s has no group assigned", username));
            }
            return group;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns a list of accessible class names for a given group.
     *
     * @param group The group name to search for
     * @return The list of accessible class names
     */
    public List<String> getAccessibleClasses(String group){
        lock.readLock().lock();
        try {
            Map<String, Access> accessMap = getGroup(group);
            // filters out classes with NO_ACCESS and then returns the class name from the map key
            return accessMap.entrySet().stream()
                    .filter(e -> !e.getValue().getAccess().equals(AccessType.NO_ACCESS))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns a list of all accessible fields for a given group on a given
     * class.
     *
     * @param className The name of the target class
     * @param group The group of the user requesting the access
     * @return A list of {@link Permission} entities for the class
     * @throws PropertyFilterException If the group does not exist
     */
    public List<Permission> getAccessibleFields(String className, String group) throws PropertyFilterException {
        lock.readLock().lock();
        try {
            Access access = getAccessForGroup(className, group);
            if(access == null) throw new FilterException(String.format("Group %s does not have Access defined for class %s", group, className));
            return FilterUtil.nullSafeStream(access.getPermissions())
                    .filter(p -> p.getPermission() != PermissionType.NO_ACCESS)
                    .map(p -> filterUtil.getClassFactory().copyPermissionClass(p))
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns a user's access for a given class, first gets the users group
     * before calling to {@link #getAccessForGroup(String, String)} to retrieve
     * the value.
     *
     * @param className The class to retrieve the access value for
     * @param username The name of the user
     * @return The {@link Access} object for the given user's group on the given class
     * @throws PropertyFilterException
     */
    public Access getAccess(String className, String username) throws PropertyFilterException {
        String userGroup = getUsersGroup(username);
        if(userGroup == null) return null;
        return getAccessForGroup(className, userGroup);
    }

    /**
     * Retrieves the {@link Access} value for the given {@code group} on the specificed
     * {@code className}.
     *
     * @param className The name of the class to get the access for
     * @param groupName The name of the group to get the access for
     * @return The {@link Access} object for the given class for the given group
     * @throws PropertyFilterException If the given {@code group} does not exist
     */
    public Access getAccessForGroup(String className, String groupName) throws PropertyFilterException {
        lock.readLock().lock();
        try {
            Map<String, Access> accessMap = getGroup(groupName);
            Access access = accessMap.get(className);
            if (access == null) {
                access = accessMap.get(displayToClassNames.get(className));
            }
            if(access == null) throw new FilterException(String.format("Group %s does not have any access set for class %s", groupName, className));
            return access;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets the group of a user and then returns the
     * result of {@link #parseObjectForReturn(Object, String, String)}
     *
     * @param object The object to be parsed
     * @param username The name of the user making the request
     * @param <T> The type of the object being parsed
     * @return The parsed object
     * @throws PropertyFilterException If the user's group cannot be found
     */
    public <T> T parseObjectForReturn(T object, String username) throws PropertyFilterException {
        return parseObjectForReturn(object, username, getUsersGroup(username));
    }

    /**
     * Parses a supplied object, removing the values from it that
     * the user does not have access to view, instantiates a blank object
     * to achieve this, only moving over the values that are required.
     *
     * @param object The object to be parsed
     * @param username The user making the request
     * @param groupName The group that the user belongs to
     * @param <T> The type of the object being parsed
     * @return The parsed object
     */
    @SuppressWarnings("unchecked")
    public <T> T parseObjectForReturn(T object, String username, String groupName) {
        if(object == null) return null;
        if(ignoredClasses.contains(object.getClass())) return object; // If it's a class we're ignoring then just return the value

        Set<Field> fields = filterUtil.getAllFields(object);
        Map<String, Access> accessMap = getGroup(groupName);

        T obj = (T) FilterUtil.instantiateObject(object.getClass()); // Create a blank object to fill with values
        try {
            Access access = accessMap.get(object.getClass().getName());
            if(access == null) throw new FilterException("Access missing for class of type " +object.getClass().getName());
            if(access.getAccess().equals(AccessType.NO_ACCESS)) return null; // If they don't have access then return null so they can't view the data at all

            for (Field f : fields) {
                if (filterUtil.isFieldReadable(f.getName(), access) ) {
                    Object value = PropertyUtils.getProperty(object, f.getName());
                    if (!Collection.class.isAssignableFrom(f.getType())) {
                        // Parse down sub values, will escape on ignored classes
                        value = parseObjectForReturn(value, username, groupName);
                        PropertyUtils.setProperty(obj, f.getName(), value);
                    }
                    else {
                        if(!filterCollectionOnLoad){ // Just dump the collection in
                            PropertyUtils.setProperty(obj, f.getName(), value);
                        }
                        else if(value != null){
                            Collection resultingCollection = handleCollectionForReturn((Collection) value, username, groupName);
                            PropertyUtils.setProperty(obj, f.getName(), resultingCollection);
                        }
                    }
                }
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new FilterException(e.getMessage(), e);
        }
        return obj;
    }

    /**
     * Parses a collection of objects for return, creates a new empty collection for the
     * results values.
     *
     * @param collection The collection of objects to be parsed
     * @param username The name of the user making the call
     * @param groupName The group of the user making the call
     * @param <T> The type of the objects in the collection
     * @return A list of parsed entities
     */
    @SuppressWarnings("unchecked")
    public <T> Collection<T> handleCollectionForReturn(Collection<T> collection, String username, String groupName) {
        Collection<T> result = FilterUtil.instantiateCollection(collection.getClass());
        for(T o : collection){
            if(ignoredClasses.contains(o.getClass())){
                result.add(o);
            }
            else {
                T parsed = parseObjectForReturn(o, username, groupName);
                if(parsed != null) {
                    result.add(parsed);
                }
            }
        }
        return result;
    }

    /**
     * Finds the group of the given user and then runs {@link #parseObjectForSaving(Object, Object, String, String)}
     *
     * @param newObject The object containing the new values
     * @param existingObject The object containing the currently stored values
     * @param username The name of the user making the request
     * @param <T> The type of the object to be returned
     * @return The {@code existingObject} with the correct new values copied over onto it
     * @throws PropertyFilterException
     */
    public <T> T parseObjectForSaving(T newObject, T existingObject, String username) throws PropertyFilterException {
        return parseObjectForSaving(newObject, existingObject, username, getUsersGroup(username));
    }

    /**
     * Parses a given object for return to the database, copies the accessible values from
     * the {@code newObject} to the {@code existingObject}, ignoring any that are {@link uk.co.agware.filter.annotations.NoAccess}
     * or {@link uk.co.agware.filter.annotations.ReadOnly}. Will filter any related classes
     * that are detected as well, including collections, unless told not to.
     *
     * @param newObject The object containing new values to be saved into the database
     * @param existingObject The object with the existing values from the database
     * @param username The name of the user making the request
     * @param groupName The group of the user making the request
     * @param <T> The type of the object to be returned
     * @return The {@code existingObject} with new values copied over into it
     */
    //TODO Worry about maps
    @SuppressWarnings("unchecked")
    public <T> T parseObjectForSaving(T newObject, T existingObject, String username, String groupName) {
        if(newObject == null) {
            throw new IllegalArgumentException("Null value passed into the filter save method");
        }

        if(ignoredClasses.contains(newObject.getClass())) return newObject; // If we're ignoring the value, just return the new one

        Set<Field> fields = filterUtil.getAllFields(newObject);
        Map<String, Access> accessMap = getGroup(groupName);

        if(existingObject == null) existingObject = (T) FilterUtil.instantiateObject(newObject.getClass());

        Access access = accessMap.get(newObject.getClass().getName());
        if(access == null) throw new FilterException(String.format("No access defined for class %s and group %s", newObject.getClass().getName(), groupName));
        // If the user doesn't have access to change things, return the object that was there before they started
        if (access.getAccess().equals(AccessType.NO_ACCESS) || access.getAccess().equals(AccessType.READ)) return existingObject;
        try {
            for (Field f : fields) {
                if(filterUtil.isFieldWritable(f.getName(), access)) {
                    Object newValue = PropertyUtils.getProperty(newObject, f.getName());
                    if (!Collection.class.isAssignableFrom(f.getType())) {
                        // If it's a normal class then we filter again, ignored classes will return full value
                        if(newValue != null) {
                            Object existingValue = PropertyUtils.getProperty(newObject, f.getName());
                            newValue = parseObjectForSaving(newValue, existingValue, username, groupName);
                            PropertyUtils.setProperty(existingObject, f.getName(), newValue);
                        }
                    }
                    else {
                        if(newValue == null){ // Null collection can be ignored
                            PropertyUtils.setProperty(existingObject, f.getName(), newValue);
                        }
                        else {
                            // Get the old and new collection
                            Collection newCollection = (Collection)newValue;
                            Collection existingCollection = (Collection) PropertyUtils.getProperty(existingObject, f.getName());
                            // Parse the collection and get one containing all the new values
                            Collection resultingCollection = handleCollectionsForSaving(existingCollection, newCollection, username, groupName);
                            if(existingCollection == null){ // If the collection was null then we need to instantiate it
                                existingCollection = FilterUtil.instantiateCollection(f.getType());
                                PropertyUtils.setProperty(existingObject, f.getName(), existingCollection);
                            }
                            // Clear the current contents of the collection and add all the results of the filtering
                            existingCollection.clear();
                            existingCollection.addAll(resultingCollection);
                        }
                    }
                }
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new FilterException(e.getMessage(), e);
        }
        return existingObject;
    }

    /**
     * Parses a collection of objects and returns a new collection containing the values
     * from {@code existingCollection} with the values from {@code newCollection} copied over
     * onto them. Relies on the objects having a valid {@code .equals()} method so that the
     * matching objects can be used by {@link #parseObjectForSaving(Object, Object, String, String)}
     *
     * @param exitingCollection A collection containing the values stored in the database
     * @param newCollection A collection containing the new values
     * @param username The name of the user making the call
     * @param groupName The group of the user
     * @return A collection containing all the objects from {@code existingCollection} with the
     * values from the matching object in {@code newCollection} copied over into them
     */
    @SuppressWarnings("unchecked")
    public  <T> Collection<T> handleCollectionsForSaving(Collection<T> exitingCollection, Collection<T> newCollection, String username, String groupName) {
        Collection<T> resultingCollection = FilterUtil.instantiateCollection(newCollection.getClass());
        if(!filterCollectionsOnSave){ // If we're not filtering collections then we just add all the new ones to the existing ones
            resultingCollection.addAll(newCollection);
            return resultingCollection;
        }
        for(T newVal : newCollection){
            T existingVal = null;
            if(exitingCollection != null){ // If there was no collection before, we don't need to check for the existence of the object before filtering
                for(T o : exitingCollection){
                    if(o.equals(newVal)){
                        existingVal = o;
                        break;
                    }
                }
            }
            // If it's a class type we aren't filtering, and it wasn't found before, then add it to the resulting collection
            if(ignoredClasses.contains(newVal.getClass())){
                if(existingVal == null) {
                    resultingCollection.add(newVal);
                }
            }
            else { // Filter the object and add to the result
                T parsedObject = parseObjectForSaving(newVal, existingVal, username, groupName);
                resultingCollection.add(parsedObject);
            }
        }
        return resultingCollection;
    }
}
