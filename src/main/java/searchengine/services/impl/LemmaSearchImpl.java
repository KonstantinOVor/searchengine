package searchengine.services.impl;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import searchengine.dto.LemmaDTO;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.PagesRepository;
import searchengine.services.Lemmatizer;
import searchengine.services.LemmaSearch;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Data
@Slf4j
public class LemmaSearchImpl implements LemmaSearch {
    private final PagesRepository pageRepository;
    private final Lemmatizer lemmatizer;
    private Lock lock = new ReentrantLock();
    private List<LemmaDTO> lemmaDtoList;
    private String selector = "*";
    private String space = " ";
    private int one = 1;

    @Override
    public List<LemmaDTO> startLemmaSearch(Site site) {
        lock.lock();
        try {
            lemmaDtoList = new CopyOnWriteArrayList<>();
            Iterable<Page> pageList = pageRepository.findBySite(site);
            lemmaSummation(pageList).forEach((key, value) -> lemmaDtoList.add(new LemmaDTO(key, value)));
        } finally {
            lock.unlock();
        }
        return lemmaDtoList;
    }

    private Map<String, Integer> lemmaSummation (Iterable<Page> pageList){
        lock.lock();
        try {
            Map<String, Integer> mapLemmas = new ConcurrentHashMap<>();

            for (Page page : pageList) {
                String content = page.getContent();
                String text = deletingTags(content, selector);
                Map<String, Integer> lemmaMap = lemmatizer.getMapLemma(text);
                lemmaMap.forEach((key, value) -> mapLemmas.merge(key, one, Integer::sum));
            }

            return mapLemmas;
        } finally {
            lock.unlock();
        }
    }


    @Override
    public String deletingTags(String content, String selector) {
        lock.lock();
        try {

            Document doc = Jsoup.parse(content);
            Elements elements = doc.select(selector);

            if (elements != null) {
                StringBuffer stringBuffer = new StringBuffer();
                for (Element element : elements) {
                    stringBuffer.append(element.ownText()).append(space);
                }
                return stringBuffer.toString();
            }
        } finally {
            lock.unlock();
        }
        return null;
    }
}
