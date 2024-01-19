package searchengine.services;

import searchengine.dto.IndexDTO;
import searchengine.model.Site;

import java.util.List;

public interface PagesParser {
    List<IndexDTO> pagesParsing(Site site);
}
