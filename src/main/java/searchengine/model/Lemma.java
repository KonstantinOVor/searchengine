package searchengine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import javax.persistence.*;
import java.util.List;
@Entity
@Data
@EqualsAndHashCode(exclude = "indexList")
@ToString(exclude = "indexList")
@Table(name = "Lemmas")
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn (name = "site_id", referencedColumnName = "id",nullable = false)
    private Site site;

    @OneToMany(mappedBy = "lemma", cascade = CascadeType.ALL)
    private List<SearchIndex> indexList;

    @Column (name = "lemma", nullable = false)
    private String lemma;

    @Column (name = "frequency", nullable = false)
    private int frequency;
}

