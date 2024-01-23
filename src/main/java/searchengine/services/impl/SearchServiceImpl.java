package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.searchComponents.SearchDTO;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SearchIndex;
import searchengine.model.Site;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PagesRepository;
import searchengine.searchComponents.CreateSearchDto;
import searchengine.services.Lemmatizer;
import searchengine.services.SearchService;
import searchengine.services.LemmaSearch;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;


@Service
@Slf4j
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final Lemmatizer lemmatizer;
    private final PagesRepository pagesRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final LemmaSearch lemmaSearch;
    private static int newFixedThreadPool = Runtime.getRuntime().availableProcessors();
    private String space = " ";
    private int zero = 0;

    @Override
    @Transactional
    public List<Lemma> getLemmaFromSite(Set<String> lemmas, Site site) {

        List<Lemma> listLemma = lemmaRepository.findLemmaListBySite(lemmas, site);
        listLemma.sort(Comparator.comparingInt(Lemma::getFrequency));
        return listLemma;
    }
    @Override
    public Set<String> getLemmaFromSearchText(String text)  {

        return Arrays.stream(text.toLowerCase(Locale.ROOT).split(space))
                .filter(word -> !word.isEmpty())
                .flatMap(word -> {
                    try {
                        return lemmatizer.getLemmas(word).stream();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toSet());
    }
    @Override
    @SneakyThrows
    @Transactional
    public List<SearchDTO> createSearchDtoList(List<Lemma> lemmaList, Set<String> textLemmaList) {

            List<Page> pagesList = pagesRepository.findByLemmaList(lemmaList);

            if (pagesList.isEmpty()) {
                return new ArrayList<>();
            }

            List<SearchIndex> indexesList = indexRepository.findByPageInAndLemmaIn(pagesList, lemmaList);

            if (indexesList.isEmpty()) {
                return new ArrayList<>();
            }

            Map<Page, Float> relevanceMap = getRelevanceFromPage(pagesList, indexesList);

        return getSearchDtoList((ConcurrentHashMap<Page, Float>) relevanceMap, textLemmaList);
    }

    private Map<Page, Float> getRelevanceFromPage(List<Page> pageList, List<SearchIndex> indexList) {

        Map<Page, Float> absRelevanceMap = new ConcurrentHashMap<>();
        Map<Page, Float> relRelevanceMap = new ConcurrentHashMap<>();

        for (Page page : pageList) {

            float relevance = zero;

            for (SearchIndex index : indexList) {

                if (index.getPage() == page) {
                    relevance += index.getRank();
                }
            }
            absRelevanceMap.put(page, relevance);
        }

        float maxRelevance = Collections.max(absRelevanceMap.values());

        for (Map.Entry<Page, Float> entry : absRelevanceMap.entrySet()) {
            float relevance = entry.getValue() / maxRelevance;
            relRelevanceMap.put(entry.getKey(), relevance);
        }

        return relRelevanceMap;
    }


    private List<SearchDTO> getSearchDtoList(ConcurrentHashMap<Page, Float> pageList, Set<String> textLemmaList) {

        List<SearchDTO> resultList = new CopyOnWriteArrayList<>();
        List<Future> tasks = new ArrayList<>();
        ExecutorService executorService = Executors.newFixedThreadPool(newFixedThreadPool);

        for (Map.Entry<Page, Float> entry : pageList.entrySet()) {
            CreateSearchDto searchDto = new CreateSearchDto(entry, textLemmaList, lemmatizer, lemmaSearch);
            Future<SearchDTO> searchDto1 = executorService.submit(searchDto);
            tasks.add(searchDto1);
        }
            for (Future future : tasks) {
                try {
                    SearchDTO searchDtoResult = (SearchDTO) future.get();
                    resultList.add(searchDtoResult);
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }
            executorService.shutdown();
        return resultList;
    }
}
