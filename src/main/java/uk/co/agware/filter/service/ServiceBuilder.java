package uk.co.agware.filter.service;

import uk.co.agware.filter.PropertyFilter;
import uk.co.agware.filter.data.Access;
import uk.co.agware.filter.data.Group;
import uk.co.agware.filter.data.Permission;
import uk.co.agware.filter.impl.PseudoRepository;
import uk.co.agware.filter.persistence.FilterRepository;

import java.util.*;

/**
 * Creates a {@link FilterService} instance which is used to manage the
 * {@link PropertyFilter}.
 * The minimum that needs to be set is a {@link PropertyFilter} and a
 * package to scan, the service will be created with a {@link PseudoRepository}
 * in cases where the groups do not need to be persisted.
 *
 * Created by Philip Ward <Philip.Ward@agware.com> on 24/06/2016.
 */
public class ServiceBuilder {

    private PropertyFilter propertyFilter = null;
    private FilterRepository<? extends Group<? extends Access<? extends Permission>>> repository = new PseudoRepository();
    private Set<String> packagesToScan = null;
    private Map<String, String> staticGroupAllocations = new HashMap<>();
    private List<Group<? extends Access<? extends Permission>>> runTimeGroups = new ArrayList<>();

    /**
     * Initialize the builder with a {@link PropertyFilter}
     *
     * @param propertyFilter The {@link PropertyFilter} that the created service will manage
     */
    public ServiceBuilder(PropertyFilter propertyFilter){
        this.propertyFilter = propertyFilter;
    }

    /**
     * Adds a {@link FilterRepository} to the service so that it can
     * retrieve and store groups.
     *
     * @param repository The repository implementation
     * @return Self
     */
    public ServiceBuilder withRepository(FilterRepository<? extends Group<? extends Access<? extends Permission>>> repository){
        this.repository = repository;
        return this;
    }

    /**
     * Adds a package path to the list of paths that need to be scanned
     * to find classes to be filtered.
     *
     * @param path The package path to scan, the service will scan all sub packages
     *             from this path
     * @return Self
     */
    public ServiceBuilder addPackageToScan(String path){
        if(packagesToScan == null){ // Lazy initialize to catch when nothing gets added
            packagesToScan = new HashSet<>();
        }
        packagesToScan.add(path);
        return this;
    }

    /**
     * Adds a static mapping between a username and a group name, this mapping
     * is one that for whatever reason is not stored with the group, for example if there
     * is an in-memory only admin user.
     *
     * @param username The username to be stored
     * @param group The group to map the user to
     * @return Self
     */
    public ServiceBuilder addStaticGroupAllocation(String username, String group){
        staticGroupAllocations.put(username, group);
        return this;
    }

    /**
     * Performs the same function as {@link #addStaticGroupAllocation(String, String)},
     * only taking a map of allocations instead of needing to be done one at a time.
     *
     * @param usersToGroup A Map containing group allocations
     * @return Self
     */
    public ServiceBuilder addStaticGroupAllocations(Map<String, String> usersToGroup){
        staticGroupAllocations.putAll(usersToGroup);
        return this;
    }

    /**
     * Adds an in memory only group that for some reason is not persisted,
     * this could be an in memory only group made from some other method than
     * the standard way used by the service.
     *
     * @param group The group to add
     * @return Self
     */
    public ServiceBuilder addRunTimeGroup(Group<? extends Access<? extends Permission>> group){
        this.runTimeGroups.add(group);
        return this;
    }

    /**
     * Performs the same function as {@link #addRunTimeGroup(Group)},
     * only taking a Collection of groups to be added.
     *
     * @param groups A collection of groups that do not need to be saved
     * @return Self
     */
    public ServiceBuilder addRunTimeGroups(Collection<? extends Group<? extends Access<? extends Permission>>> groups){
        this.runTimeGroups.addAll(groups);
        return this;
    }

    /**
     * Builds the {@link FilterService} from the values specified in the builder.
     * @throws IllegalArgumentException If either the PropertyFilter is null
     * or no packages were added for scanning.
     *
     * @return A {@link FilterService} created from the values passed to the builder
     */
    public FilterService build(){
        if(propertyFilter == null){
            throw new IllegalArgumentException("PropertyFilter was null");
        }
        else if(packagesToScan == null){
            throw new IllegalArgumentException("No packages specified to scan");
        }
        return new FilterService(propertyFilter, repository, packagesToScan, staticGroupAllocations, runTimeGroups);
    }
}
