package myophoria.core.model;

/** Something that can report its training volume (sets and sessions). */
public interface VolumeCalculable {
    /** Returns the volume in the given reporting unit. */
    double volume(Unit reportingUnit);
}
