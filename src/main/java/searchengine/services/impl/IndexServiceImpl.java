package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.model.ErrorResponce;
import searchengine.dto.responce.DtoStartIndexing;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repository.PagesRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.IndexService;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexServiceImpl implements IndexService {

    private final SiteRepository siteRepository;
    private final PagesRepository pageRepository;
    private final JsoupConnectionImpl jsoupConnection;
    private final SitesList config;
    private ExecutorService executorService;
    private boolean flag = false;


    @Override
    public DtoStartIndexing startIndexing() {
        flag = isIndexing();
        if (flag) {
            return new DtoStartIndexing(false, ErrorResponce.INDEXING_HAS_ALREADY_STARTED.getDescription());

        } else {

            List<SiteConfig> siteList = config.getSites();
            siteRepository.deleteAll();
            executorService = Executors.
                    newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            for (SiteConfig site : siteList) {
                String url = site.getUrl();
                executorService.submit(new StartParseHTMLImpl(url, pageRepository, siteRepository, config));
            }
            executorService.shutdown();
        }
        return new DtoStartIndexing(true, "");
    }

    private boolean isIndexing() {
        siteRepository.flush();
        Iterable<Site> siteList = siteRepository.findAll();
        for (Site site : siteList) {
            if (site.getStatus().equals(Status.INDEXING)) {
                flag = true;
                return flag;
            }
        }
        return flag;
    }
}
