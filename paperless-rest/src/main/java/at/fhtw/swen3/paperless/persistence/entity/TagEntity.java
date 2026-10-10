package at.fhtw.swen3.paperless.persistence.entity;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "tags")
public class TagEntity {

    public static final int MAX_NAME_LENGTH = 50;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = MAX_NAME_LENGTH)
    private String name;

    public TagEntity() {}

    public TagEntity(String name) {
        this.name = name;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    /** Tag names are unique (and stored normalised), so the name identifies a tag. */
    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof TagEntity other && Objects.equals(name, other.name));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }
}
