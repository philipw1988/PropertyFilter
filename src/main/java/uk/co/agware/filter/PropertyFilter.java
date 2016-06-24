package uk.co.agware.filter;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.agware.filter.data.*;
import uk.co.agware.filter.exceptions.FilterException;
import uk.co.agware.filter.exceptions.PropertyFilterException;
import uk.co.agware.filter.util.FilterUtil;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
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
//TODO Switch to using BeanUtils to make the code a little cleaner
public class PropertyFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertyFilter.class);
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final List<Class> ignoredClasses = new ArrayList<>(Arrays.asList(String.class, Integer.class, int.class, Double.class, double.class, Float.class, float.class, BigDecimal.class, Boolean.class, boolean.class, Byte.class, byte.class, Date.class, LocalDate.class, LocalDateTime.class, BigInteger.class, Long.class, long.class));

    private final BiMap<String, String> displayToClassNames = HashBiMap.create();
    private final Map<String, Map<String, IAccess>> groups = new HashMap<>();
    private final Map<String, String> userToGroup = new HashMap<>();
    private boolean filterCollectionsOnSave = true;
    private boolean filterCollectionOnLoad = true;

    private FilterUtil filterUtil;

    public PropertyFilter(FilterUtil filterUtil) {
        this.filterUtil = filterUtil;
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

    public boolean collectionClassesContains(Class clazz){
        return ignoredClasses.contains(clazz);
    }

    public void addCollectionClass(Class clazz){
        if(!ignoredClasses.contains(clazz)) {
            ignoredClasses.add(clazz);
        }
    }

    public boolean removeCollectionClass(Class clazz){
        return ignoredClasses.remove(clazz);
    }

    public Map<String, String> getUserToGroupMap(){
        lock.readLock().lock();
        Map<String, String> result = new HashMap<>();
        result.putAll(userToGroup);
        lock.readLock().unlock();
        return result;
    }

    /**
     * Refreshes the groups listing
     */
    public void setGroups(List<IGroup> IGroupList) {
        lock.writeLock().lock();
        groups.clear();
        userToGroup.clear();
        for (IGroup g : FilterUtil.nullSafe(IGroupList)) {
            Map<String, IAccess> accessMap = new HashMap<>();
            for (IAccess a : FilterUtil.nullSafe(g.getAccess())) {
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

    public Map<String, IAccess> getGroup(String key){
        return groups.get(key);
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

    public List<String> getAccessibleClasses(String username){
        return getAccessibleClassesForGroup(getUsersGroup(username));
    }

    public List<String> getAccessibleClassesForGroup(String group){
        lock.readLock().lock();
        Map<String, IAccess> accessMap = getGroup(group);
        List<String> result = new ArrayList<>();
        if(accessMap != null) {
            for (Map.Entry<String, IAccess> e : accessMap.entrySet()) {
                if (!e.getValue().getAccess().equals(AccessType.NO_ACCESS)) {
                    result.add(e.getKey());
                }
            }
        }
        lock.readLock().unlock();
        return result;
    }

    public List<IPermission> getAccessibleFields(Object target, String username){
        return getAccessibleFields(target.getClass().getName(), username);
    }

    public List<IPermission> getAccessibleFields(Class clazz, String username){
        return getAccessibleFields(clazz.getName(), username);
    }

    public List<IPermission> getAccessibleFields(String className, String username){
        return getAccessibleFieldsForGroup(className, getUsersGroup(username));
    }

    public List<IPermission> getAccessibleFieldsForGroup(Object target, String group){
        return getAccessibleFieldsForGroup(target.getClass().getName(), group);
    }

    public List<IPermission> getAccessibleFieldsForGroup(Class clazz, String group){
        return getAccessibleFieldsForGroup(clazz.getName(), group);
    }

    public List<IPermission> getAccessibleFieldsForGroup(String className, String group){
        lock.readLock().lock();
        Map<String, IAccess> accessMap = getGroup(group);
        List<IPermission> results = new ArrayList<>();
        if(accessMap != null) {
            IAccess access = accessMap.get(className);
            if(access == null){
                access = accessMap.get(displayToClassNames.get(className));
            }
            if (access != null) {
                for (IPermission p : FilterUtil.nullSafe(access.getPermissions())) {
                    if (!p.getPermission().equals(PermissionType.NO_ACCESS)) {
                        results.add(filterUtil.getClassFactory().copyPermissionClass(p));
                    }
                }
            }
        }
        lock.readLock().unlock();
        return results;
    }

    public IAccess getAccess(Object target, String username) throws PropertyFilterException {
        return getAccess(target.getClass().getName(), username);
    }

    public IAccess getAccess(Class clazz, String username) throws PropertyFilterException {
        return getAccess(clazz.getName(), username);
    }

    public IAccess getAccess(String className, String username) throws PropertyFilterException {
        String userGroup = getUsersGroup(username);
        if(userGroup == null) return null;
        return getAccessForGroup(className, userGroup);
    }

    public IAccess getAccessForGroup(Object target, String groupName) throws PropertyFilterException {
        return getAccessForGroup(target.getClass().getName(), groupName);
    }

    public IAccess getAccessForGroup(Class clazz, String groupName) throws PropertyFilterException {
        return getAccessForGroup(clazz.getName(), groupName);
    }

    public IAccess getAccessForGroup(String className, String groupName) throws PropertyFilterException {
        lock.readLock().lock();
        Map<String, IAccess> accessMap = groups.get(groupName);
        if(accessMap == null) throw new PropertyFilterException(String.format("Group %s does not exist", groupName));
        IAccess access = accessMap.get(className);
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
        Map<String, IAccess> accessMap = getGroup(groupName);
        if(accessMap == null) {
            LOGGER.error("Could not find group {}", groupName);
            throw new PropertyFilterException(String.format("Could not find group %s", groupName));
        }

        T obj = (T) FilterUtil.instantiateObject(object.getClass()); // Create a blank object to fill with values
        try {
            IAccess access = accessMap.get(object.getClass().getName());
            if(access == null) throw new FilterException("Access missing for class of type " +object.getClass().getName());
            if(access.getAccess().equals(AccessType.NO_ACCESS)) return null; // If they don't have access then return null so they can't view the data at all

            for (Field f : fields) {
                if (filterUtil.isFieldReadable(f.getName(), access) ) {
                    PropertyDescriptor pd = new PropertyDescriptor(f.getName(), object.getClass());
                    Object value = pd.getReadMethod().invoke(object);
                    if (!Collection.class.isAssignableFrom(f.getType())) {
                        // Parse down sub values, will escape on ignored classes
                        value = parseObjectForReturn(value, username, groupName);
                        pd.getWriteMethod().invoke(obj, value);
                    }
                    else {
                        if(!filterCollectionOnLoad){ // Just dump the collection in
                            pd.getWriteMethod().invoke(obj, value);
                        }
                        else if(value != null){
                            pd.getWriteMethod().invoke(obj, handleCollectionForReturn((Collection) value, username, groupName));
                        }
                    }
                }
            }
        } catch (IntrospectionException | InvocationTargetException | IllegalAccessException e) {
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
        Map<String, IAccess> accessMap = getGroup(groupName);
        if(accessMap == null) throw new FilterException(String.format("Group %s does not exist in current groups map", groupName));

        if(existingObject == null) existingObject = (T) FilterUtil.instantiateObject(newObject.getClass());

        IAccess access = accessMap.get(newObject.getClass().getName());
        if(access == null) throw new FilterException(String.format("No access defined for class %s and group %s", newObject.getClass().getName(), groupName));
        // If the user doesn't have access to change things, return the object that was there before they started
        if (access.getAccess().equals(AccessType.NO_ACCESS) || access.getAccess().equals(AccessType.READ)) return existingObject;
        try {
            for (Field f : fields) {
                if(filterUtil.isFieldWritable(f.getName(), access)) {
                    PropertyDescriptor pd = new PropertyDescriptor(f.getName(), newObject.getClass());
                    Object newValue = pd.getReadMethod().invoke(newObject);
                    if (!Collection.class.isAssignableFrom(f.getType())) {
                        // If it's a normal class then we filter again, ignored classes will return full value
                        if(newValue != null) {
                            Object existingValue = pd.getReadMethod().invoke(existingObject);
                            newValue = parseObjectForSaving(newValue, existingValue, username, groupName);
                            pd.getWriteMethod().invoke(existingObject, newValue);
                        }
                    }
                    else {
                        if(newValue == null){ // Null collection can be ignored
                            pd.getWriteMethod().invoke(existingObject, newValue);
                        }
                        else {
                            // Get the old and new collection
                            Collection newCollection = (Collection)newValue;
                            Collection existingCollection = (Collection) pd.getReadMethod().invoke(existingObject);
                            // Parse the collection and get one containing all the new values
                            Collection resultingCollection = handleCollectionsForSaving(existingCollection, newCollection, username, groupName);
                            // Clear the current contents of the collection and add all the results of the filtering
                            existingCollection.clear();
                            existingCollection.addAll(resultingCollection);
                        }
                    }
                }
            }
        } catch (IntrospectionException | InvocationTargetException | IllegalAccessException e) {
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
