package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.dto.IndexDTO;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PagesRepository;
import searchengine.services.Lemmatizer;
import searchengine.services.PagesParser;
import searchengine.services.LemmaSearch;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


@Service
@RequiredArgsConstructor
@Slf4j
public class PagesParserImpl implements PagesParser {
    private final PagesRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final LemmaSearch lemmaSearch;
    private final Lemmatizer lemmatizer;
    private Lock lock = new ReentrantLock();
    private Map<String, Integer> lemmaMap;
    private  String selector = "*";
    private float zero = 0.0f;
    private int oneHundred = 100;
    private int fourHundred = 400;


    @Override
    public List<IndexDTO> pagesParsing(Site site) {

        List<IndexDTO> indexDTOList = new CopyOnWriteArrayList<>();
        AtomicReference<List<Page>> pageListRef = new AtomicReference<>();
        AtomicReference<List<Lemma>> lemmaListRef = new AtomicReference<>();

        new Thread(() -> {
            pageListRef.set(pageRepository.findAllBySite(site));
        }).start();

        new Thread(() -> {
            lemmaListRef.set(lemmaRepository.findAllBySite(site));
        }).start();


        while (pageListRef.get() == null || lemmaListRef.get() == null) {

            try {
                Thread.sleep(oneHundred);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        List <Page> pageList = pageListRef.get();
        List<Lemma> lemmaList = lemmaListRef.get();

        processPageList(pageList, lemmaList, indexDTOList);

        return indexDTOList;
    }

    private void processPageList(List<Page> pageList, List<Lemma> lemmaList, List<IndexDTO> indexDTOList) {

        for (Page page : pageList) {

            if (page.getCode() >= fourHundred) {
                log.debug("Bad status code - " + page.getCode());
                continue;
            }

                String content = lemmaSearch.deletingTags(page.getContent(), selector);
                lemmaMap = lemmatizer.getMapLemma(content);
                processLemmaList(lemmaList, lemmaMap, page, indexDTOList);

        }
    }

    private void processLemmaList(List<Lemma> lemmaList, Map<String, Integer> lemmaMap, Page page,
                                  List<IndexDTO> indexDTOList) {

        for (Lemma lemma : lemmaList) {

            float totalRank = zero;
            String word = lemma.getLemma();

            if (lemmaMap.containsKey(word)) {
                totalRank += lemmaMap.get(word);
                indexDTOList.add(new IndexDTO(page, lemma, totalRank));
            } else {
                log.debug("Lemma not found - " + word);
            }
        }
    }
}
