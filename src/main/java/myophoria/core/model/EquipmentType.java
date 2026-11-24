package myophoria.core.model;

/** How an exercise is loaded; decides which Exercise subclass is used. */
public enum EquipmentType {
    /** Bar loaded with plates; weight is the total. */
    BARBELL,
    /** Free weights; weight is per dumbbell. */
    DUMBBELL,
    /** Cable or machine pin stack; weight is the stack setting. */
    STACK,
    /** Plate-loaded machine; weight is the plates added. */
    PLATE_LOADED,
    /** Bodyweight movement; weight is the added load (0 allowed). */
    CALISTHENICS
}
