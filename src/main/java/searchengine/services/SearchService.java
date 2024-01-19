package searchengine.services;

import searchengine.searchComponents.SearchDTO;
import searchengine.model.Lemma;
import searchengine.model.Site;

import java.util.List;
import java.util.Set;

public interface SearchService {
    List<Lemma> getLemmaFromSite(Set<String> lemmas, Site site);
    Set<String> getLemmaFromSearchText(String text);
    List<SearchDTO> createSearchDtoList(List<Lemma> lemmaList, Set<String> textLemmaList);
}
