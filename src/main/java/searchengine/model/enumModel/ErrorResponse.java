package searchengine.model.enumModel;

public enum ErrorResponse {
    PAGE_NOT_FOUND("Страница не найдена"),
    INDEXING_HAS_ALREADY_STARTED ("Индексация уже запущена"),
    INDEXING_HAS_NOT_STARTED ("Индексация не запущена"),
    INDEXING_STOPPED_BY_THE_USER ("Индексация остановлена пользователем"),
    INDEXING_STOPPED_BY_THE_SYSTEM ("Индексация остановлена системой"),
    URL_ADDRESS ("URL-адрес имеет неправильный формат или содержит недопустимые символы"),
    ELEMENT_NOT_FOUND ("Не удалось найти элемент на веб-странице"),
    PAGE_OUTSIDE_THE_CONFIGURATION_FILE ("Данная страница находится за пределами сайтов, " +
                                "указанных в конфигурационном файле"),
    PAGE_IS_NOT_SPECIFIED("Страница не указана или введена неверно!"),
    EMPTY_REQUEST("Задан пустой поисковый запрос"),
    RESPONSE_IS_BLANK ("Ответ от поисковой системы пустой"),
    FAILED_TO_ESTABLISH_CONNECTION("Не удалось установить соединение"),
    INVALID_SYNTAX ("Слово содержит недопустимые символы или синтаксис"),
    SITE_NOT_AVAILABLE("Сайт недоступен");


    private final String description;

    ErrorResponse(String description) {
        this.description = description;
    }

    public String getDescription() {

        return description;
    }
}
