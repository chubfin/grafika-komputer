public enum LineStyle {
    SOLID("Solid"),
    DASHED("Dashed"),
    DOTTED("Dotted");

    private final String displayName;

    LineStyle(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}