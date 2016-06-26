package uk.co.agware.filter.service;

import uk.co.agware.filter.PropertyFilter;
import uk.co.agware.filter.data.Group;
import uk.co.agware.filter.impl.PseudoRepository;
import uk.co.agware.filter.persistence.FilterRepository;

import java.util.*;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 24/06/2016.
 */
public class ServiceBuilder {

    private PropertyFilter propertyFilter;
    private FilterRepository repository = new PseudoRepository();
    private Set<String> packagesToScan;
    private Map<String, String> staticGroupAllocations = new HashMap<>();
    private List<Group> runTimeGroups = new ArrayList<>();

    public ServiceBuilder(PropertyFilter propertyFilter){
        this.propertyFilter = propertyFilter;
    }

    public ServiceBuilder withRepository(FilterRepository repository){
        this.repository = repository;
        return this;
    }

    public ServiceBuilder addPackageToScan(String path){
        if(packagesToScan == null){ // Lazy initialize to catch when nothing gets added
            packagesToScan = new HashSet<>();
        }
        packagesToScan.add(path);
        return this;
    }

    public ServiceBuilder addStaticGroupAllocation(String username, String group){
        staticGroupAllocations.put(username, group);
        return this;
    }

    public ServiceBuilder addStaticGroupAllocations(Map<String, String> usersToGroup){
        staticGroupAllocations.putAll(usersToGroup);
        return this;
    }

    public ServiceBuilder addRunTimeGroup(Group group){
        this.runTimeGroups.add(group);
        return this;
    }

    public ServiceBuilder addRunTimeGroups(List<Group> groups){
        this.runTimeGroups.addAll(groups);
        return this;
    }

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