package searchengine.services;


import searchengine.dto.indexation.IndexingResponse;

public interface IndexService {

    IndexingResponse startIndexing();

    IndexingResponse stopIndexing();

    IndexingResponse indexPage(String url);
}
