package searchengine.connection;

import org.jsoup.Connection;

public interface JsoupСonnection {

    Connection.Response getConnection();
    Boolean isAvailableContent(Connection.Response response);

    Integer getStatusCode(Connection.Response response);



}

