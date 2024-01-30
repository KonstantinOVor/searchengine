package searchengine.model;

import lombok.Data;
import javax.persistence.*;
@Data
@Entity
@Table (name = "SearchIndex", uniqueConstraints = {@UniqueConstraint(columnNames = {"page_id", "lemma_id"})})
public class SearchIndex {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "page_id", referencedColumnName = "id", nullable = false)
    private Page page;
    @ManyToOne
    @JoinColumn(name = "lemma_id", referencedColumnName = "id", nullable = false)
    private Lemma lemma;

    @Column (name = "`rank`", nullable = false)
    private float rank;
}