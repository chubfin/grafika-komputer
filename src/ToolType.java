public enum ToolType {
    SELECT("Select"),
    RECTANGLE("Rectangle"),
    CIRCLE("Circle"),
    TRIANGLE("Triangle"),
    LINE("Line");

    private final String displayName;

    ToolType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
