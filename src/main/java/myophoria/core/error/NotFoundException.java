package myophoria.core.error;

/** Thrown when an ID does not exist. */
public class NotFoundException extends RuntimeException {
    private final String resourceType;
    private final String id;

    /** Creates the exception for a missing resource. */
    public NotFoundException(String resourceType, String id) {
        super(resourceType + " not found: " + id);
        this.resourceType = resourceType;
        this.id = id;
    }

    /** Returns the kind of resource, for example "exercise". */
    public String getResourceType() {
        return resourceType;
    }

    /** Returns the ID that was not found. */
    public String getId() {
        return id;
    }
}
