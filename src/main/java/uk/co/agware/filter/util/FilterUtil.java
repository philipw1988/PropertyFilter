package uk.co.agware.filter.util;

import com.google.common.reflect.ClassPath;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.agware.filter.annotations.*;
import uk.co.agware.filter.exceptions.FilterException;
import uk.co.agware.filter.objects.Access;
import uk.co.agware.filter.objects.Permission;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 20/02/2016.
 */
public class FilterUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilterUtil.class);
    private static Access.Type DEFAULT_ACCESS_TYPE = Access.Type.NO_ACCESS;
    private static Permission.Type DEFAULT_PERMISSION_TYPE = Permission.Type.NO_ACCESS;

    // Don't make an instance of this class!
    private FilterUtil(){}

    public static void setDefaultAccessType(Access.Type type){
        DEFAULT_ACCESS_TYPE = type;
    }

    public static void setDefaultPermissionType(Permission.Type defaultPermissionType) {
        DEFAULT_PERMISSION_TYPE = defaultPermissionType;
    }

    public static Object instantiateObject(Class clazz) {
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

    // Returns and empty list if the collection passed in is null
    public static <T> Collection<T> nullSafe(Collection<T> collection){
        return collection == null ? Collections.emptyList() : collection;
    }

    public static boolean isFieldReadable(String fieldName, Access access){
        for(Permission p : nullSafe(access.getPermissions())){
            if(p.getPropertyName().equals(fieldName)){
                return !p.getPermission().equals(Permission.Type.NO_ACCESS);
            }
        }
        LOGGER.error("No permission defined for field {} on object {}", fieldName, access.getObjectClass());
        throw new FilterException(String.format("No permission defined for field %s on object %s", fieldName, access.getObjectClass()));
    }

    public static boolean isFieldWritable(String fieldName, Access access){
        for(Permission p : nullSafe(access.getPermissions())){
            if(p.getPropertyName().equals(fieldName)){
                return !p.getPermission().equals(Permission.Type.NO_ACCESS) && !p.getPermission().equals(Permission.Type.READ);
            }
        }
        LOGGER.error("No permission defined for field {} on object {}", fieldName, access.getObjectClass());
        throw new FilterException(String.format("No permission defined for field %s on object %s", fieldName, access.getObjectClass()));
    }

    public static Set<Field> getAllFields(Object o){
        return getAllFields(o.getClass());
    }

    public static Set<Field> getAllFields(Class c){
        Set<Field> fields = new HashSet<>();
        Class clazz = c;
        do {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        } while (clazz != null);
        return fields;
    }

    @SuppressWarnings("unchecked")
    public static List<Class> getAllClasses(String path){
        try {
            ClassPath classPath = ClassPath.from(Thread.currentThread().getContextClassLoader());
            Set<ClassPath.ClassInfo> classInfo = classPath.getTopLevelClasses(path);
            return classInfo.stream().map(ClassPath.ClassInfo::load).collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new FilterException("An issue occurred while trying to read objects on the ClassPath " +path);
        }
    }

    public static List<Class> getAllAvailableClasses(List<Class> classes){
        return classes.stream().filter(c -> !Modifier.isAbstract(c.getModifiers()) && c.isAnnotationPresent(FilterTarget.class)).collect(Collectors.toList());
    }

    public static List<Class> getAllIgnoredClasses(String path){
        return getAllClasses(path).stream().filter(c -> c.isAnnotationPresent(FilterIgnored.class)).collect(Collectors.toList());
    }

    /* Returns a complete list of all non-hidden objects and fields for all classes */
    public static List<Access> getFullAccessList(String path){
        List<Class> classes = getAllAvailableClasses(getAllClasses(path));
        List<Access> objects = new ArrayList<>();
        for(Class c : classes){
            Access access = createDefaultAccessFromClass(c);
            objects.add(access);
        }
        Collections.sort(objects);
        return objects;
    }

    //TODO Split field part into separate method as well
    public static Access createDefaultAccessFromClass(Class c){
        Access access = buildBaseAccess(c);
        List<Permission> permissions = new ArrayList<>();
        for(Field f : getAllFields(c)){
            Permission permission = new Permission();
            if(f.isAnnotationPresent(NoAccess.class)){
                permission.setPermission(Permission.Type.NO_ACCESS);
                permission.setModifiable(false);
            }
            else if(f.isAnnotationPresent(ReadOnly.class)){
                permission.setPermission(Permission.Type.READ);
                permission.setModifiable(false);
            }
            else if(f.isAnnotationPresent(Write.class)){
                permission.setPermission(Permission.Type.WRITE);
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
        return access;
    }

    public static Access buildBaseAccess(Class c){
        Access access = new Access();
        access.setObjectClass(c.getName());

        FilterTarget ft = (FilterTarget) c.getAnnotation(FilterTarget.class);
        if(ft == null) throw new FilterException(String.format("Class %s appeared without a FilterTarget Annotation present", c.getName()));
        access.setDisplayName("".equals(ft.value()) ? c.getName() : ft.value());

        if(c.isAnnotationPresent(ReadOnly.class)) {
            access.setAccess(Access.Type.READ);
            access.setModifiable(false);
        }
        else if(c.isAnnotationPresent(Update.class)) {
            access.setAccess(Access.Type.UPDATE);
            access.setModifiable(false);
        }
        else if(c.isAnnotationPresent(Create.class)) {
            access.setAccess(Access.Type.CREATE);
            access.setModifiable(false);
        }
        else if(c.isAnnotationPresent(NoAccess.class)) {
            access.setAccess(Access.Type.NO_ACCESS);
            access.setModifiable(false);
        }
        else {
            access.setAccess(DEFAULT_ACCESS_TYPE);
            access.setModifiable(true);
        }
        return access;
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
