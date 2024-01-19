package searchengine.searchComponents;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import searchengine.model.Page;
import searchengine.model.enumModel.ErrorResponse;
import searchengine.services.Lemmatizer;
import searchengine.services.LemmaSearch;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;

@Data
@Slf4j
@RequiredArgsConstructor
public class CreateSearchDto implements Callable<SearchDTO> {
    private final Map.Entry<Page, Float> entry;
    private final Set<String> textLemmaList;
    private final Lemmatizer lemmatizer;
    private final LemmaSearch lemmaSearch;
    private String selector = "*";
    private String titleSelector = "title";
    private String space = " ";
    private String regularSpaces ="\\s+";
    private String multipoint = "... ";
    private String openingTag = "<b>";
    private String closingTag = "</b>";
    private int zero = 0;
    private int one = 1;
    private int tree = 3;
    private int eleven = 12;

    @Override
    public SearchDTO call() {

        StringBuffer stringBuffer = new StringBuffer();
        String site = entry.getKey().getSite().getUrl();
        String siteName = entry.getKey().getSite().getName();
        String uri = entry.getKey().getPath();
        String body = lemmaSearch.deletingTags(entry.getKey().getContent(), selector);
        String title = lemmaSearch.deletingTags(entry.getKey().getContent(), titleSelector);
        stringBuffer.append(body.replaceAll(regularSpaces, space));
        String snippet = getSnippet(stringBuffer.toString(), textLemmaList);
        float relevance = entry.getValue();
        SearchDTO searchDto = new SearchDTO(site, siteName, uri, title, snippet, relevance);

        return searchDto;
    }

    private String getSnippet(String content, Set<String> textLemmaList) {

        List<Integer> lemmaIndex = new CopyOnWriteArrayList<>();
        StringBuffer result = new StringBuffer();

        try {
            for (String lemma : textLemmaList) {
                lemmaIndex.addAll(lemmatizer.findLemmaIndexInText(content, lemma));
            }
        } catch (IOException e) {
            log.error("Lemma index can't be found. " + e.getMessage());
        }

        Collections.sort(lemmaIndex);
        List<String> wordsList = getWordsFromContent(content, lemmaIndex);

        for (int i = 0; i < wordsList.size() && i <= tree; i++) {
            result.append(wordsList.get(i)).append(multipoint).append(System.lineSeparator());
        }

        return result.toString();
    }

    private List<String> getWordsFromContent(String content, List<Integer> lemmaIndex) {

        List<String> result = new CopyOnWriteArrayList<>();

        for (int i = 0; i < lemmaIndex.size(); i++) {
            int start = lemmaIndex.get(i);
            int end = content.indexOf(" ", start);
            int nextPoint = i + one;

            while (nextPoint < lemmaIndex.size() && lemmaIndex.get(nextPoint) - end > 0
                    && lemmaIndex.get(nextPoint) - end < tree) {
                end = content.indexOf(space, lemmaIndex.get(nextPoint));
                nextPoint += one;
            }

            String text = getWordsFromIndexes(start, end, content);
            result.add(text);
        }
        result.sort(Comparator.comparingInt(String::length).reversed());

        return result;
    }

    private String getWordsFromIndexes(int start, int end, String content) {

        String word = content.substring(start, end);
        int prevPoint;
        int lastPoint;

        if (content.lastIndexOf(space, start) != -1) {
            prevPoint = content.lastIndexOf(space, start);
        } else prevPoint = start;

        int nextIndex = end + one;
        int count = zero;

        while (count < eleven && nextIndex < content.length()) {
            nextIndex = content.indexOf(space, nextIndex + one);
            count++;
        }

        lastPoint = content.indexOf(space, nextIndex);
        String text = content.substring(prevPoint, lastPoint);

        try {
            text = text.replaceAll(word, openingTag + word + closingTag);
        } catch (RuntimeException e) {
            log.debug(ErrorResponse.INVALID_SYNTAX.getDescription() + e.getMessage());
        }

        return text;
    }
}
