package uk.co.agware.filter.util;

import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.agware.filter.annotations.*;
import uk.co.agware.filter.data.Access;
import uk.co.agware.filter.data.AccessType;
import uk.co.agware.filter.data.Permission;
import uk.co.agware.filter.data.PermissionType;
import uk.co.agware.filter.exceptions.FilterException;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 20/02/2016.
 */
public class FilterUtil {

    private final Logger LOGGER = LoggerFactory.getLogger(FilterUtil.class);
    private AccessType DEFAULT_ACCESS_TYPE = AccessType.NO_ACCESS;
    private PermissionType DEFAULT_PERMISSION_TYPE = PermissionType.NO_ACCESS;
    private ClassFactory<? extends Access<? extends Permission>, ? extends Permission> classFactory;

    public FilterUtil(ClassFactory<? extends Access<? extends Permission>, ? extends Permission> classFactory) {
        this.classFactory = classFactory;
    }

    public ClassFactory<? extends Access<? extends Permission>, ? extends Permission> getClassFactory() {
        return classFactory;
    }

    public void setDefaultAccessType(AccessType type){
        DEFAULT_ACCESS_TYPE = type;
    }

    public void setDefaultPermissionType(PermissionType defaultPermissionType) {
        DEFAULT_PERMISSION_TYPE = defaultPermissionType;
    }

    public boolean isFieldReadable(String fieldName, Access<? extends Permission> access){
        for(Permission p : nullSafe(access.getPermissions())){
            if(p.getPropertyName().equals(fieldName)){
                return !p.getPermission().equals(PermissionType.NO_ACCESS);
            }
        }
        LOGGER.error("No permission defined for field {} on object {}", fieldName, access.getObjectClass());
        throw new FilterException(String.format("No permission defined for field %s on object %s", fieldName, access.getObjectClass()));
    }

    public boolean isFieldWritable(String fieldName, Access<? extends Permission> access){
        for(Permission p : nullSafe(access.getPermissions())){
            if(p.getPropertyName().equals(fieldName)){
                return !p.getPermission().equals(PermissionType.NO_ACCESS) && !p.getPermission().equals(PermissionType.READ);
            }
        }
        LOGGER.error("No permission defined for field {} on object {}", fieldName, access.getObjectClass());
        throw new FilterException(String.format("No permission defined for field %s on object %s", fieldName, access.getObjectClass()));
    }

    public Set<Field> getAllFields(Object o){
        return getAllFields(o.getClass());
    }

    public Set<Field> getAllFields(Class<?> c){
        Set<Field> fields = new HashSet<>();
        Class<?> clazz = c;
        do {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        } while (clazz != null);
        return fields;
    }

    @SuppressWarnings("unchecked")
    public List<Class<?>> getAllClasses(String path){
        Reflections reflections = new Reflections(path, new SubTypesScanner(false));
        Set<Class<?>> classes = reflections.getSubTypesOf(Object.class);
        return new ArrayList<>(classes);
    }

    public List<Class<?>> getAllAvailableClasses(List<Class<?>> classes){
        return classes.stream()
                      .filter(c -> !Modifier.isAbstract(c.getModifiers()) && c.isAnnotationPresent(FilterTarget.class))
                      .collect(Collectors.toList());
    }

    public List<Class<?>> getAllIgnoredClasses(String path){
        return getAllClasses(path).stream()
                                  .filter(c -> c.isAnnotationPresent(FilterIgnored.class))
                                  .collect(Collectors.toList());
    }

    /* Returns a complete list of all non-hidden objects and fields for all classes */
    public <T extends Access<? extends Permission>> List<T> getFullAccessList(String path){
        List<Class<?>> classes = getAllAvailableClasses(getAllClasses(path));
        List<T> objects = new ArrayList<>();
        for(Class<?> c : classes){
            T access = createDefaultAccessFromClass(c);
            objects.add(access);
        }
        Collections.sort(objects);
        return objects;
    }

