package at.fhtw.swen3.paperless.persistence;

import at.fhtw.swen3.paperless.persistence.entity.DocumentEntity;
import at.fhtw.swen3.paperless.persistence.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

/** Runs against in-memory H2, not the production PostgreSQL. */
@DataJpaTest
class DocumentRepositoryTest {

    @Autowired
    private DocumentRepository documentRepository;

    private DocumentEntity newDocument(String title) {
        DocumentEntity e = new DocumentEntity();
        e.setTitle(title);
        e.setOriginalFilename(title + ".pdf");
        return e;
    }

    @Test
    void save_setsIdAndTimestamps() {
        DocumentEntity saved = documentRepository.saveAndFlush(newDocument("HelloWorld"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUploadedAt()).isNotNull();
    }

    @Test
    void findByTitle_isCaseInsensitive() {
        documentRepository.save(newDocument("HelloWorld"));
        documentRepository.save(newDocument("Invoice"));

        assertThat(documentRepository.findByTitleContainingIgnoreCase("hello"))
                .extracting(DocumentEntity::getTitle).containsExactly("HelloWorld");
    }
}
