package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;
import searchengine.model.Site;


@Repository
public interface PagesRepository extends JpaRepository<Page, Long> {
    long countBySite(Site site);

}
