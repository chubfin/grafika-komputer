public enum AnimationType {
    NONE("None"),
    SPIN("Spin (Rotate)"),
    BOUNCE("Bounce (Move)"),
    PULSE("Pulse (Scale)");

    private final String displayName;

    AnimationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
