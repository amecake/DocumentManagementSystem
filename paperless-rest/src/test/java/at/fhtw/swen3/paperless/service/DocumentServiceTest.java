package at.fhtw.swen3.paperless.service;

import at.fhtw.swen3.paperless.exception.ConflictException;
import at.fhtw.swen3.paperless.exception.NotFoundException;
import at.fhtw.swen3.paperless.exception.StorageException;
import at.fhtw.swen3.paperless.messaging.DocumentEventPublisher;
import at.fhtw.swen3.paperless.messaging.DocumentUploadedEvent;
import at.fhtw.swen3.paperless.persistence.entity.DocumentEntity;
import at.fhtw.swen3.paperless.persistence.entity.TagEntity;
import at.fhtw.swen3.paperless.persistence.repository.DocumentRepository;
import at.fhtw.swen3.paperless.persistence.repository.TagRepository;
import at.fhtw.swen3.paperless.service.dto.DocumentDto;
import at.fhtw.swen3.paperless.service.dto.DocumentUpdateDto;
import at.fhtw.swen3.paperless.service.mapper.DocumentMapper;
import at.fhtw.swen3.paperless.storage.FileStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private DocumentEventPublisher eventPublisher;

    @Mock
    private FileStorage fileStorage;

    private DocumentService service;

    @BeforeEach
    void setUp() {
        service = new DocumentServiceImpl(documentRepository, Mappers.getMapper(DocumentMapper.class), tagRepository,
                eventPublisher, fileStorage);
    }

    private static DocumentEntity document(long id, String title) {
        DocumentEntity e = new DocumentEntity();
        e.setId(id);
        e.setTitle(title);
        e.setOriginalFilename(title + ".pdf");
        return e;
    }

    private static DocumentEntity storedDocument(long id, String storageKey) {
        DocumentEntity e = document(id, "a");
        e.setContentType("application/pdf");
        e.setStorageKey(storageKey);
        return e;
    }

    private static InputStream pdf() {
        return new ByteArrayInputStream(new byte[4]);
    }

    @Test
    void upload_validPdf_isPersisted() {
        when(documentRepository.save(any())).thenAnswer(inv -> {
            DocumentEntity e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        DocumentDto result = service.upload("My invoice", "invoice.pdf", 8, pdf());

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.title()).isEqualTo("My invoice");
        assertThat(result.originalFilename()).isEqualTo("invoice.pdf");
        verify(fileStorage).store(any(), eq(8L), eq("application/pdf"));
    }

    @Test
    void upload_withoutTitle_usesFilename() {
        when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.upload("  ", "scan.pdf", 4, pdf()).title()).isEqualTo("scan.pdf");
    }

    @Test
    void upload_messageSent_statusIsPending() {
        when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(eventPublisher.publish(any())).thenReturn(true);

        assertThat(service.upload("Scan", "scan.pdf", 4, pdf()).ocrStatus()).isEqualTo("PENDING");
    }

    @Test
    void upload_messageCouldNotBeSent_documentIsSavedWithStatusFailed() {
        when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(eventPublisher.publish(any())).thenReturn(false);

        DocumentDto result = service.upload("Scan", "scan.pdf", 4, pdf());

        assertThat(result.title()).isEqualTo("Scan");
        assertThat(result.ocrStatus()).isEqualTo("FAILED");
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
        when(documentRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.update(1L, new DocumentUpdateDto(" new ")).title()).isEqualTo("new");
    }

    @Test
    void update_flushesSoThatUpdatedAtIsCurrent() {
        DocumentEntity doc = document(1, "old");
        when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));
        when(documentRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        service.update(1L, new DocumentUpdateDto("new"));

        verify(documentRepository).saveAndFlush(doc);
        verify(documentRepository, never()).save(any());
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

    @Test
    void upload_sendsMessageToQueue() {
        when(fileStorage.store(any(), anyLong(), any())).thenReturn("key-1");
        when(documentRepository.save(any())).thenAnswer(inv -> {
            DocumentEntity e = inv.getArgument(0);
            e.setId(7L);
            return e;
        });

        service.upload("Scan", "scan.pdf", 4, pdf());

        verify(eventPublisher).publish(new DocumentUploadedEvent(7L, "Scan", "scan.pdf", "key-1"));
    }

    @Test
    void upload_storesFileAndKeepsItsKeyOnTheDocument() {
        when(fileStorage.store(any(), eq(4L), eq("application/pdf"))).thenReturn("key-1");
        when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.upload("Scan", "scan.pdf", 4, pdf());

        ArgumentCaptor<DocumentEntity> saved = ArgumentCaptor.forClass(DocumentEntity.class);
        verify(documentRepository).save(saved.capture());
        assertThat(saved.getValue().getStorageKey()).isEqualTo("key-1");
    }

    @Test
    void upload_databaseFails_removesStoredFileAgain() {
        when(fileStorage.store(any(), anyLong(), any())).thenReturn("key-1");
        when(documentRepository.save(any())).thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> service.upload("Scan", "scan.pdf", 4, pdf()))
                .isInstanceOf(IllegalStateException.class);

        verify(fileStorage).delete("key-1");
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void upload_storageFails_documentIsNotSaved() {
        when(fileStorage.store(any(), anyLong(), any())).thenThrow(new StorageException("minio down", null));

        assertThatThrownBy(() -> service.upload("Scan", "scan.pdf", 4, pdf()))
                .isInstanceOf(StorageException.class);

        verify(documentRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void delete_removesStoredFile() {
        DocumentEntity doc = storedDocument(1, "key-1");
        when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));

        service.delete(1L);

        verify(documentRepository).delete(doc);
        verify(fileStorage).delete("key-1");
    }

    @Test
    void delete_storageFails_documentIsStillDeleted() {
        DocumentEntity doc = storedDocument(1, "key-1");
        when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));
        doThrow(new StorageException("minio down", null)).when(fileStorage).delete("key-1");

        assertThatCode(() -> service.delete(1L)).doesNotThrowAnyException();

        verify(documentRepository).delete(doc);
    }

    @Test
    void delete_withoutStoredFile_doesNotTouchStorage() {
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document(1, "old")));

        service.delete(1L);

        verifyNoInteractions(fileStorage);
    }

    @Test
    void download_returnsContentFromStorage() {
        InputStream stream = pdf();
        when(documentRepository.findById(1L)).thenReturn(Optional.of(storedDocument(1, "key-1")));
        when(fileStorage.retrieve("key-1")).thenReturn(stream);

        var content = service.download(1L);

        assertThat(content.filename()).isEqualTo("a.pdf");
        assertThat(content.contentType()).isEqualTo("application/pdf");
        assertThat(content.stream()).isSameAs(stream);
    }

    @Test
    void download_withoutStoredFile_throwsNotFound() {
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document(1, "old")));

        assertThatThrownBy(() -> service.download(1L)).isInstanceOf(NotFoundException.class);
        verifyNoInteractions(fileStorage);
    }

    @Test
    void download_missingDocument_throwsNotFound() {
        when(documentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.download(99L)).isInstanceOf(NotFoundException.class);
    }
}
