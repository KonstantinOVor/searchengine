package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import java.util.Collection;
import java.util.List;


@Repository
public interface PagesRepository extends JpaRepository<Page, Long> {
    long countBySite(Site site);
    List <Page> findAllBySite(Site site);
    Page findByPath(String path);

    Iterable<Page> findBySite(Site site);

    @Transactional
    @Query(value = "SELECT * FROM Page as p JOIN search_index as si ON p.id = si.page_id " +
            "WHERE si.lemma_id IN (:lemmas)", nativeQuery = true)
    List<Page> findByLemmaList(@Param("lemmas") Collection<Lemma> lemmas);

}
