package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import java.util.List;
import java.util.Set;


@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Long> {
    long countBySite(Site site);
    List<Lemma> findAllBySite(Site site);

    @Transactional
    @Query(value = "select * from Lemmas l where l.lemma in (:lemmas) AND l.site_id = :site", nativeQuery = true)
    List<Lemma> findLemmaListBySite(Set<String> lemmas, Site site);

    @Transactional
    @Query(value = "SELECT * FROM Lemmas as l JOIN search_index as si ON l.id = si.lemma_id " +
            "WHERE si.page_id = (:page)", nativeQuery = true)
    List<Lemma> findByPage(@Param("page") Page page);
}
