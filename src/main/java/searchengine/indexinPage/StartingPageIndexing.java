package searchengine.indexinPage;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StopWatch;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.dto.LemmaDTO;
import searchengine.dto.PageDTO;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.enumModel.Status;
import searchengine.parser.impl.DistributedHTMLParserImpl;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PagesRepository;
import searchengine.services.Lemmatizer;
import searchengine.parser.ParallelSiteParsingEngine;
import searchengine.services.LemmaSearch;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Data
@RequiredArgsConstructor
@Slf4j
public class StartingPageIndexing implements Callable<Boolean> {

    private final SiteConfig site;
    private final String url;
    private final SitesList config;
    private final PagesRepository pagesRepository;
    private final LemmaRepository lemmaRepository;
    private final ParallelSiteParsingEngine parallelSiteParsingEngine;
    private final LemmaSearch lemmaSearch;
    private final Lemmatizer lemmatizer;
    private Lock lock = new ReentrantLock();
    private String slash = "/";
    private  String selector = "*";
    private int one = 1;
    private int twoHundred = 200;

    @Override
    public Boolean call() {

        try {

            log.info("Start parse " + url);
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            Site newSite = parallelSiteParsingEngine.getSite(site.getUrl(), Status.INDEXING);
            List<PageDTO> pages = getPageDTOs(url);
            Page pageNew = getPageNew(newSite, url, pages);

            if (pageNew == null) {
                return false;
            }

            List<LemmaDTO> lemmaDtoList = dtoLemmaListCreation(pageNew);
            indexPageLemmas(newSite,lemmaDtoList);
            parallelSiteParsingEngine.saveIndexesToDatabase(newSite);
            stopWatch.stop();
            log.info("Время выполнения метода: " + stopWatch.getTotalTimeMillis() + " мс");
            return true;

        } catch (InterruptedException ex) {
            log.error("Parsing has been stopped ".concat(url).concat(". "));
            return false;
        }
    }

    private List<PageDTO> getPageDTOs(String url) {

        ForkJoinPool forkJoinPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
        Set<String> urlsSet = ConcurrentHashMap.newKeySet();
        List<PageDTO> pageDtoList = Collections.synchronizedList(new ArrayList<>());
        lock.lock();
        try {
            forkJoinPool
                .invoke(new DistributedHTMLParserImpl(url, urlsSet, pageDtoList, config));
        } finally {
            lock.unlock();
        }
        return pageDtoList;
    }

    private Page getPageNew(Site site, String url, List<PageDTO> pages) {

        return pages.stream()
                .filter(pageDTO -> pageDTO.code() == twoHundred && pageDTO.url().equals(url))
                .findFirst()
                .map(pageDTO -> {
                    Page page = new Page();
                    int startPath = pageDTO.url().lastIndexOf(site.getUrl()) + site.getUrl().length();
                    String pagePath = slash.concat(pageDTO.url().substring(startPath));
                    page.setSite(site);
                    page.setPath(pagePath);
                    page.setCode(pageDTO.code());
                    page.setContent(pageDTO.content());
                    pagesRepository.saveAndFlush(page);
                    return page;
                })
                .orElse(null);
    }

    private List<LemmaDTO> dtoLemmaListCreation(Page pageNew) {

        String content = pageNew.getContent();
        String text = lemmaSearch.deletingTags(content, selector);
        Map<String, Integer> mapLemmas = lemmatizer.getMapLemma(text);

        return  mapLemmas.entrySet().stream()
                .map(entry -> new LemmaDTO(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }



    private void indexPageLemmas(Site site, List<LemmaDTO> lemmaDtoList) throws InterruptedException {

        List<Lemma> lemmaList = parallelSiteParsingEngine.lemmaSheetAssembly(site, lemmaDtoList);

        if (lemmaList.isEmpty()) {
            return;
        }

        List<Lemma> storedLemmas = lemmaRepository.findAllBySite(site);
        lock.lock();
        try {
            for (Lemma lemma : lemmaList) {
                storedLemmas.stream()
                        .filter(l -> l.getLemma().equals(lemma.getLemma()))
                        .findFirst()
                        .ifPresentOrElse(l -> l.setFrequency(l.getFrequency() + one),
                                () -> {
                                    lemma.setFrequency(one);
                                    storedLemmas.add(lemma);
                                });
            }

            lemmaRepository.saveAllAndFlush(storedLemmas);
        } finally {
            lock.unlock();
        }
    }
}