    @SuppressWarnings("unchecked")
    public <T extends Access> T createDefaultAccessFromClass(Class<?> c){
        Access access = buildBaseAccess(c);
        List<Permission> permissions = new ArrayList<>();
        for(Field f : getAllFields(c)){
            Permission permission = classFactory.createPermissionClass();
            if(f.isAnnotationPresent(NoAccess.class)){
                permission.setPermission(PermissionType.NO_ACCESS);
                permission.setModifiable(false);
            }
            else if(f.isAnnotationPresent(ReadOnly.class)){
                permission.setPermission(PermissionType.READ);
                permission.setModifiable(false);
            }
            else if(f.isAnnotationPresent(Write.class)){
                permission.setPermission(PermissionType.WRITE);
                permission.setModifiable(false);
            }
            else {
                permission.setPermission(DEFAULT_PERMISSION_TYPE);
                permission.setModifiable(true);
            }
            permission.setPropertyName(f.getName());
            permission.setDisplayName(buildDisplayName(f.getName()));
            permissions.add(permission);
        }
        Collections.sort(permissions);
        access.setPermissions(permissions);
        return (T) access;
    }

    public <T extends Access<? extends Permission>> T buildBaseAccess(Class<?> c){
        T access = (T) classFactory.createAccessClass();
        access.setObjectClass(c.getName());

        FilterTarget ft = c.getAnnotation(FilterTarget.class);
        if(ft == null) throw new FilterException(String.format("Class %s appeared without a FilterTarget Annotation present", c.getName()));
        access.setDisplayName("".equals(ft.value()) ? c.getName() : ft.value());

        if(c.isAnnotationPresent(ReadOnly.class)) {
            access.setAccess(AccessType.READ);
            access.setModifiable(false);
        }
        else if(c.isAnnotationPresent(Update.class)) {
            access.setAccess(AccessType.UPDATE);
            access.setModifiable(false);
        }
        else if(c.isAnnotationPresent(Create.class)) {
            access.setAccess(AccessType.CREATE);
            access.setModifiable(false);
        }
        else if(c.isAnnotationPresent(NoAccess.class)) {
            access.setAccess(AccessType.NO_ACCESS);
            access.setModifiable(false);
        }
        else {
            access.setAccess(DEFAULT_ACCESS_TYPE);
            access.setModifiable(true);
        }
        return access;
    }

    /* Static Helper Methods */
    public static Object instantiateObject(Class<?> clazz) {
        if(Collection.class.isAssignableFrom(clazz)){
            return instantiateCollection(clazz);
        }
        try {
            MethodHandle methodHandle = MethodHandles.lookup().findConstructor(clazz, MethodType.methodType(void.class));
            return methodHandle.invoke();
        } catch (NoSuchMethodException e) {
            throw new FilterException("Unable to find constructor for class " +clazz.getName());
        } catch (IllegalAccessException e) {
            throw new FilterException("Unable to access default constructor for class " +clazz.getName());
        } catch (Throwable throwable) {
            throw new FilterException(String.format("Class %s threw an exception during instantiation of default constructor", clazz.getName()), throwable);
        }
    }

    public static <T> Collection<T> instantiateCollection(Class<?> collection){
        if(Set.class.isAssignableFrom(collection)){
            return new HashSet<>();
        }
        return new ArrayList<>();
    }

    // Returns and empty list if the collection passed in is null
    public static <T> Collection<T> nullSafe(Collection<T> collection){
        return collection == null ? Collections.emptyList() : collection;
    }

    public static <T> Stream<T> nullSafeStream(Collection<T> collection){
        return nullSafe(collection).stream();
    }

    public static String capitalizeFirst(String input){
        if(input == null) return null;
        if(input.length() < 2) return input.toUpperCase();
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    public static String buildDisplayName(String input){
        if(input == null) return "";
        String name = capitalizeFirst(input);
        String[] words = StringUtils.splitByCharacterTypeCamelCase(capitalizeFirst(name));
        if(words == null || words.length == 0) return "";

        StringBuilder s = new StringBuilder(words[0]);
        for (int i = 1; i < words.length; i++) {
            s.append(" ");
            s.append(words[i]);
        }
        return s.toString();
    }
}
