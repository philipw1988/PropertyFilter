package uk.co.agware.filter.util;

import com.google.common.reflect.ClassPath;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.agware.filter.annotations.Hidden;
import uk.co.agware.filter.annotations.ReadOnly;
import uk.co.agware.filter.objects.Access;
import uk.co.agware.filter.objects.Permission;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 20/02/2016.
 */
public class ClassUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClassUtil.class);
    private static Access.Type DEFAULT_ACCESS_TYPE = Access.Type.NO_ACCESS;
    private static Permission.Type DEFAULT_PERMISSION_TYPE = Permission.Type.NO_ACCESS;

    public static void setDefaultAccessType(Access.Type type){
        DEFAULT_ACCESS_TYPE = type;
    }

    public static void setDefaultPermissionType(Permission.Type defaultPermissionType) {
        DEFAULT_PERMISSION_TYPE = defaultPermissionType;
    }

    public static Object instantiateObject(Class clazz){
        Constructor[] constructors = clazz.getDeclaredConstructors();
        for(Constructor c : constructors){
            if(c.getGenericParameterTypes().length == 0){
                try {
                    return c.newInstance();
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
        LOGGER.error("No Default Constructor found for class {}", clazz);
        return null;
    }


    // Returns and empty list of the list passed in is null
    public static <T> Collection<T> checkNull(Collection<T> collection){
        return collection == null ? Collections.emptyList() : collection;
    }

    public static boolean isFieldReadable(String fieldName, Access access){
        for(Permission p : checkNull(access.getPermissions())){
            if(p.getPropertyName().equals(fieldName)){
                return !p.getPermission().equals(Permission.Type.NO_ACCESS);
            }
        }
        LOGGER.error("No permission defined for field {} on object {}", fieldName, access.getObjectClass());
        return false;
    }

    public static boolean isFieldWritable(String fieldName, Access access){
        for(Permission p : checkNull(access.getPermissions())){
            if(p.getPropertyName().equals(fieldName)){
                return !p.getPermission().equals(Permission.Type.NO_ACCESS) && !p.getPermission().equals(Permission.Type.READ);
            }
        }
        return true;
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
            List<Class> classes = new ArrayList<>(classInfo.size());
            for(ClassPath.ClassInfo c : classInfo){
                classes.add(c.load());
            }
            return classes;
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            return Collections.EMPTY_LIST;
        }
    }

    public static List<Class> getAllNonHiddenClasses(List<Class> classes){
        List<Class> result = new ArrayList<>();
        for(Class c : classes){
            if( !(Modifier.isAbstract(c.getModifiers()) || c.isAnnotationPresent(Hidden.class)) ){
                result.add(c);
            }
        }
        return result;
    }

    /* Returns a complete list of all non-hidden objects and fields for all classes */
    public static List<Access> getFullAccessList(String path){
        List<Class> classes = getAllNonHiddenClasses(getAllClasses(path));
        List<Access> objects = new ArrayList<>();
        for(Class c : classes){
            Access access = createDefaultAccessFromClass(c);
            objects.add(access);
        }
        Collections.sort(objects);
        return objects;
    }

    public static Access createDefaultAccessFromClass(Class c){
        Access access = new Access();
        access.setObjectClass(c.getName());
        access.setAccess(DEFAULT_ACCESS_TYPE);
        List<Permission> permissions = new ArrayList<>();
        for(Field f : getAllFields(c)){
            if(!f.isAnnotationPresent(Hidden.class)){
                Permission permission = new Permission();
                if(f.isAnnotationPresent(ReadOnly.class)){
                    permission.setPermission(Permission.Type.READ);
                }
                else {
                    permission.setPermission(DEFAULT_PERMISSION_TYPE);
                }
                permission.setPropertyName(f.getName());
                permission.setDisplayName(buildDisplayName(f.getName()));
                permissions.add(permission);
            }
        }
        Collections.sort(permissions);
        access.setPermissions(permissions);
        return access;
    }

    public static String capitalizeFirst(String input){
        if(input == null) return null;
        if(input.length() < 2) return input.toUpperCase();
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    public static String buildDisplayName(String input){
        if(input == null) return null;
        String name = capitalizeFirst(input);
        String[] words = StringUtils.splitByCharacterTypeCamelCase(capitalizeFirst(name));
        if(words != null && words.length > 0) {
            StringBuilder s = new StringBuilder(words[0]);
            for (int i = 1; i < words.length; i++) {
                s.append(" ");
                s.append(words[i]);
            }
            return s.toString();
        }
        return "";
    }
}
