package at.fhtw.swen3.paperless.service;

import at.fhtw.swen3.paperless.exception.InvalidFileException;
import at.fhtw.swen3.paperless.exception.NotFoundException;
import at.fhtw.swen3.paperless.persistence.entity.DocumentEntity;
import at.fhtw.swen3.paperless.persistence.repository.DocumentRepository;
import at.fhtw.swen3.paperless.service.dto.DocumentDto;
import at.fhtw.swen3.paperless.service.dto.DocumentUpdateDto;
import at.fhtw.swen3.paperless.service.mapper.DocumentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

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

    private DocumentService service;

    @BeforeEach
    void setUp() {
        service = new DocumentService(documentRepository, Mappers.getMapper(DocumentMapper.class));
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
        var file = new MockMultipartFile("file", "invoice.pdf", "application/pdf", "%PDF-1.4".getBytes());
        when(documentRepository.save(any())).thenAnswer(inv -> {
            DocumentEntity e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        DocumentDto result = service.upload("My invoice", file);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.title()).isEqualTo("My invoice");
        assertThat(result.originalFilename()).isEqualTo("invoice.pdf");
    }

    @Test
    void upload_withoutTitle_usesFilename() {
        var file = new MockMultipartFile("file", "scan.pdf", "application/pdf", "%PDF".getBytes());
        when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.upload("  ", file).title()).isEqualTo("scan.pdf");
    }

    @Test
    void upload_emptyFile_throws() {
        var file = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> service.upload("x", file)).isInstanceOf(InvalidFileException.class);
        verifyNoInteractions(documentRepository);
    }

    @Test
    void upload_nonPdf_throws() {
        var file = new MockMultipartFile("file", "photo.png", "image/png", new byte[]{1, 2});

        assertThatThrownBy(() -> service.upload("x", file))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("PDF");
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
}
