package searchengine.model;

import lombok.Data;
import org.hibernate.annotations.Cascade;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "Site")
public class Site {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToMany(mappedBy = "site")
    @Cascade(org.hibernate.annotations.CascadeType.ALL)
    private List<Page> listOfPages;
    @OneToMany(mappedBy = "site", cascade = CascadeType.PERSIST)
    @Cascade(org.hibernate.annotations.CascadeType.ALL)
    private List<Lemma> listOfLemma;

    @Column(name = "name", columnDefinition = "VARCHAR(255)", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
//    @Column(name = "status", columnDefinition = "enum ('INDEXING', 'INDEXED', 'FAILED')", nullable = false)
    private Status status;

    @Column(name = "status_time", columnDefinition = "DATETIME", nullable = false)
    @DateTimeFormat (pattern = "yyyy-MM-dd hh:mm")
    private LocalDateTime statusTime;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "url", columnDefinition = "VARCHAR(255)", nullable = false)
    private String url;

}
