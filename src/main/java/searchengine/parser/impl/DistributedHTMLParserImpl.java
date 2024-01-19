package searchengine.parser.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.SitesList;
import searchengine.connection.JsoupСonnection;
import searchengine.dto.PageDTO;
import searchengine.parser.DistributedHTMLParser;
import searchengine.connection.impl.JsoupConnectionImpl;
import java.util.*;
import java.util.concurrent.*;
@Slf4j
@RequiredArgsConstructor
public class DistributedHTMLParserImpl extends RecursiveTask<List<PageDTO>> implements DistributedHTMLParser {
    private final String url;
    private final Set<String> urlsSet;
    private final List<PageDTO> pageDtoList;
    private final SitesList config;



    @Override
    protected List<PageDTO> compute() {

        Connection.Response response;
        Document document;
        JsoupСonnection jsoupСonnection = new JsoupConnectionImpl(config, url);

        try {
            response = jsoupСonnection.getConnection();
            List<DistributedHTMLParserImpl> listTask;
            if (jsoupСonnection.isAvailableContent(response)) {
                document = response.parse();
                listTask = taskListCreation(document, response);
                log.info("Task count:{}", listTask.size());
                listTask.forEach(ForkJoinTask::join);
            }
        } catch (InterruptedException interruptedException) {
            log.debug("Thread interrupted while parsing from ".concat(url));
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.debug("Error parsing from ".concat(url));
            e.printStackTrace();
        }
            return pageDtoList;
    }

    private void dtoAssembly (Document document, Connection.Response response){

        String html;
        int status = response.statusCode();
        html = document.outerHtml();
        PageDTO pageDTO = new PageDTO(url, html, status);
        pageDtoList.add(pageDTO);
    }

    private List<DistributedHTMLParserImpl> taskListCreation (Document document, Connection.Response response)
            throws InterruptedException {

        dtoAssembly(document, response);
        Elements elements = document.select("body").select("a");
        log.info("Number of elements:{}", elements.size());
        List<DistributedHTMLParserImpl> listTask = new CopyOnWriteArrayList<>();
        String link;
        DistributedHTMLParserImpl task;

        for (Element element : elements) {
            link = element.absUrl("href");

            if (!isSiteElementsType(link)
                    && link.startsWith(element.baseUri())
                    && !link.equals(element.baseUri())
                    && !link.contains("#")
                    && !urlsSet.contains(link)) {
                urlsSet.add(link);
                task = new DistributedHTMLParserImpl(link, urlsSet, pageDtoList ,config);
                listTask.add(task);
                task.fork();
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
