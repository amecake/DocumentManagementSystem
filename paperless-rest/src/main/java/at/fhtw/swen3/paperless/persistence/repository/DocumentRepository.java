package at.fhtw.swen3.paperless.persistence.repository;

import at.fhtw.swen3.paperless.persistence.entity.DocumentEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {

    // load the tags together with the documents, otherwise every document triggers its own tag query
    @Override
    @EntityGraph(attributePaths = "tags")
    List<DocumentEntity> findAll();

    @EntityGraph(attributePaths = "tags")
    List<DocumentEntity> findByTitleContainingIgnoreCase(String title);
}
