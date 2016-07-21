package uk.co.agware.filter.persistence;

import uk.co.agware.filter.data.Group;

import java.util.List;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 24/06/2016.
 */
public interface FilterRepository<G extends Group> {

    G getGroup(String id);

    List<G> getGroups();

    List<G> initGroups();

    Object save(Group group);

    void delete(String id);
}
