package searchengine.model;

import lombok.Data;

import javax.persistence.*;
@Data
@Entity
@Table (name = "`Index`")
public class SearchIndex {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne (cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn (nullable = false)
    private Page page;
    @ManyToOne (cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn (nullable = false)
    private Lemma lemma;

    @Column (name = "`rank`", nullable = false)
    private float rank;
}