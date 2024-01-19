package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.indexinPage.StartingPageIndexing;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.enumModel.ErrorResponse;
import searchengine.dto.indexation.IndexingResponse;
import searchengine.model.Site;
import searchengine.model.enumModel.PositiveResponse;
import searchengine.model.enumModel.Status;
import searchengine.parser.impl.ParallelSiteParsingEngineImpl;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PagesRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.IndexService;
import searchengine.services.Lemmatizer;
import searchengine.services.PagesParser;
import searchengine.services.LemmaSearch;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexServiceImpl implements IndexService {

    private final SiteRepository siteRepository;
    private final PagesRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final LemmaSearch lemmaSearch;
    private final PagesParser pagesParser;
    private final SitesList config;
    private final Lemmatizer lemmatizer;
    private ExecutorService executorService;
    private boolean flag = false;
    private final String WEBSITE_TEMPLATE = "^https?://(www\\.)?[\\w-]+\\.[\\w.-]+(/[\\w-./?%&=]*)?$";
    private int zero = 0;
    private int one = 1;
    private String voidString = "";
    private String lastSlash = "/$";


    @Override
    public IndexingResponse startIndexing() {

        flag = isIndexing();

        if (flag) {
            log.info("Index already started.");
            flag = false;
            return new IndexingResponse(false, ErrorResponse.INDEXING_HAS_ALREADY_STARTED.getDescription());
        }

        List<SiteConfig> siteList = config.getSites();
        siteRepository.deleteAll();
        executorService = Executors.
                newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        for (SiteConfig site : siteList) {
            String url = site.getUrl();
            executorService.submit(new ParallelSiteParsingEngineImpl(url, pageRepository, siteRepository, lemmaRepository,
                    indexRepository, lemmaSearch, pagesParser, config));
        }
        executorService.shutdown();
        flag = false;
        return new IndexingResponse(true, PositiveResponse.GOOD.getDescription());
    }

    @Override
    public IndexingResponse stopIndexing() {

        flag = isIndexing();

        if (!flag) {
            return new IndexingResponse(false, ErrorResponse.INDEXING_HAS_NOT_STARTED.getDescription());
        } else {
            log.info("Index stopping.");
            executorService.shutdownNow();
            return new IndexingResponse(false, ErrorResponse. INDEXING_STOPPED_BY_THE_USER.getDescription());
        }
    }

    private boolean isIndexing() {
        siteRepository.flush();
        Iterable<Site> siteList = siteRepository.findAll();
        for (Site site : siteList) {
            flag = site.getStatus().equals(Status.INDEXING) ? true : site.getStatus().equals(Status.FAILED)
                                                                        ? false : flag;
            if (!flag) {
                break;
            }
        }
        return flag;
    }
    @Override
    public IndexingResponse indexPage(String urlPage) {

        IndexingResponse indexingResponse = null;

        try {
            if (urlPage.isEmpty() || !urlPage.matches(WEBSITE_TEMPLATE)) {
                log.info("Page is not specified");
                indexingResponse = new IndexingResponse(false, ErrorResponse.PAGE_IS_NOT_SPECIFIED.getDescription(),
                        HttpStatus.BAD_REQUEST);
            }

            executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

            if (findSite(urlPage).isPresent()) {

                String url = findSite(urlPage).get();

                deleteSiteByUrl(url);

                log.info("Indexing web site " + urlPage);
                indexingResponse = indexWebSite(url);

            } else if (isPageInTable(urlPage).isPresent()) {

                indexingResponse = indexPageOnConfig(urlPage);
            }

        } catch (InterruptedException interruptedException) {
            log.info("Indexing has been stopped ".concat(urlPage).concat(". "));
            return new IndexingResponse(false, ErrorResponse.INDEXING_STOPPED_BY_THE_SYSTEM.getDescription());
        } catch (NoSuchElementException noSuchElementException) {
            log.info("Element not found ".concat(urlPage).concat(". "));
            return new IndexingResponse(false, ErrorResponse.ELEMENT_NOT_FOUND.getDescription());
        } catch (MalformedURLException malformedURLException) {
            log.info("Incorrect URL ".concat(urlPage).concat(". "));
            return new IndexingResponse(false, ErrorResponse.URL_ADDRESS.getDescription());
        }

        return indexingResponse;
    }

    private void deleteSiteByUrl(String url) {
        if (siteRepository.existsByUrl(url)) {
            Site site = siteRepository.findByUrl(url);
            siteRepository.deleteById(site.getId());
        }
    }


    private IndexingResponse indexWebSite(String url) throws InterruptedException {
        executorService.submit(new ParallelSiteParsingEngineImpl(url, pageRepository, siteRepository, lemmaRepository,
                indexRepository, lemmaSearch, pagesParser, config));
        executorService.shutdown();
        return new IndexingResponse(true, PositiveResponse.REQUEST_PROCESSED.getDescription());
    }

    private IndexingResponse indexPageOnConfig(String urlPage) throws MalformedURLException, NoSuchElementException,
            InterruptedException {

        Optional<String> result = isPageInTable(urlPage);

        if (result.isPresent()) {
            String url = result.get();
            Page page = pageRepository.findByPath(url);
            deletePageAndLemmas(page);
        }

        config.getSites().stream()
                .filter(site -> urlPage.contains(site.getUrl()))
                .forEach(site -> {
                    ParallelSiteParsingEngineImpl startParseHTML = new ParallelSiteParsingEngineImpl(urlPage, pageRepository,
                            siteRepository, lemmaRepository, indexRepository, lemmaSearch, pagesParser, config);
                    executorService.submit(new StartingPageIndexing(site, urlPage, config,
                            pageRepository, lemmaRepository, startParseHTML, lemmaSearch, lemmatizer));
                });

        executorService.shutdown();

        return new IndexingResponse(true, PositiveResponse.REQUEST_PROCESSED.getDescription());
    }

    private void deletePageAndLemmas(Page page) {

        List<Lemma> pageLemmaList = lemmaRepository.findByPage(page);
        pageRepository.delete(page);
        deleteFrequency(pageLemmaList);
    }

    public void deleteFrequency(List<Lemma> pageLemmaList) {

        for (Lemma lemma : pageLemmaList) {
            int frequencyCount = one;
            lemma.setFrequency(lemma.getFrequency() - frequencyCount);

            if (lemma.getFrequency() == zero) {
                lemmaRepository.delete(lemma);
            }
        }
        lemmaRepository.saveAll(pageLemmaList);
    }


    private Optional<String> findSite(String url) {

        return config.getSites().stream()
                .filter(s -> s.getUrl().replaceAll(lastSlash, voidString)
                        .equals(url.replaceAll(lastSlash, voidString)))
                .findFirst()
                .map(SiteConfig::getUrl);
    }

    private Optional<String> isPageInTable(String url) throws MalformedURLException {

        String relativePath = new URL(url).getPath();

        return pageRepository.findAll().stream()
                .filter(page -> page.getPath().equals(relativePath))
                .findFirst()
                .map(Page::getPath);
    }
}
