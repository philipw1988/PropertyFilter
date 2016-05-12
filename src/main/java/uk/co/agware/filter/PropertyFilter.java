package uk.co.agware.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.agware.filter.objects.Access;
import uk.co.agware.filter.objects.Permission;
import uk.co.agware.filter.objects.Group;
import uk.co.agware.filter.util.FilterUtil;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 9/04/2016.
 */
//TODO Need to handle errors a bit better, probably worth creating an exception to throw for the parse methods
//TODO I think I only half did the ReadWriteLock stuff...
//TODO Put better commends on methods to explain what they're doing
public class PropertyFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertyFilter.class);
    private static final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final List<Class> ignoredClasses;

    private final Map<String, Map<String, Access>> groups;
    private final Map<String, String> userToGroup;

    public PropertyFilter() {
        this.ignoredClasses = new ArrayList<>(Arrays.asList(String.class, Integer.class, Double.class, Float.class, BigDecimal.class, Boolean.class, Byte.class, Date.class)); // Not efficient, but a lazy way to do it in one line
        this.groups = new HashMap<>();
        this.userToGroup = new HashMap<>();
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

    public Access getAccess(Object target, String username){
        return getAccess(target.getClass().getName(), username);
    }

    public Access getAccess(Class clazz, String username){
        return getAccess(clazz.getName(), username);
    }

    public Access getAccess(String className, String username){
        String userGroup = getUsersGroup(username);
        if(userGroup == null) return null;
        Map<String, Access> accessMap = groups.get(userGroup);
        if(accessMap == null) return null;
        Access access = accessMap.get(className);
        if(access == null) return null;

        return access;
    }

    //TODO Handle lists
    @SuppressWarnings("unchecked")
    public <T> T parseObjectForReturn(T object, String username){
        String userGroup = getUsersGroup(username);
        if(userGroup == null){
            LOGGER.warn("User {} has no group", username);
            return null;
        }

        Set<Field> fields = FilterUtil.getAllFields(object);
        Map<String, Access> accessMap = getGroup(userGroup);
        if(accessMap == null) {
            LOGGER.error("No Access defined for object {} in group {}", object.getClass().getName(), userGroup);
            return null;
        }

        T obj = (T) FilterUtil.instantiateObject(object.getClass()); // Create a blank object to fill with values
        if(obj == null) return null;
        try {
            Access access = accessMap.get(object.getClass().getName());
            if(access != null && !access.getAccess().equals(Access.Type.NO_ACCESS)) {
                for (Field f : fields) {
                    if (FilterUtil.isFieldReadable(f.getName(), access) ) { //TODO handle collections
                        PropertyDescriptor pd = new PropertyDescriptor(f.getName(), object.getClass());
                        Object value = pd.getReadMethod().invoke(object);
                        if (!Collection.class.isAssignableFrom(f.getType())) {
                            pd.getWriteMethod().invoke(obj, value);
                        }
                        else {
                            if(value != null){
                                pd.getWriteMethod().invoke(obj, handleCollectionForReturn((Collection) value, username));
                            }
                        }
                    }
                }
            }
            else {
                return null;
            }
        } catch (IntrospectionException | InvocationTargetException | IllegalAccessException e) {
            LOGGER.error(e.getMessage(), e);
            return null;
        }
        return obj;
    }

    @SuppressWarnings("unchecked")
    private Collection handleCollectionForReturn(Collection collection, String username){
        Collection result = (Collection) FilterUtil.instantiateObject(collection.getClass());
        if(result == null) return null;
        for(Object o : collection){
            if(ignoredClasses.contains(o.getClass())){
                result.add(o);
            }
            else {
                result.add(parseObjectForReturn(o, username));
            }
        }
        return result;
    }

    //TODO doesn't alert to missing permissions
    //TODO Worry about maps
    public boolean parseObjectForSaving(Object newObject, Object existingObject, String username) throws IllegalAccessException {
        String userGroup = getUsersGroup(username);
        if(userGroup == null) {
            LOGGER.debug("User {} has no group", username);
            return false;
        }

        Set<Field> fields = FilterUtil.getAllFields(newObject);
        Map<String, Access> accessMap = getGroup(userGroup);

        Access access = accessMap.get(newObject.getClass().getName());
        if (access != null && !access.getAccess().equals(Access.Type.NO_ACCESS) && !access.getAccess().equals(Access.Type.READ)) {
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
                                Collection newCollection = (Collection)newValue;
                                Collection existingCollection = (Collection) pd.getReadMethod().invoke(existingObject);
                                handleCollectionsForSaving(existingCollection, newCollection, username);
                            }
                        }
                    }
                }
            } catch (IntrospectionException | InvocationTargetException | IllegalAccessException e) {
                LOGGER.error(e.getMessage(), e);
                return false;
            }
        }
        else {
            LOGGER.warn("User {} attempting to parse object {}, which they do not have access to", username, newObject.getClass().getSimpleName());
            throw new IllegalAccessException("Trying to save an object for which the user has no access");
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private boolean handleCollectionsForSaving(Collection exitingCollection, Collection newCollection, String username){
        // If existing collection is null, create a new list to store all the values in
        if(exitingCollection == null) {
            exitingCollection = (Collection) FilterUtil.instantiateObject(newCollection.getClass());
            if(exitingCollection == null){
                return false;
            }
        }
        // For everything in the exiting collection, if new collection doesn't contain it, remove it
        for(Iterator<Object> itr = exitingCollection.iterator(); itr.hasNext();){
            Object o = itr.next();
            if(!newCollection.contains(o)){
                itr.remove();
            }
        }
        for(Object newVal : newCollection){
            // For everything in the new collection, check it isn't in the primitive type
            if(ignoredClasses.contains(newVal.getClass()) && !exitingCollection.contains(newVal)){ // If it's a type that wont have an access type set for it, i.e. a "primitive" type where you just want the value as is
                exitingCollection.add(newVal);
            }
            else {
                // if not then find the one in the existing collection
                Object existingVal = null;
                for(Object o : exitingCollection){
                    if(o.equals(newVal)){
                        existingVal = o;
                        break;
                    }
                }
                // Run the save method over the two of them
                try {
                    if(existingVal == null){
                        existingVal = FilterUtil.instantiateObject(newVal.getClass());
                    }
                    parseObjectForSaving(newVal, existingVal, username);
                    exitingCollection.add(existingVal);
                } catch (IllegalAccessException e) {
                    LOGGER.error(e.getMessage(), e);
                    return false;
                }
            }
        }
        return true;
    }
}
