package searchengine.services;

import org.jsoup.Connection;

public interface JsoupСonnection {

    Connection.Response getConnection(String url);
    Boolean isAvailableContent(Connection.Response response);

    Integer getStatusCode(Connection.Response response);



}

