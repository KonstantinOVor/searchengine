package searchengine.services.impl;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.LuceneMorphologyConfig;
import searchengine.services.Lemmatizer;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;


@Service
@Data
@Slf4j
public class LemmatizerImpl implements Lemmatizer {
    private final LuceneMorphologyConfig luceneMorphology;
    private String regularPunctuation = "\\p{Punct}|\\s+";
    private String regularPunctuationForSearch = "\\p{Punct}|[0-9]|@|©|◄|»|«|—|-|№|…";
    private String regularCyrillic = "([^а-яА-Я])";
    private String regularRussianLetters = "[а-яА-Я]+";
    private String regularExpression = ".*(МС|ПРЕДЛ|СОЮЗ|МЕЖД|ВВОДН|ЧАСТ|Н указат).*";
    private int one = 1;
    private String space = " ";

    @Override
    public Map<String, Integer> getMapLemma(String context) {

        String[] words = arrayContainsWords(context);
        Map<String, Integer> lemmaMap = new ConcurrentHashMap<>();

        for (String element : words) {
            if (element.isEmpty()) {
                continue;
            }
            List<String> wordsList = getLemmasForElement(element);
            addLemmasToMap(wordsList, lemmaMap);
        }
        return lemmaMap;
    }

    private List<String> getLemmasForElement(String element) {

        try {
            return getLemmas(element);
        } catch (IOException e) {
            log.error("Error getting lemmas for word: {}", element, e);
            return Collections.emptyList();
        }
    }

    private void addLemmasToMap(List<String> wordsList, Map<String, Integer> lemmaMap) {

        wordsList.stream()
                .filter(word -> !word.isEmpty())
                .forEach(word -> lemmaMap.merge(word, one, Integer::sum));
    }

    private String[] arrayContainsWords(String text) {

        return text.toLowerCase(Locale.ROOT)
                .replaceAll(regularCyrillic, space)
                .split(regularPunctuation);
    }

    @Override
    public List<String> getLemmas(String string) throws IOException {

        List<String> listLemmas = new CopyOnWriteArrayList<>();
        List<String> listNormalFormWords = luceneMorphology.ruLuceneMorphology().getNormalForms(string);

        for (String word : listNormalFormWords) {
            if (!isCorrectWordForm(word)) {
                listLemmas.add(word);
            }
        }
        return listLemmas;
    }

    private boolean isCorrectWordForm(String word) throws IOException {

        List<String> morphForm = luceneMorphology.ruLuceneMorphology().getMorphInfo(word);

        return morphForm.stream()
                .anyMatch(l -> l.matches(regularExpression));
    }


    @Override
    public Collection<Integer> findLemmaIndexInText(String content, String lemma) throws IOException {

        List<Integer> lemmaIndexList = new CopyOnWriteArrayList<>();
        String[] elements = content.toLowerCase(Locale.ROOT).split(regularPunctuation);
        int index = 0;

        for (String string : elements) {

            if (string.matches(regularRussianLetters)) {
                List<String> lemmas = getLemmas(string);
                addLemmaIndices(lemmas, lemma, index, lemmaIndexList);
            }
            index += string.length() + 1;
        }

        return lemmaIndexList;
    }

    private void addLemmaIndices(List<String> lemmas, String lemma, int index, List<Integer> lemmaIndexList) {

        for (String lem : lemmas) {

            if (lem.equals(lemma)) {
                lemmaIndexList.add(index);
            }
        }
    }
}
