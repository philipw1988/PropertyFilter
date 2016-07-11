package uk.co.agware.filter.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.agware.filter.PropertyFilter;
import uk.co.agware.filter.data.Access;
import uk.co.agware.filter.data.Group;
import uk.co.agware.filter.data.Permission;
import uk.co.agware.filter.exceptions.PropertyFilterException;
import uk.co.agware.filter.persistence.FilterRepository;
import uk.co.agware.filter.util.FilterUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 24/06/2016.
 */
public class FilterService {

    private final Logger logger = LoggerFactory.getLogger(FilterService.class);

    private PropertyFilter propertyFilter;
    private FilterRepository repository;
    private Set<String> packagesToScan;
    private Map<String, String> staticGroupAllocation;
    private List<Group> runTimeGroups;

    FilterService(PropertyFilter propertyFilter, FilterRepository repository, Set<String> packagesToScan, Map<String, String> staticGroupAllocation, List<Group> runTimeGroups) {
        this.propertyFilter = propertyFilter;
        this.repository = repository;
        this.packagesToScan = packagesToScan;
        this.staticGroupAllocation = staticGroupAllocation;
        this.runTimeGroups = runTimeGroups;
    }

    public void init(){
        List<Group> groups = repository.initGroups();
        List<Access> allClasses = new ArrayList<>();
        for(String s : packagesToScan){
            allClasses.addAll(FilterUtil.nullSafe(propertyFilter.getFilterUtil().getFullAccessList(s)));
        }
        // For each saved group, check for updates to the access objects and then re-save
        FilterUtil.nullSafe(groups).forEach(group -> {
            /* The .orElse(null) calls used in the streams in these methods should be safe due to the order in which objects are being called and used */
            removeOldClasses(group, allClasses);
            addNewAccess(group, allClasses);
            repository.save(group);
        });
        // Add all extra ignored classes to the property filter's list
        packagesToScan.forEach(s ->
                propertyFilter.getFilterUtil().getAllIgnoredClasses(s).forEach(c ->
                        propertyFilter.addIgnoredClass(c)
                )
        );
        setGroups(groups);
    }

    /* Removes classes which are no longer present in the package scans */
    private Group removeOldClasses(Group group, List<Access> allClasses){
        // Split the group's access classes into ones that are in the allClasses list and ones that aren't
        Map<Boolean, List<Access>> accessGroups = FilterUtil.nullSafeStream(group.getAccess()).collect(Collectors.partitioningBy(allClasses::contains));
        // Log the removed ones
        accessGroups.get(Boolean.FALSE).forEach(access -> logger.info("Removing old access for class {} from group {}", access.getObjectClass(), group.getName()));
        // These are ones in the allClasses group
        List<Access> remainingAccess = accessGroups.get(Boolean.TRUE);

        remainingAccess.forEach(access -> {
            // Get the matching access from the "all" list
            Access matchingAllAccess = allClasses.stream().filter(a -> access.getObjectClass().equals(a.getObjectClass())).findFirst().orElse(null);
            // Split into exists and doesn't exist
            Map<Boolean, List<Permission>> permissionGroups = FilterUtil.nullSafeStream(access.getPermissions()).collect(Collectors.partitioningBy(p -> FilterUtil.nullSafe(matchingAllAccess.getPermissions()).contains(p)));
            // Log the removed ones
            permissionGroups.get(Boolean.FALSE).forEach(permission -> logger.info("Removing old permission for field {} on class {} in group {}", permission.getPropertyName(), access.getObjectClass(), group.getName()));
            // Add the survivors back into the access
            access.setPermissions(permissionGroups.get(Boolean.TRUE));
        });
        group.setAccess(remainingAccess);
        return group;
    }

    /* Updates the access list on each group, modifies the group's list directly */
    private Group addNewAccess(Group group, List<Access> allAccess){
        // Split the all access list into ones that exist and ones that don't as they're processed differently
        Map<Boolean, List<Access>> accessGroups = allAccess.stream().collect(Collectors.partitioningBy(a -> FilterUtil.nullSafe(group.getAccess()).contains(a)));
        // Split the existing access list into hard coded (from an annotation) or not
        Map<Boolean, List<Access>> modifiableSplit = accessGroups.get(Boolean.TRUE).stream().collect(Collectors.partitioningBy(Access::isModifiable));
        // Update all modifiable access objects to be correct
        modifiableSplit.get(Boolean.TRUE).stream().forEach(access -> {
            // Get the matching access from the group and make sure its modifiable value is true
            group.getAccess().stream().filter(a -> access.getObjectClass().equals(a.getObjectClass())).findFirst().orElse(null).setModifiable(true);
        });
        // Update any unmodifiable values to be correct
        modifiableSplit.get(Boolean.FALSE).forEach(access -> {
            Access matchingGroupAccess = FilterUtil.nullSafeStream(group.getAccess()).filter(a -> access.getObjectClass().equals(a.getObjectClass())).findFirst().orElse(null);
            if(matchingGroupAccess.isModifiable()){
                matchingGroupAccess.setModifiable(false);
            }
            matchingGroupAccess.setAccess(access.getAccess());
        });
        // Any new access objects can simply be logged and added to the list
        accessGroups.get(Boolean.FALSE).forEach(access -> {
            logger.info("Adding access for class {} to group {}", access.getObjectClass(), group.getName());
            group.getAccess().add(access);
        });
        group.getAccess().forEach(access -> {
            Access matchingAllAccess = allAccess.stream().filter(a -> a.getObjectClass().equals(access.getObjectClass())).findFirst().orElse(null);
            updateAccessPermissions(access, matchingAllAccess.getPermissions(), group.getName());
        });
        return group;
    }

