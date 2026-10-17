package at.fhtw.swen3.paperless.persistence.repository;

import at.fhtw.swen3.paperless.persistence.entity.DocumentEntity;
import at.fhtw.swen3.paperless.persistence.entity.ProcessingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {

    List<DocumentEntity> findByTitleContainingIgnoreCase(String title);

    List<DocumentEntity> findByStatusAndQueuedAtBefore(ProcessingStatus status, Instant queuedBefore);
}
