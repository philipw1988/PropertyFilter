package uk.co.agware.filter.impl;

import uk.co.agware.filter.data.Group;
import uk.co.agware.filter.persistence.FilterRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 24/06/2016.
 *
 * Does nothing, used when no other persistence entity is defined for the Filter Service to use
 */
public class PseudoRepository implements FilterRepository<GroupImpl> {

    @Override
    public GroupImpl getGroup(String id) {
        return null;
    }

    @Override
    public List<GroupImpl> getGroups() {
        return new ArrayList<>();
    }

    @Override
    public List<GroupImpl> initGroups() {
        return new ArrayList<>();
    }

    @Override
    public String save(Group group) {
        return null;
    }

    @Override
    public void delete(String id) {

    }
}
