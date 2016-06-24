package uk.co.agware.filter.persistence;

import uk.co.agware.filter.data.Group;

import java.util.List;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 24/06/2016.
 */
public interface FilterRepository {

    <T extends Group> T getGroup(String id);

    <T extends Group> List<T> getGroups();

    <T extends Group> List<T> initGroups();

    String save(Group group);

    void delete(String id);
}
