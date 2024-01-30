package searchengine.model;

import lombok.*;
import javax.persistence.*;
import java.util.List;

@Entity
@Data
@EqualsAndHashCode(exclude = "indexList")
@ToString(exclude = "indexList")
@Table(name = "Page", indexes = {@Index(name = "pathIndex", columnList = "path")})
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn (name = "site_id", referencedColumnName = "id", nullable = false)
    private Site site;

    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL)
    private List<SearchIndex> indexList;

    @Column (name = "path", nullable = false)
    private String path;

    @Column (name = "code", nullable = false)
    private int code;

    @Column(name = "content", columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;
}
