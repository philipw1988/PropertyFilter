package uk.co.agware.filter;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.agware.filter.data.*;
import uk.co.agware.filter.exceptions.FilterException;
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

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 9/04/2016.
 */
//TODO Put better commends on methods to explain what they're doing
//TODO Need to worry about arrays as well as collections
public class PropertyFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertyFilter.class);
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final List<Class> ignoredClasses;

    private final BiMap<String, String> displayToClassNames = HashBiMap.create();
    private final Map<String, Map<String, Access>> groups = new HashMap<>();
    private final Map<String, String> userToGroup = new HashMap<>();
    private boolean filterCollectionsOnSave = true;
    private boolean filterCollectionOnLoad = true;

    private FilterUtil filterUtil;

    PropertyFilter(FilterUtil filterUtil) {
        this.filterUtil = filterUtil;
        this.ignoredClasses = new ArrayList<>(Arrays.asList(String.class, Integer.class, int.class, Double.class, double.class, Float.class, float.class, BigDecimal.class, Boolean.class, boolean.class, Byte.class, byte.class, Date.class, LocalDate.class, LocalDateTime.class, BigInteger.class, Long.class, long.class)); // Not efficient, but a lazy way to do it in one line
    }

    public FilterUtil getFilterUtil() {
        return filterUtil;
    }

    public void filterCollectionsOnSave(boolean filter) {
        this.filterCollectionsOnSave = filter;
    }

    public void filterCollectionsOnLoad(boolean filter){
        this.filterCollectionOnLoad = filter;
    }

    public boolean filterCollectionsOnSave() {
        return filterCollectionsOnSave;
    }

    public boolean filterCollectionsOnLoad(){
        return filterCollectionOnLoad;
    }

    public boolean ignoredClassesContains(Class clazz){
        return ignoredClasses.contains(clazz);
    }

    public void addIgnoredClass(Class clazz){
        if(!ignoredClasses.contains(clazz)) {
            ignoredClasses.add(clazz);
        }
    }

    public boolean removeCollectionClass(Class clazz){
        return ignoredClasses.remove(clazz);
    }

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
     * Refreshes the groups listing
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

    public Map<String, Access> getGroup(String key){
        lock.readLock().lock();
        try {
            Map<String, Access> group = groups.get(key);
            if(group != null){
                return new HashMap<>(group);
            }
            return null;
        }
        finally {
            lock.readLock().unlock();
        }
    }

    public void addUserToGroup(String username, String group){
        userToGroup.put(username.toUpperCase(), group);
    }

    public String getUsersGroup(String username){
        lock.readLock().lock();
        String groupName = userToGroup.get(username.toUpperCase());
        lock.readLock().unlock();
        return groupName;
    }

    public List<String> getAccessibleClassesForGroup(String group){
        lock.readLock().lock();
        Map<String, Access> accessMap = getGroup(group);
        List<String> result = new ArrayList<>();
        if(accessMap != null) {
            for (Map.Entry<String, Access> e : accessMap.entrySet()) {
                if (!e.getValue().getAccess().equals(AccessType.NO_ACCESS)) {
                    result.add(e.getKey());
                }
            }
        }
        lock.readLock().unlock();
        return result;
    }

    public List<Permission> getAccessibleFieldsForGroup(String className, String group){
        lock.readLock().lock();
        Map<String, Access> accessMap = getGroup(group);
        List<Permission> results = new ArrayList<>();
        if(accessMap != null) {
            Access access = accessMap.get(className);
            if(access == null){
                access = accessMap.get(displayToClassNames.get(className));
            }
            if (access != null) {
                for (Permission p : FilterUtil.nullSafe(access.getPermissions())) {
                    if (!p.getPermission().equals(PermissionType.NO_ACCESS)) {
                        results.add(filterUtil.getClassFactory().copyPermissionClass(p));
                    }
                }
            }
        }
        lock.readLock().unlock();
        return results;
    }

    public Access getAccess(String className, String username) throws PropertyFilterException {
        String userGroup = getUsersGroup(username);
        if(userGroup == null) return null;
        return getAccessForGroup(className, userGroup);
    }

    public Access getAccessForGroup(String className, String groupName) throws PropertyFilterException {
        lock.readLock().lock();
        Map<String, Access> accessMap = groups.get(groupName);
        if(accessMap == null) throw new PropertyFilterException(String.format("Group %s does not exist", groupName));
        Access access = accessMap.get(className);
        if(access == null) {
            access = accessMap.get(displayToClassNames.get(className));
        }
        lock.readLock().unlock();
        return access;
    }

    public <T> T parseObjectForReturn(T object, String username) throws PropertyFilterException {
        String userGroup = getUsersGroup(username);
        if(userGroup == null){
            LOGGER.warn("User {} has no group", username);
            throw new PropertyFilterException(String.format("User %s has no group assigned", username));
        }
        return parseObjectForReturn(object, username, userGroup);
    }

    @SuppressWarnings("unchecked")
    public <T> T parseObjectForReturn(T object, String username, String groupName) throws PropertyFilterException {
        if(object == null) return null;
        if(ignoredClasses.contains(object.getClass())) return object; // If it's a class we're ignoring then just return the value

        Set<Field> fields = filterUtil.getAllFields(object);
        Map<String, Access> accessMap = getGroup(groupName);
        if(accessMap == null) {
            LOGGER.error("Could not find group {}", groupName);
            throw new PropertyFilterException(String.format("Could not find group %s", groupName));
        }

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
            LOGGER.error(e.getMessage(), e);
            throw new FilterException(e.getMessage(), e);
        }
        return obj;
    }

    @SuppressWarnings("unchecked")
    private Collection handleCollectionForReturn(Collection collection, String username, String groupName) throws PropertyFilterException {
        Collection result = (Collection) FilterUtil.instantiateObject(collection.getClass());
        for(Object o : collection){
            if(ignoredClasses.contains(o.getClass())){
                result.add(o);
            }
            else {
                result.add(parseObjectForReturn(o, username, groupName));
            }
        }
        return result;
    }

    public <T> T parseObjectForSaving(T newObject, T existingObject, String username) throws IllegalAccessException, PropertyFilterException {
        String userGroup = getUsersGroup(username);
        if(userGroup == null) {
            LOGGER.debug("User {} has no group", username);
            throw new PropertyFilterException(String.format("User %s has no group assigned", username));
        }
        return parseObjectForSaving(newObject, existingObject, username, userGroup);
    }

    //TODO Worry about maps
    @SuppressWarnings("unchecked")
    public <T> T parseObjectForSaving(T newObject, T existingObject, String username, String groupName) throws IllegalAccessException {
        if(newObject == null) {
            LOGGER.error("Null value passed into save method");
            throw new IllegalArgumentException("Null value passed into the filter save method");
        }

        if(ignoredClasses.contains(newObject.getClass())) return newObject; // If we're ignoring the value, just return the new one

        Set<Field> fields = filterUtil.getAllFields(newObject);
        Map<String, Access> accessMap = getGroup(groupName);
        if(accessMap == null) throw new FilterException(String.format("Group %s does not exist in current groups map", groupName));

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
            LOGGER.error(e.getMessage(), e);
            throw new FilterException(e.getMessage(), e);
        }
        return existingObject;
    }

    @SuppressWarnings("unchecked")
    private Collection handleCollectionsForSaving(Collection exitingCollection, Collection newCollection, String username, String groupName) throws IllegalAccessException {
        Collection resultingCollection = (Collection) FilterUtil.instantiateObject(newCollection.getClass());
        if(!filterCollectionsOnSave){ // If we're not filtering collections then we just add all the new ones to the existing ones
            resultingCollection.addAll(newCollection);
            return resultingCollection;
        }
        for(Object newVal : newCollection){
            Object existingVal = null;
            if(exitingCollection != null){ // If there was no collection before, we don't need to check for the existence of the object before filtering
                for(Object o : exitingCollection){
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
                Object parsedObject = parseObjectForSaving(newVal, existingVal, username, groupName);
                resultingCollection.add(parsedObject);
            }
        }
        return resultingCollection;
    }
}
