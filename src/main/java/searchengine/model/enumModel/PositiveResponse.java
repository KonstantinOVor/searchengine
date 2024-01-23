package searchengine.model.enumModel;

public enum PositiveResponse {
    GOOD(""),
    SITE_DELETED ("Сайт успешно удален."),
    REQUEST_PROCESSED("Поисковый запрос обработан. Ответ получен.");

    private final String description;

    PositiveResponse (String description) {
        this.description = description;
    }

    public String getDescription() {

        return description;
    }
}
