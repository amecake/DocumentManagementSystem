package at.fhtw.swen3.paperless.persistence.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tags")
public class TagEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @ManyToMany(mappedBy = "tags")
    private Set<DocumentEntity> documents = new HashSet<>();

    public TagEntity() {}

    public TagEntity(String name) {
        this.name = name;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Set<DocumentEntity> getDocuments() { return documents; }
    public void setDocuments(Set<DocumentEntity> documents) { this.documents = documents; }
}