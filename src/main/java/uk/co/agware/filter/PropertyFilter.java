package uk.co.agware.filter;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.agware.filter.exceptions.FilterException;
import uk.co.agware.filter.exceptions.PropertyFilterException;
import uk.co.agware.filter.objects.Access;
import uk.co.agware.filter.objects.Permission;
import uk.co.agware.filter.objects.Group;
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
    private final List<Class> ignoredClasses;

    private final BiMap<String, String> displayToClassNames;
    private final Map<String, Map<String, Access>> groups;
    private final Map<String, String> userToGroup;
    private boolean filterCollectionsOnSave;
    private boolean filterCollectionOnLoad;

    public PropertyFilter() {
        this.ignoredClasses = new ArrayList<>(Arrays.asList(String.class, Integer.class, int.class, Double.class, double.class, Float.class, float.class, BigDecimal.class, Boolean.class, boolean.class, Byte.class, byte.class, Date.class, LocalDate.class, LocalDateTime.class, BigInteger.class, Long.class, long.class)); // Not efficient, but a lazy way to do it in one line
        this.groups = new HashMap<>();
        this.userToGroup = new HashMap<>();
        this.filterCollectionsOnSave = true;
        this.filterCollectionOnLoad = true;
        this.displayToClassNames = HashBiMap.create();
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
    public void setGroups(List<Group> groupList) {
        lock.writeLock().lock();
        groups.clear();
        userToGroup.clear();
        for (Group g : FilterUtil.checkNull(groupList)) {
            Map<String, Access> accessMap = new HashMap<>();
            for (Access a : FilterUtil.checkNull(g.getAccess())) {
                accessMap.put(a.getObjectClass(), a);
                String displayName = a.getDisplayName() == null || "".equals(a.getDisplayName()) ? a.getObjectClass() : a.getDisplayName();
                displayToClassNames.put(displayName, a.getObjectClass());
            }
            groups.put(g.getName(), accessMap);
            for (String s : FilterUtil.checkNull(g.getMembers())) {
                userToGroup.put(s.toUpperCase(), g.getName());
            }
        }
        lock.writeLock().unlock();
    }

    public Map<String, Access> getGroup(String key){
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
        Map<String, Access> accessMap = getGroup(group);
        List<String> result = new ArrayList<>();
        if(accessMap != null) {
            for (Map.Entry<String, Access> e : accessMap.entrySet()) {
                if (!e.getValue().getAccess().equals(Access.Type.NO_ACCESS)) {
                    result.add(e.getKey());
                }
            }
        }
        lock.readLock().unlock();
        return result;
    }

    public List<Permission> getAccessibleFields(Object target, String username){
        return getAccessibleFields(target.getClass().getName(), username);
    }

    public List<Permission> getAccessibleFields(Class clazz, String username){
        return getAccessibleFields(clazz.getName(), username);
    }

    public List<Permission> getAccessibleFields(String className, String username){
        return getAccessibleFieldsForGroup(className, getUsersGroup(username));
    }

    public List<Permission> getAccessibleFieldsForGroup(Object target, String group){
        return getAccessibleFieldsForGroup(target.getClass().getName(), group);
    }

    public List<Permission> getAccessibleFieldsForGroup(Class clazz, String group){
        return getAccessibleFieldsForGroup(clazz.getName(), group);
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
                for (Permission p : FilterUtil.checkNull(access.getPermissions())) {
                    if (!p.getPermission().equals(Permission.Type.NO_ACCESS)) {
                        results.add(new Permission(p));
                    }
                }
            }
        }
        lock.readLock().unlock();
        return results;
    }

    public Access getAccess(Object target, String username) throws PropertyFilterException {
        return getAccess(target.getClass().getName(), username);
    }

    public Access getAccess(Class clazz, String username) throws PropertyFilterException {
        return getAccess(clazz.getName(), username);
    }

    public Access getAccess(String className, String username) throws PropertyFilterException {
        String userGroup = getUsersGroup(username);
        if(userGroup == null) return null;
        return getAccessForGroup(className, userGroup);
    }

    public Access getAccessForGroup(Object target, String groupName) throws PropertyFilterException {
        return getAccessForGroup(target.getClass().getName(), groupName);
    }

    public Access getAccessForGroup(Class clazz, String groupName) throws PropertyFilterException {
        return getAccessForGroup(clazz.getName(), groupName);
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
        Set<Field> fields = FilterUtil.getAllFields(object);
        Map<String, Access> accessMap = getGroup(groupName);
        if(accessMap == null) {
            LOGGER.error("Could not find group {}", groupName);
            throw new PropertyFilterException(String.format("Could not find group %s", groupName));
        }

        T obj = (T) FilterUtil.instantiateObject(object.getClass()); // Create a blank object to fill with values
        try {
            Access access = accessMap.get(object.getClass().getName());
            if(access == null) throw new FilterException("Access missing for class of type " +object.getClass().getName());
            if(!access.getAccess().equals(Access.Type.NO_ACCESS)) {
                for (Field f : fields) {
                    if (FilterUtil.isFieldReadable(f.getName(), access) ) {
                        PropertyDescriptor pd = new PropertyDescriptor(f.getName(), object.getClass());
                        Object value = pd.getReadMethod().invoke(object);
                        if (!Collection.class.isAssignableFrom(f.getType())) {
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
            }
            else {
                return null; // We just want to null out any values the user doesn't have access to as these could be sub properties of a main class they do have access to
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
        Set<Field> fields = FilterUtil.getAllFields(newObject);
        Map<String, Access> accessMap = getGroup(groupName);
        if(accessMap == null) throw new FilterException(String.format("Group %s does not exist in current groups map", groupName));

        if(existingObject == null) existingObject = (T) FilterUtil.instantiateObject(newObject.getClass());

        Access access = accessMap.get(newObject.getClass().getName());
        if(access == null) throw new FilterException(String.format("No access defined for class %s and group %s", newObject.getClass().getName(), groupName));
        if (!access.getAccess().equals(Access.Type.NO_ACCESS) && !access.getAccess().equals(Access.Type.READ)) {
            try {
                for (Field f : fields) {
                    if(FilterUtil.isFieldWritable(f.getName(), access)) {
                        PropertyDescriptor pd = new PropertyDescriptor(f.getName(), newObject.getClass());
                        Object newValue = pd.getReadMethod().invoke(newObject);
                        if (!Collection.class.isAssignableFrom(f.getType())) {
                            pd.getWriteMethod().invoke(existingObject, newValue);
                        }
                        else {
                            if(newValue == null){
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
        }
        else {
            LOGGER.warn("User attempting to parse object {}, which they do not have access to", newObject.getClass().getSimpleName());
            throw new IllegalAccessException("Trying to save an object for which the user has no access");
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
