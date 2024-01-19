package searchengine.services;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface Lemmatizer {
    Map<String, Integer> getMapLemma(String text);
    List<String> getLemmas(String string) throws IOException;
    Collection<Integer> findLemmaIndexInText(String content, String lemma) throws IOException;
}
