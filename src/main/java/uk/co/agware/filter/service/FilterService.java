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
 * Holds a {@link PropertyFilter} and provides a default implementation for managing the
 * security groups in the system.
 *
 * Created by Philip Ward <Philip.Ward@agware.com> on 24/06/2016.
 */
public class FilterService {

    private final Logger logger = LoggerFactory.getLogger(FilterService.class);

    private PropertyFilter propertyFilter;
    private FilterRepository repository;
    private Set<String> packagesToScan;
    private Map<String, String> staticGroupAllocation;
    private List<Group> runTimeGroups;

    /* Package local constructor for builder to use */
    FilterService(PropertyFilter propertyFilter, FilterRepository repository, Set<String> packagesToScan, Map<String, String> staticGroupAllocation, List<Group> runTimeGroups) {
        this.propertyFilter = propertyFilter;
        this.repository = repository;
        this.packagesToScan = packagesToScan;
        this.staticGroupAllocation = staticGroupAllocation;
        this.runTimeGroups = runTimeGroups;
    }

    /**
     * Initializes the groups that have been saved in the system by calling {@link FilterRepository#initGroups()}
     * to retrieve a list of stored groups in the system, also performs checks on each of the groups to
     * check for any old permissions that do not exist any more, i.e. Ones that have been deleted, and also
     * for new ones that weren't in the groups when they were saved. For any old permissions they are removed from the
     * group, and any new ones are added to it, with the default security access that was set in the {@link FilterUtil}
     * as the access set for all groups, unless the property has been annotated with one of the provided classes from
     * {@link uk.co.agware.filter.annotations}.
     *
     * It also checks all specified paths for any values that are marked with {@link uk.co.agware.filter.annotations.FilterIgnored}
     * and adds those to the list in {@link PropertyFilter} that stops it from attempting to process the values in that class.
     */
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

    /**
     * Uses the master list of Access objects from the PropertyFilter to check
     * a group for old permissions that need to be cleaned up. This method will
     * replace the contents of the existing collection within the group
     * rather than setting a new one.
     *
     * @param group The group to check the access on
     * @param allClasses The master list of available classes
     * @return The group object with the correct values set
     */
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
            access.getPermissions().clear();
            access.getPermissions().addAll(permissionGroups.get(Boolean.TRUE));
        });
        group.getAccess().clear();
        group.getAccess().addAll(remainingAccess);
        return group;
    }

    /**
     * Updates a group with any new access and permissions that were not present in the saved group.
     * First processes any existing access values by whether or not they are modifiable, which means that
     * they have an annotation present. This pass through simply ensures that the value is set to the correct
     * one, for example if an annotation was present but has been removed, then it will be updated to be false.
     * After this process is complete it will then check all existing access values to see if they have any new
     * permission values that need to be added.
     * This method adds directly to the list in the group rather than adding a new list to the group after completion.
     *
     * @param group The group being processed
     * @param allAccess The master access list
     * @return The group with the updated access list
     */
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

    /**
     * Updates and adds permissions to an access class, this method
     * works directly on the access's list rather than setting
     * a new list on the entity
     *
     * @param access The access object to update the permissions on
     * @param allPermissions The Master list of permissions for a class
     * @param groupName The name of the group being changed
     * @return The updated {@link Access} entity
     */
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

    /**
     * Sets the groups in the {@link PropertyFilter}, first adding
     * the {@code runTimeGroups} to the supplied list and then
     * passing this list into the {@link PropertyFilter} where the
     * groups will be refreshed.
     *
     * @param groups The list of groups to be set in the filter
     */
    private void setGroups(List<Group> groups){
        groups.addAll(runTimeGroups); // Add in the runtime specified groups as well
        propertyFilter.setGroups(groups);
        // Add the static allocations into the group map, this is mainly for either overrides, or virtual users such as system users that might need a group
        staticGroupAllocation.entrySet().forEach(e -> propertyFilter.addUserToGroup(e.getKey(), e.getValue()));
    }

    /**
     * Adds a static allocation to a group, adds the allocation to the
     * held static list, and also updates the mapping in the {@link PropertyFilter},
     * this mapping will remain through refreshes of the groups until the
     * application is shut down, another mapping replaces it,
     * or {@link #removeGroupAllocation(String)} method is called.
     *
     * @param username The username to add to the group
     * @param group The group to add the user to
     */
    public void addStaticGroupAllocation(String username, String group){
        staticGroupAllocation.put(username, group);
        propertyFilter.addUserToGroup(username, group);
    }

    /**
     * Removes a group allocation from the {@link PropertyFilter}, and additionally
     * the {@link #staticGroupAllocation} if it is present in there, this method only
     * affects the mapping during run time, it does not persist the changes, for that
     * the {@link #saveGroup(Group)} method should be called.
     *
     * @param username The username to remove
     * @return The group name that was associated with the user
     */
    public String removeGroupAllocation(String username){
        staticGroupAllocation.remove(username);
        return propertyFilter.removeUserFromGroup(username);
    }

    /**
     * Returns a {@link Group} from the repository
     *
     * @param id The ID of the group
     * @return The matching {@link Group}
     */
    public Group getGroup(String id){
        return repository.getGroup(id);
    }

    /**
     * Returns a full list of the groups from the repository
     * @return A list of saved groups
     */
    public List<Group> getGroups(){
        return repository.getGroups();
    }

    /**
     * Deletes a group from the repository.
     *
     * @param id The ID of the group to delete
     */
    public void deleteGroup(String id){
        repository.delete(id);
    }

    /**
     * Saves a group to the repository, either a new group or one to be
     * updated. After saving the group it then refreshes the entire list
     * from the repository.
     *
     * @param group The group to be saved
     */
    public Object saveGroup(Group group){
        Object id = repository.save(group);
        List<Group> groups = repository.getGroups();
        setGroups(groups);
        return id;
    }

    /* Delegating calls to PropertyFilter */
    /*
     * All these calls simply extend the limited calls that the property filter accepts
     * for example the PropertyFilter only accepts a class name, while these calls will
     * accept objects and classes directly and then convert that into a string before sending
     * on to the filter.
     * They also convert a call from a username to a "*forGroup()" call by first getting the
     * group name of the user passed in before returning the call to the filter.
     */
    public Access getAccessForGroup(Object target, String groupName) throws PropertyFilterException {
        return getAccessForGroup(target.getClass().getName(), groupName);
    }

    public Access getAccessForGroup(Class clazz, String groupName) throws PropertyFilterException {
        return getAccessForGroup(clazz.getName(), groupName);
    }

    public Access getAccessForGroup(String className, String groupName) throws PropertyFilterException {
        return propertyFilter.getAccess(className, groupName);
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
        return propertyFilter.getAccessibleClasses(propertyFilter.getUsersGroup(username));
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
        return propertyFilter.getAccessibleFields(className, group);
    }
}
