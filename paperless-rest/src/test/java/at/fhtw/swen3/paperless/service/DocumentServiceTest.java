package at.fhtw.swen3.paperless.service;

import at.fhtw.swen3.paperless.exception.ConflictException;
import at.fhtw.swen3.paperless.exception.NotFoundException;
import at.fhtw.swen3.paperless.persistence.entity.DocumentEntity;
import at.fhtw.swen3.paperless.persistence.entity.TagEntity;
import at.fhtw.swen3.paperless.persistence.repository.DocumentRepository;
import at.fhtw.swen3.paperless.persistence.repository.TagRepository;
import at.fhtw.swen3.paperless.service.dto.DocumentDto;
import at.fhtw.swen3.paperless.service.dto.DocumentUpdateDto;
import at.fhtw.swen3.paperless.service.mapper.DocumentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private TagRepository tagRepository;

    private DocumentService service;

    @BeforeEach
    void setUp() {
        service = new DocumentService(documentRepository, Mappers.getMapper(DocumentMapper.class), tagRepository);
    }

    private static DocumentEntity document(long id, String title) {
        DocumentEntity e = new DocumentEntity();
        e.setId(id);
        e.setTitle(title);
        e.setOriginalFilename(title + ".pdf");
        return e;
    }

    @Test
    void upload_validPdf_isPersisted() {
        when(documentRepository.save(any())).thenAnswer(inv -> {
            DocumentEntity e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        DocumentDto result = service.upload("My invoice", "invoice.pdf", 8);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.title()).isEqualTo("My invoice");
        assertThat(result.originalFilename()).isEqualTo("invoice.pdf");
    }

    @Test
    void upload_withoutTitle_usesFilename() {
        when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.upload("  ", "scan.pdf", 4).title()).isEqualTo("scan.pdf");
    }

    @Test
    void findById_missing_throwsNotFound() {
        when(documentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void findAll_returnsMappedDtos() {
        when(documentRepository.findAll()).thenReturn(List.of(document(1, "a"), document(2, "b")));

        assertThat(service.findAll()).extracting(DocumentDto::title).containsExactly("a", "b");
    }

    @Test
    void search_blankQuery_returnsAll() {
        when(documentRepository.findAll()).thenReturn(List.of(document(1, "a")));

        assertThat(service.search(" ")).hasSize(1);
        verify(documentRepository, never()).findByTitleContainingIgnoreCase(any());
    }

    @Test
    void search_query_delegatesToRepository() {
        when(documentRepository.findByTitleContainingIgnoreCase("hello")).thenReturn(List.of(document(1, "Hello World")));

        assertThat(service.search(" hello ")).extracting(DocumentDto::title).containsExactly("Hello World");
    }

    @Test
    void update_changesTitle() {
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document(1, "old")));
        when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.update(1L, new DocumentUpdateDto(" new ")).title()).isEqualTo("new");
    }

    @Test
    void delete_existing_deletes() {
        DocumentEntity doc = document(1, "a");
        when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));

        service.delete(1L);

        verify(documentRepository).delete(doc);
    }

    @Test
    void addTag_newTagName_createsAndAttachesTag() {
        DocumentEntity doc = document(1, "invoice");
        when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));
        when(tagRepository.findByName("finance")).thenReturn(Optional.empty());
        when(tagRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.addTag(1L, "finance")).isEqualTo("finance");
        assertThat(doc.getTags()).containsExactly(new TagEntity("finance"));
    }

    @Test
    void addTag_normalisesName() {
        DocumentEntity doc = document(1, "invoice");
        when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));
        when(tagRepository.findByName("finance")).thenReturn(Optional.of(new TagEntity("finance")));

        assertThat(service.addTag(1L, "  Finance ")).isEqualTo("finance");
        verify(tagRepository, never()).save(any());
    }

    @Test
    void addTag_alreadyOnDocument_throwsConflict() {
        DocumentEntity doc = document(1, "invoice");
        doc.getTags().add(new TagEntity("finance"));
        when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));
        // a separately loaded instance with the same name counts as the same tag
        when(tagRepository.findByName("finance")).thenReturn(Optional.of(new TagEntity("finance")));

        assertThatThrownBy(() -> service.addTag(1L, "FINANCE")).isInstanceOf(ConflictException.class);
        assertThat(doc.getTags()).hasSize(1);
    }

    @Test
    void removeTag_removesNormalisedName() {
        DocumentEntity doc = document(1, "invoice");
        doc.getTags().add(new TagEntity("finance"));
        when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));

        service.removeTag(1L, " Finance");

        assertThat(doc.getTags()).isEmpty();
    }

    @Test
    void removeTag_notOnDocument_doesNothing() {
        DocumentEntity doc = document(1, "invoice");
        when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));

        service.removeTag(1L, "finance");

        assertThat(doc.getTags()).isEmpty();
    }

    @Test
    void getTags_returnsSortedNames() {
        DocumentEntity doc = document(1, "invoice");
        doc.getTags().add(new TagEntity("urgent"));
        doc.getTags().add(new TagEntity("finance"));
        when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));

        assertThat(service.getTags(1L)).containsExactly("finance", "urgent");
    }
}
