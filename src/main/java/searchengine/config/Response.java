package searchengine.config;

public enum Response {
    SITE_NOT_AVAILABLE("Сайт не доступен!");
    private final String description;
    Response(String description) {
        this.description = description;
    }
    @Override
    public String toString() {
        return description;
    }
}
