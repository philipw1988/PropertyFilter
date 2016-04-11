# PropertyFilter

This was designed as something to use as a way of limiting users access to fields within an object in a hopefully easily manageable manner when they are being passed to and from a front end of an application / the persistence layer.

The basic idea is that you can build up a set of groups which have different access levels to each property of the objects you want to manage and then passing the objects through the filters will either null out fields on the return objects which should not be seen, or not copy across any values which should not be changed. This will hopefully be a compact way to manage all the logic required for that.

The FilterUtil class has methods for creating the list of Access objects required for each group.

Calling `FilterUtil.getFullAccessList(path)` will return a list of Access Objects, one for each class in the given classpath.

The default access type for the run can be configured by calling the `FilterUtil.setDefaultAccessType(accessType)` and `FilterUtil.setDefaultPermissionType(permissionType)` methods which will define the values set for all classes and fields which are not annotated with specific values such as `@ReadOnly`

Groups are loaded into the PropertyFiler by calling the `propertyFilter.load(groups)` method, this method can be called at any time to live update the stored values, the calls use a ReadWriteLock so there shouldn't be any reads during the reload process.

The `propertyFilter.parseObjectForReturn(object, username)` method creates a blank object internally to copy the correct values into, therefore any object used with this module needs to have a default constructor available. The basic idea of the class would be that you get the object out of your chosen persistence layer and then pass it through the filter along with the username of the current user.

The `propertyFilter.parseObjectForSaving(newObject, oldObject, username)` works slightly differently, with this you would have an updated object `newObject` that is being returned from the client side with new data, and then the object as it currently exists in storage `oldObject` and you want to update the storage with the correct values. In this case you only want to copy over certain values that the client has sent you, this method does that and then `oldObject` will be the updated value that you save again.

The PropertyFilter also contains some convenience methods for getting lists of things such as all classes the user has access to `getAccessibleClasses()`, and all fields within that class tha the user has access to `getAccessibleFields()`

There is likely more to be written here, but I'll sort that out once I've actually implemented this into a test app and found out where things are wrong/lacking.