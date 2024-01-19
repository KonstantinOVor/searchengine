package searchengine.dto;

import searchengine.model.Lemma;
import searchengine.model.Page;


public record IndexDTO(Page pageId, Lemma lemmaId, float rank) {
}
