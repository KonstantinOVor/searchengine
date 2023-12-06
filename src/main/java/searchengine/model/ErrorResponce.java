package searchengine.model;

public enum ErrorResponce {
    GOOD(""),
    PAGE_NOT_FOUND("Страница не найдена"),
    INDEXING_HAS_ALREADY_STARTED ("Индексация уже запущена");


    private final String description;

    ErrorResponce(String description) {
        this.description = description;
    }

    public String getDescription() {

        return description;
    }
}
