package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.Site;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PagesRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.StatisticsService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
        private final PagesRepository pagesRepository;
        private final LemmaRepository lemmaRepository;
        private final SiteRepository siteRepository;

    @Override
    public StatisticsResponse getStatisticsResponse() {

        TotalStatistics total = getTotalStatistics();
        List<DetailedStatisticsItem> list = getDetailedStatisticsItemList();
        return new StatisticsResponse(true, new StatisticsData(total, list));
    }

    private TotalStatistics getTotalStatistics() {

        long sites = siteRepository.count();
        long pages = pagesRepository.count();
        long lemmas = lemmaRepository.count();
        return new TotalStatistics(sites, pages, lemmas, true);
    }

    private List<DetailedStatisticsItem> getDetailedStatisticsItemList() {

        List<Site> siteList = siteRepository.findAll();
        return siteList.stream().map(this::getDetailedStatisticItem).collect(Collectors.toList());
    }

    private DetailedStatisticsItem getDetailedStatisticItem(Site site) {

        String url = site.getUrl();
        String name = site.getName();
        String status = site.getStatus().toString();
        LocalDateTime statusTime = site.getStatusTime();
        String error = site.getLastError();
        long pages = pagesRepository.countBySite(site);
        long lemmas = lemmaRepository.countBySite(site);
        return new DetailedStatisticsItem(url, name, status, statusTime, error, pages, lemmas);
    }
}
