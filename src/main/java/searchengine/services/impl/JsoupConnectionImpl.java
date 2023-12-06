package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import searchengine.config.Response;
import searchengine.config.SitesList;
import searchengine.services.JsoupСonnection;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class JsoupConnectionImpl implements JsoupСonnection {
    private final SitesList config;
    private Connection.Response response;

    @Override
    public Connection.Response getConnection(String url) {
        try {
            response = Jsoup.connect(url).
                    ignoreContentType(true).
                    userAgent(config.getUserAgent()).
                    referrer(config.getReferer()).
                    timeout(config.getTimeout()).
                    execute();
        } catch (IOException e) {
            System.out.println(Response.SITE_NOT_AVAILABLE);
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
