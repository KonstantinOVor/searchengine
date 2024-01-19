package searchengine.services;

import searchengine.searchComponents.SearchConfig;
import searchengine.searchComponents.SearchResponse;

public interface StartingSearch {
    SearchResponse startSearch(SearchConfig searchConfig);
}
