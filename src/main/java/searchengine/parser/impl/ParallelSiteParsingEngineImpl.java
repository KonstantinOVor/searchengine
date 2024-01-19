package searchengine.parser.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StopWatch;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.dto.IndexDTO;
import searchengine.dto.LemmaDTO;
import searchengine.dto.PageDTO;
import searchengine.model.*;
import searchengine.model.enumModel.Status;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PagesRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.PagesParser;
import searchengine.parser.ParallelSiteParsingEngine;
import searchengine.services.LemmaSearch;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;



@Slf4j
@RequiredArgsConstructor
public class ParallelSiteParsingEngineImpl implements ParallelSiteParsingEngine, Callable<Boolean> {
    private final String url;
    private final PagesRepository pagesRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final LemmaSearch lemmaSearch;
    private final PagesParser pagesParser;
    private final SitesList config;
    private Lock lock = new ReentrantLock();
    private static ForkJoinPool forkJoinPool;
    private static String slash ="/";
    private String voidString = "";
    private Site site;

    @Override
    public Boolean call() {

        forkJoinPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {

            if (!Thread.interrupted()) {

                site = getSite(url, Status.INDEXING);
                Set<String> urlsSet = ConcurrentHashMap.newKeySet();
                List<PageDTO> pageDtoList = Collections.synchronizedList(new ArrayList<>());
                log.info("Start parse " + url);
                lock.lock();

                try {
                     forkJoinPool
                        .invoke(new DistributedHTMLParserImpl(url, urlsSet, pageDtoList, config));
                log.info("Pages size: " + pageDtoList.size());
                } finally {
                    lock.unlock();
                }

                savePagesInDatabase(site, pageDtoList);
                saveLemmasInDatabase(site);
                saveIndexesToDatabase(site);
                stopWatch.stop();
                System.out.println("Время выполнения метода: " + stopWatch.getTotalTimeMillis() + " мс");

            } else {
                forkJoinPool.shutdown();
                log.info("Site " + site.getName() + " stopped");
            }
        } catch (InterruptedException ex) {
            getStoppedSite();
            log.error("Parsing has been stopped ".concat(url).concat(". "));
        } finally {
            forkJoinPool.shutdown();
        }
        return true;
    }
    protected void getStoppedSite() {

        List <Site> list = siteRepository.findAll();
        List<Site> stoppedSites = list.stream().filter(site -> site.getStatus().equals(Status.INDEXING))
                .map(site -> {
                    site.setLastError("Stopping parsing");
                    site.setStatus(Status.FAILED);
                    site.setStatusTime(LocalDateTime.now());
                    return site;
                })
                .collect(Collectors.toList());
        siteRepository.saveAll(stoppedSites);
        forkJoinPool.shutdown();
    }

    public Site getSite(String url, Status status) {

            lock.lock();
            try {
                Site siteNew = siteRepository.findByUrl(url);
                if (siteNew == null) {
                    siteNew = new Site();
                }
                siteNew.setStatus(status);
                siteNew.setName(getSiteName(url));
                siteNew.setUrl(url);
                siteNew.setStatusTime(LocalDateTime.now());
                siteNew.setLastError(voidString);
                siteRepository.saveAndFlush(siteNew);
                return siteNew;
            } finally {
                lock.unlock();
            }
        }
    private String getSiteName(String url) {

        return config.getSites().stream()
                .filter(site -> site.getUrl().equals(url))
                .findFirst()
                .map(SiteConfig::getName)
                .orElse("");
    }


    private void savePagesInDatabase(Site site, List<PageDTO> pageDtoList) throws InterruptedException {

        int startPath;
        String pagePath;
        List<Page> pageList = new CopyOnWriteArrayList<>();

        for (PageDTO pageDTO : pageDtoList) {

            Page pageNew = new Page();
            startPath = pageDTO.url().indexOf(url) + url.length();
            pagePath = slash.concat(pageDTO.url().substring(startPath));
            pageNew.setSite(site);
            pageNew.setPath(pagePath);
            pageNew.setCode(pageDTO.code());
            pageNew.setContent(pageDTO.content());
            pageList.add(pageNew);

            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
        }
        pagesRepository.saveAllAndFlush(pageList);
    }

    private List<Lemma> saveLemmasInDatabase(Site site) throws InterruptedException {

        lock.lock();
        List<Lemma> lemmaList;

        try {
            if (!Thread.interrupted()) {

                site.setStatusTime(LocalDateTime.now());
                List<LemmaDTO> lemmaDtoList = lemmaSearch.startLemmaSearch(site);
                lemmaList = lemmaSheetAssembly(site, lemmaDtoList);

                if (!lemmaList.isEmpty()) {
                    lemmaRepository.saveAllAndFlush(lemmaList);
                    log.info("Save lemmas in the database");
                }else {
                    log.debug("Lemma list is empty. No lemmas to save.");
                }

            } else throw new InterruptedException();
            return lemmaList;
        } finally {
            lock.unlock();
        }
    }
    @Override
    public List<Lemma> lemmaSheetAssembly(Site site, List<LemmaDTO> lemmaDtoList) throws InterruptedException {

        List<Lemma> lemmaList = new CopyOnWriteArrayList<>();

        for (LemmaDTO lemmaDto : lemmaDtoList) {
            Lemma lemma = new Lemma();
            lemma.setSite(site);
            lemma.setLemma(lemmaDto.lemma());
            lemma.setFrequency(lemmaDto.frequency());
            lemmaList.add(lemma);
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
        }
        return lemmaList;
    }

    public void saveIndexesToDatabase(Site site) throws InterruptedException {

        if (Thread.interrupted()) {
            return;
        }
            List<IndexDTO> indexDtoList = pagesParser.pagesParsing(site);

        lock.lock();
        try {
            List<SearchIndex> searchIndexList = indexTableBuilding (indexDtoList);
            indexRepository.saveAllAndFlush(searchIndexList);
            log.info("Indexing stopped. Saving indexes in the database");
            site.setStatusTime(LocalDateTime.now());
            site.setStatus(Status.INDEXED);
            siteRepository.saveAndFlush(site);
        } finally {
            lock.unlock();
        }
    }

    private List<SearchIndex> indexTableBuilding(List<IndexDTO> indexDtoList) throws InterruptedException {

        List<SearchIndex> searchIndexList = new CopyOnWriteArrayList<>();

        for (IndexDTO indexDto : indexDtoList) {
            SearchIndex searchIndex = indexRepository.findByPageAndLemma(indexDto.pageId(),
                    indexDto.lemmaId());

            if (searchIndex == null) {
                searchIndex = new SearchIndex();
                searchIndex.setPage(indexDto.pageId());
                searchIndex.setLemma(indexDto.lemmaId());
                searchIndex.setRank(indexDto.rank());
                searchIndexList.add(searchIndex);
            }
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
        }
        return searchIndexList;
    }
}

