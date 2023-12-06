package searchengine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ColumnTransformer;

import javax.persistence.*;
import java.util.List;

@Data
@EqualsAndHashCode(exclude = "indexList")
@ToString(exclude = "indexList")
@Entity
@Table(name = "Lemma")
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn (nullable = false)
    private Site site;

    @OneToMany(mappedBy = "lemma")
    @Cascade(org.hibernate.annotations.CascadeType.ALL)
    private List<SearchIndex> indexList;

    //    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    private Page page;
    @Column (name = "lemma", columnDefinition = "VARCHAR(255)", nullable = false)
    private String lemma;

    @Column (name = "frequency", nullable = false)
//    @ColumnTransformer (read = "frequency", write = "LEAST(frequency, page.site_id)")
    private int frequency;

}

