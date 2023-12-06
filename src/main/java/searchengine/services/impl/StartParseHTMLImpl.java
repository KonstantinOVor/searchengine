package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.dto.PageDTO;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repository.PagesRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.StartParseHTML;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;
@RequiredArgsConstructor
public class StartParseHTMLImpl implements StartParseHTML, Callable<Boolean> {
    private final String url;
    private final PagesRepository pagesRepository;
    private final SiteRepository siteRepository;
    private final SitesList config;
    private static ForkJoinPool forkJoinPool;
    private Site site;

    @Override
    public Boolean call() {
        forkJoinPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());

//        if (siteRepository.existsByUrl(url)){
//            site = siteRepository.findByUrl(url);
//            siteRepository.delete(site);
//        }

        site = getSite(Status.INDEXING);
        siteRepository.saveAndFlush(site);

        try {
                Set<String> urlsSet = ConcurrentHashMap.newKeySet();
                List<PageDTO> pageDtoList = new CopyOnWriteArrayList<>();
                List<PageDTO> pages = forkJoinPool
                        .invoke(new ParseHTMLImpl(url, urlsSet, pageDtoList));
                List<PageDTO> newPageDtoList = new CopyOnWriteArrayList<>(pages);
                List<Page> pageList = addPageList(newPageDtoList);
                pagesRepository.saveAllAndFlush(pageList);
        } catch (Exception exception) {
                exception.printStackTrace();
        } finally {
            forkJoinPool.isShutdown();
        }
        return true;
    }

    private Site getSite(Status status) {
        Site siteNew = new Site();
        siteNew.setStatus(status);
        siteNew.setName(getSiteName());
        siteNew.setUrl(url);
        siteNew.setStatusTime(LocalDateTime.now());
        siteNew.setLastError("");
        return siteNew;
    }

//    private Site getSite(Status status) {
//        Site siteNew = Site.builder()
//                .status(status)
//                .name(getSiteName())
//                .url(url)
//                .statusTime(LocalDateTime.now())
//                .lastError("")
//                .build();
//        return siteNew;
//    }

    private List<Page> addPageList (List<PageDTO> pageDtoList) {
        int start;
        int code;
        String pagePath;
        String content;
        List<Page> pageList = new CopyOnWriteArrayList<>();
        for (PageDTO page : pageDtoList) {
            start = page.getUrl().indexOf(url) + url.length();
            pagePath = page.getUrl().substring(start);
            code = page.getCode();
            content = page.getContent();
            pageList.add(new Page(site, pagePath, code, content));
        }
        return pageList;
    }


    private String getSiteName() {
        return config.getSites().stream()
                .filter(site -> site.getUrl().equals(url))
                .findFirst()
                .map(SiteConfig::getName)
                .orElse("");
    }
}