    /* Updates and adds permissions to an access class, works directly on the access's list */
    private Access updateAccessPermissions(Access access, List<Permission> allPermissions, String groupName){
        // Split into existing and not existing
        Map<Boolean, List<Permission>> permissionGroups = FilterUtil.nullSafeStream(allPermissions).collect(Collectors.partitioningBy(p -> FilterUtil.nullSafe(access.getPermissions()).contains(p)));
        // Split into modifiable and not
        Map<Boolean, List<Permission>> modifiableSplit = permissionGroups.get(Boolean.TRUE).stream().collect(Collectors.partitioningBy(Permission::isModifiable));
        // Set modifiable value to true for all properties
        modifiableSplit.get(Boolean.TRUE).forEach(permission -> FilterUtil.nullSafeStream(access.getPermissions()).filter(p -> p.getPropertyName().equals(permission.getPropertyName())).findFirst().orElse(null).setModifiable(true));
        // Update unmodifiable values to be correct
        modifiableSplit.get(Boolean.FALSE).forEach(permission -> {
            Permission matchingAccessPermission = FilterUtil.nullSafeStream(access.getPermissions()).filter(p -> p.getPropertyName().equals(permission.getPropertyName())).findFirst().orElse(null);
            matchingAccessPermission.setModifiable(false);
            matchingAccessPermission.setPermission(permission.getPermission());
        });
        // Add all the permissions in that didn't exist
        permissionGroups.get(Boolean.FALSE).forEach(permission -> {
            logger.info("Adding in permission for field {} on class {} for group {}", permission.getPropertyName(), access.getObjectClass(), groupName);
            access.getPermissions().add(permission);
        });
        return access;
    }

    private void setGroups(List<Group> groups){
        groups.addAll(runTimeGroups); // Add in the runtime specified groups as well
        propertyFilter.setGroups(groups);
        // Add the static allocations into the group map, this is mainly for either overrides, or virtual users such as system users that might need a group
        staticGroupAllocation.entrySet().forEach(e -> propertyFilter.addUserToGroup(e.getKey(), e.getValue()));
    }

    public Group getGroup(String id){
        return repository.getGroup(id);
    }

    public List<Group> getGroups(){
        return repository.getGroups();
    }

    public void deleteGroup(String id){
        repository.delete(id);
    }

    public void saveGroup(Group group){
        repository.save(group);
        List<Group> groups = repository.getGroups();
        setGroups(groups);
    }

    /* Delegating calls to PropertyFilter */

    public Access getAccessForGroup(Object target, String groupName) throws PropertyFilterException {
        return getAccessForGroup(target.getClass().getName(), groupName);
    }

    public Access getAccessForGroup(Class clazz, String groupName) throws PropertyFilterException {
        return getAccessForGroup(clazz.getName(), groupName);
    }

    public Access getAccessForGroup(String className, String groupName) throws PropertyFilterException {
        return propertyFilter.getAccessForGroup(className, groupName);
    }

    public Access getAccess(Object target, String username) throws PropertyFilterException {
        return getAccess(target.getClass().getName(), username);
    }

    public Access getAccess(Class clazz, String username) throws PropertyFilterException {
        return getAccess(clazz.getName(), username);
    }

    public Access getAccess(String className, String username) throws PropertyFilterException {
        return propertyFilter.getAccess(className, username);
    }

    public List<Permission> getAccessibleFields(Object target, String username) throws PropertyFilterException {
        return getAccessibleFields(target.getClass().getName(), username);
    }

    public List<Permission> getAccessibleFields(Class clazz, String username) throws PropertyFilterException {
        return getAccessibleFields(clazz.getName(), username);
    }

    public List<String> getAccessibleClasses(String username) throws PropertyFilterException {
        return propertyFilter.getAccessibleClassesForGroup(propertyFilter.getUsersGroup(username));
    }

    public List<Permission> getAccessibleFields(String className, String username) throws PropertyFilterException {
        return getAccessibleFieldsForGroup(className, propertyFilter.getUsersGroup(username));
    }

    public List<Permission> getAccessibleFieldsForGroup(Object target, String group) throws PropertyFilterException {
        return getAccessibleFieldsForGroup(target.getClass().getName(), group);
    }

    public List<Permission> getAccessibleFieldsForGroup(Class clazz, String group) throws PropertyFilterException {
        return getAccessibleFieldsForGroup(clazz.getName(), group);
    }

    public List<Permission> getAccessibleFieldsForGroup(String className, String group) throws PropertyFilterException {
        return propertyFilter.getAccessibleFieldsForGroup(className, group);
    }
}
