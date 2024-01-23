package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import searchengine.searchComponents.SearchConfig;
import searchengine.searchComponents.SearchDTO;
import searchengine.searchComponents.SearchResponse;
import searchengine.model.Lemma;
import searchengine.model.Site;
import searchengine.model.enumModel.ErrorResponse;
import searchengine.model.enumModel.PositiveResponse;
import searchengine.repository.SiteRepository;
import searchengine.services.SearchService;
import searchengine.services.StartingSearch;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class StartingSearchImpl implements StartingSearch {
    private final SiteRepository siteRepository;
    private  final SearchService searchService;
    private String voidString = "";
    private int zero = 0;
    @Override
    public SearchResponse startSearch(SearchConfig searchConfig){

        List<SearchDTO> searchData;
        String site = searchConfig.getSite();

        if (searchConfig.getQuery().equals(voidString)) {
            return new SearchResponse(false, ErrorResponse.EMPTY_REQUEST.getDescription(),
                    HttpStatus.BAD_REQUEST);
        }

        if(!(site == null)){
            if (siteRepository.findByUrl(site) == null) {
                return new SearchResponse(false, ErrorResponse.PAGE_OUTSIDE_THE_CONFIGURATION_FILE.getDescription(),
                        HttpStatus.NOT_FOUND);
            } else {
                searchData = siteSearch(searchConfig.getQuery(), searchConfig.getSite());
            }
        } else {
            searchData = allSiteSearch(searchConfig.getQuery());
        }

        searchData.sort(Comparator.comparing(SearchDTO::relevance).reversed());
        SearchResponse fullSearchResponse = new SearchResponse(true, voidString, searchData.size(),
                searchData, HttpStatus.OK);
        List<SearchDTO> filteredSearchData = filterSearchData(fullSearchResponse, searchConfig);

        return new SearchResponse(true, voidString, fullSearchResponse.getCount(),
                filteredSearchData, HttpStatus.OK);
    }

    private List<SearchDTO> filterSearchData(SearchResponse fullSearchResponse, SearchConfig searchConfig) {

        SearchResponse searchResponse = fullSearchResponse;
        int startIndex = searchConfig.getOffset();
        int limit = searchConfig.getLimit();
        List<SearchDTO> listData = fullSearchResponse.getData();

        if (startIndex == zero && listData.size() < limit) {
            return searchResponse.getData();
        }

        int endIndex = Math.min(startIndex + limit, listData.size());
        List<SearchDTO> subList = listData.subList(startIndex, endIndex);

        return subList;
    }


    private List<SearchDTO> siteSearch (String text, String url) {

        Site site = siteRepository.findByUrl(url);
        Set<String> textLemmaSet = searchService.getLemmaFromSearchText(text);
        List<Lemma> foundLemmaList = searchService.getLemmaFromSite(textLemmaSet, site);

        if (foundLemmaList.isEmpty()) {
            log.debug(ErrorResponse.RESPONSE_IS_BLANK.getDescription());
            return new ArrayList<>();
        }

        log.info(PositiveResponse.REQUEST_PROCESSED.getDescription());
        List<SearchDTO> searchData = searchService.createSearchDtoList(foundLemmaList, textLemmaSet);

        return searchData;
    }

    private List<SearchDTO> allSiteSearch(String text){

        List<Site> siteList = siteRepository.findAll();
        List<SearchDTO> searchData = new ArrayList<>();

        for (Site site: siteList) {
            searchData.addAll(siteSearch (text, site.getUrl()));
        }

        return searchData;
    }
}
