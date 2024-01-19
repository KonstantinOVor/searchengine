package searchengine.services;

import searchengine.dto.LemmaDTO;
import searchengine.model.Site;
import java.util.List;

public interface LemmaSearch {
    List<LemmaDTO> startLemmaSearch(Site site);
    String deletingTags(String content, String selector);
}
