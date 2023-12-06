package searchengine.model;

import lombok.*;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.util.List;

@Builder
@Entity
@Data
@AllArgsConstructor
@EqualsAndHashCode(exclude = "indexList")
@ToString(exclude = "indexList")
@Table(name = "Page", indexes = {@Index(name = "pathIndex", columnList = "path", unique = true)})
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn (name = "site_id", referencedColumnName = "id", nullable = false)
    private Site site;

    @OneToMany(mappedBy = "page")
    @Cascade(org.hibernate.annotations.CascadeType.ALL)
    private List<SearchIndex> indexList;

//    @OneToMany(mappedBy = "page")
//    private List <Lemma> ListOfLemma;

    @Column (name = "path", nullable = false)
    private String path;

    @Column (name = "code", nullable = false)
    private int code;

    @Column(name = "content", columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;

    public Page(Site site, String path, int code, String content) {
        this.site = site;
        this.path = path;
        this.code = code;
        this.content = content;
    }

}
