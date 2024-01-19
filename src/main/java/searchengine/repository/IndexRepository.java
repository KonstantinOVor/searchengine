package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SearchIndex;
import java.util.Collection;
import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<SearchIndex, Long> {

    SearchIndex findByPageAndLemma(Page page, Lemma lemma);

    List<SearchIndex> findByPageInAndLemmaIn(Collection<Page> pages, Collection<Lemma> lemmas);
}