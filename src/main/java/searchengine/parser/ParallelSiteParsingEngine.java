package searchengine.parser;

import searchengine.dto.LemmaDTO;
import searchengine.model.Lemma;
import searchengine.model.Site;
import searchengine.model.enumModel.Status;

import java.util.List;

public interface ParallelSiteParsingEngine {

    Site getSite(String url, Status status);
    List<Lemma> lemmaSheetAssembly(Site site, List<LemmaDTO> lemmaDtoList) throws InterruptedException;
    void saveIndexesToDatabase(Site site) throws InterruptedException;
}
