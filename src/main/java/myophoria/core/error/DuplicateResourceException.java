package myophoria.core.error;

/** Thrown when an active name is already in use; carries what the UI needs to offer restore or rename. */
public class DuplicateResourceException extends RuntimeException {
    private final String resourceType;
    private final String existingId;
    private final boolean softDeleted;

    /** Creates the exception for a name clash with an existing (possibly soft-deleted) resource. */
    public DuplicateResourceException(String resourceType, String existingId, boolean softDeleted) {
        super(resourceType + " name already in use by " + existingId);
        this.resourceType = resourceType;
        this.existingId = existingId;
        this.softDeleted = softDeleted;
    }

    /** Returns the kind of resource, for example "exercise". */
    public String getResourceType() {
        return resourceType;
    }

    /** Returns the ID of the resource that already has the name. */
    public String getExistingId() {
        return existingId;
    }

    /** Returns true if the clashing resource is soft-deleted. */
    public boolean isSoftDeleted() {
        return softDeleted;
    }
}
