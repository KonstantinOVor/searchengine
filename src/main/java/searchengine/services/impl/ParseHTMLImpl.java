package searchengine.services.impl;

import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.SitesList;
import searchengine.model.ErrorResponce;
import searchengine.dto.PageDTO;
import searchengine.services.*;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

public class ParseHTMLImpl  extends RecursiveTask<List<PageDTO>> implements ParserHTML {
    private final String url;
    private final List<PageDTO> pageDtoList;
    private Set<String> urlsSet;
    private SitesList config;
    public static volatile boolean stop = false;

    public ParseHTMLImpl(String url, Set<String> urlsSet, List<PageDTO> pageDtoList) {

        this.url = url;
        this.urlsSet = urlsSet;
        this.pageDtoList = pageDtoList;

    }

    @Override
    protected List<PageDTO> compute() {
        Connection.Response response = null;
        Document document = null;
        JsoupСonnection jsoupСonnection = new JsoupConnectionImpl(config);
        try {
//            if (stop) {
//                stopExecute();
//                return;
//            }

            Thread.sleep(300);
            response = jsoupСonnection.getConnection(url);

            if (jsoupСonnection.isAvailableContent(response)) {
                document = response.parse();
                dtoAssembly(document, response);
            }

            List<ParseHTMLImpl> listTask = taskListCreation(document);
            listTask.forEach(ForkJoinTask::join);
        } catch (NullPointerException e) {
            dtoAssembly (document, response);
            e.printStackTrace();
        } finally {
            return pageDtoList;
        }
    }

    private void dtoAssembly (Document document, Connection.Response response){

        String html;
        String error;
        int status = response.statusCode();
        if (status == 200) {
            html = document.outerHtml();
            error = ErrorResponce.GOOD.toString();
        } else {
            html = "";
            error = ErrorResponce.PAGE_NOT_FOUND.toString();
        }
        PageDTO pageDTO = new PageDTO(url, html, status);

        pageDtoList.add(pageDTO);
    }

    private List<ParseHTMLImpl> taskListCreation (Document document){
        Elements elements = document.select("body").select("a");
        List<ParseHTMLImpl> listTask = new CopyOnWriteArrayList<>();
        urlsSet = ConcurrentHashMap.newKeySet();
        String link;
        for (Element element : elements) {
            link = element.absUrl("href");
            if (!isSiteElementsType(link)
                    && link.startsWith(element.baseUri())
                    && !link.equals(element.baseUri())
                    && !link.contains("#")
                    && !urlsSet.contains(link)) {
                urlsSet.add(link);
                ParseHTMLImpl task = new ParseHTMLImpl(link, urlsSet, pageDtoList);
                task.fork();
                listTask.add(task);
            }
        }
        return listTask;
    }
    @Override
    public boolean isSiteElementsType(String path) {
        List<String> listMatchingType = Arrays.asList("jar","exe","JPG", "gif", "gz", "jar", "jpeg",
                 "jpg", "pdf", "png", "ppt", "pptx", "svg", "svg", "tar", "zip");
        return listMatchingType.contains(path.substring(path.lastIndexOf(".") + 1));
    }
}
