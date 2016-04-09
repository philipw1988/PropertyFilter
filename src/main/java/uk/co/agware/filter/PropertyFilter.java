package uk.co.agware.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.agware.filter.objects.Access;
import uk.co.agware.filter.objects.Permission;
import uk.co.agware.filter.objects.SecurityGroup;
import uk.co.agware.filter.util.ClassUtil;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 9/04/2016.
 */
//TODO change to fully qualified package name in lowercase for the keys
public class PropertyFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertyFilter.class);
    private static final ReadWriteLock lock = new ReentrantReadWriteLock();

    private static final Map<String, Map<String, Access>> groups = new HashMap<>();
    private static final Map<String, String> userToGroup = new HashMap<>();

    /**
     * Refreshes the groups listing
     */
    public void load(List<SecurityGroup> securityGroups) {
        lock.writeLock().lock();
        groups.clear();
        userToGroup.clear();
        for (SecurityGroup g : ClassUtil.checkNull(securityGroups)) {
            Map<String, Access> accessMap = new HashMap<>();
            for (Access a : ClassUtil.checkNull(g.getAccess())) {
                accessMap.put(a.getObjectClass(), a);
            }
            groups.put(g.getName(), accessMap);
            for (String s : ClassUtil.checkNull(g.getMembers())) {
                userToGroup.put(s.toUpperCase(), g.getName());
            }
        }
        lock.writeLock().unlock();
    }

    private Map<String, Access> getGroup(String key){
        return groups.get(key);
    }

    public String getUsersGroup(String username){
        lock.readLock().lock();
        String groupName = userToGroup.get(username.toUpperCase());
        lock.readLock().unlock();
        return groupName;
    }

    public List<String> getAccessibleClasses(String username){
        lock.readLock().lock();
        Map<String, Access> accessMap = getGroup(getUsersGroup(username));
        if(accessMap == null) return null;
        List<String> result = new ArrayList<>();
        for(Map.Entry<String, Access> e : accessMap.entrySet()){
            if(!e.getValue().getAccess().equals(Access.Type.NO_ACCESS)){
                result.add(e.getKey());
            }
        }
        lock.readLock().unlock();
        return result;
    }

    public List<Permission> getAccessibleFields(Object target, String username){
        return getAccessibleFields(target.getClass().getName(), username);
    }

    public List<Permission> getAccessibleFields(String className, String username){
        lock.readLock().lock();
        String userGroup = getUsersGroup(username);
        Map<String, Access> accessMap = getGroup(userGroup);
        if(accessMap == null) return null;
        Access access = accessMap.get(className);
        if(access == null) return null;
        List<Permission> results = new ArrayList<>();
        for(Permission p : ClassUtil.checkNull(access.getPermissions())){
            if(!p.getPermission().equals(Permission.Type.NO_ACCESS)){
                results.add(new Permission(p));
            }
        }
        lock.readLock().unlock();
        return results;
    }

    public Access getAccessFromObject(Object target, String username){
        return getAccessFromClass(target.getClass().getName(), username);
    }

    public Access getAccessFromClass(String className, String username){
        String userGroup = getUsersGroup(username);
        if(userGroup == null) return null;
        Map<String, Access> accessMap = groups.get(userGroup);
        if(accessMap == null) return null;
        Access access = accessMap.get(className.toLowerCase());
        if(access == null) return null;

        return access;
    }

    public Object parseObjectForReturn(Object object, String username){
        String userGroup = getUsersGroup(username);
        if(userGroup == null){
            LOGGER.warn("User {} has no group", username);
            return null;
        }

        Set<Field> fields = ClassUtil.getAllFields(object);
        Map<String, Access> accessMap = getGroup(userGroup);
        if(accessMap == null) {
            LOGGER.error("No Access defined for object {} in group {}", object.getClass().getName(), userGroup);
            return null;
        }

        Object obj = ClassUtil.instantiateObject(object.getClass()); // Create a blank object to fill with values //TODO error on no default constructor
        try {
            Access access = accessMap.get(object.getClass().getName());
            if(access != null && !access.getAccess().equals(Access.Type.NO_ACCESS)) {
                for (Field f : fields) {
                    if (ClassUtil.isFieldReadable(f.getName(), access) ) {
                        PropertyDescriptor pd = new PropertyDescriptor(f.getName(), object.getClass());
                        Object value = pd.getReadMethod().invoke(object);
                        if(value instanceof Object){ //TODO
                            value = parseObjectForReturn(value, userGroup);
                        }
                        pd.getWriteMethod().invoke(obj, value);
                    }
                }
            }
        } catch (IntrospectionException | InvocationTargetException | IllegalAccessException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return obj;
    }

    public Object parseObjectForSaving(Object newObject, Object existingObject, String username) throws IllegalAccessException {
        String userGroup = getUsersGroup(username);
        if(userGroup == null) {
            LOGGER.debug("User {} has no group", username);
            return null;
        }

        Set<Field> fields = ClassUtil.getAllFields(newObject);
        Map<String, Access> accessMap = getGroup(userGroup);

        Access access = accessMap.get(newObject.getClass().getSimpleName());
        if (access != null && !access.getAccess().equals(Access.Type.NO_ACCESS) && !access.getAccess().equals(Access.Type.READ)) {
            try {
                for (Field f : fields) {
                    if (!Collection.class.isAssignableFrom(f.getType()) && ClassUtil.isFieldWritable(f.getName(), access)) { // We can skip collections
                        PropertyDescriptor pd = new PropertyDescriptor(f.getName(), newObject.getClass());
                        Object value = pd.getReadMethod().invoke(newObject);
                        pd.getWriteMethod().invoke(existingObject, value);
                    }
                }
            } catch (IntrospectionException | InvocationTargetException | IllegalAccessException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
        else {
            LOGGER.warn("User {} attempting to parse object {}, which they do not have access to", username, newObject.getClass().getSimpleName());
            throw new IllegalAccessException("Trying to save an object for which the user has no access");
        }
        return existingObject;
    }
}
