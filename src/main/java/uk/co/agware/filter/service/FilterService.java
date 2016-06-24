package uk.co.agware.filter.service;

import uk.co.agware.filter.PropertyFilter;
import uk.co.agware.filter.persistence.FilterRepository;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 24/06/2016.
 */
public class FilterService {

    private PropertyFilter propertyFilter;
    private FilterRepository repository;


    public FilterService(PropertyFilter propertyFilter, FilterRepository repository) {
        this.propertyFilter = propertyFilter;
        this.repository = repository;
    }


}
