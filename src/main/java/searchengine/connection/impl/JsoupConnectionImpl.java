package searchengine.connection.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.http.HttpStatus;
import searchengine.config.SitesList;
import searchengine.connection.JsoupСonnection;
import searchengine.model.enumModel.ErrorResponse;

import java.io.IOException;
@Slf4j
@RequiredArgsConstructor

public class JsoupConnectionImpl implements JsoupСonnection {
    private final SitesList config;
    private final String url;
    private Connection.Response response;

    @Override
    public Connection.Response getConnection() {
        try {
            response = Jsoup.connect(url).
                    ignoreContentType(true).
                    userAgent(config.getUserAgent()).
                    referrer(config.getReferrer()).
                    timeout(config.getTimeout()).
                    execute();

        } catch (IOException e) {
            log.debug(ErrorResponse.FAILED_TO_ESTABLISH_CONNECTION.getDescription());
            throw new RuntimeException(ErrorResponse.FAILED_TO_ESTABLISH_CONNECTION.getDescription(), e);
        }
        return response;
    }

    public Integer getStatusCode(Connection.Response response){

        return response.statusCode();
    }

    @Override
    public Boolean isAvailableContent(Connection.Response response) {
        return ((response != null)
                && (response.contentType().equalsIgnoreCase(config.getContentType()) &&
                (getStatusCode(response) == HttpStatus.OK.value())));
    }
}
