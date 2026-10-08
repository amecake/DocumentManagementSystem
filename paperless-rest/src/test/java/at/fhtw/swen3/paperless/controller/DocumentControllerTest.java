package at.fhtw.swen3.paperless.controller;

import at.fhtw.swen3.paperless.exception.ConflictException;
import at.fhtw.swen3.paperless.exception.NotFoundException;
import at.fhtw.swen3.paperless.service.DocumentService;
import at.fhtw.swen3.paperless.service.dto.DocumentDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private DocumentService documentService;

    private static DocumentDto dto(long id, String title) {
        return new DocumentDto(id, title, title + ".pdf", "application/pdf", 10, Instant.now(), Instant.now());
    }

    @Test
    void upload_returns201() throws Exception {
        var file = new MockMultipartFile("file", "a.pdf", "application/pdf", "%PDF".getBytes());
        when(documentService.upload(eq("A"), eq("a.pdf"), eq(4L))).thenReturn(dto(1, "A"));

        mvc.perform(multipart("/api/documents").file(file).param("title", "A"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/documents/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("A"));
    }

    @Test
    void upload_nonPdf_returns400() throws Exception {
        var file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1});

        mvc.perform(multipart("/api/documents").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Only PDF documents are supported"));
        verifyNoInteractions(documentService);
    }

    @Test
    void upload_emptyFile_returns400() throws Exception {
        var file = new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[0]);

        mvc.perform(multipart("/api/documents").file(file)).andExpect(status().isBadRequest());
        verifyNoInteractions(documentService);
    }

    @Test
    void upload_titleTooLong_returns400() throws Exception {
        var file = new MockMultipartFile("file", "a.pdf", "application/pdf", "%PDF".getBytes());

        mvc.perform(multipart("/api/documents").file(file).param("title", "x".repeat(256)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(documentService);
    }

    @Test
    void upload_filenameTooLong_returns400() throws Exception {
        var file = new MockMultipartFile("file", "x".repeat(252) + ".pdf", "application/pdf", "%PDF".getBytes());

        mvc.perform(multipart("/api/documents").file(file)).andExpect(status().isBadRequest());
        verifyNoInteractions(documentService);
    }

    @Test
    void getAll_returnsList() throws Exception {
        when(documentService.search(null)).thenReturn(List.of(dto(1, "A"), dto(2, "B")));

        mvc.perform(get("/api/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getAll_empty_returns204() throws Exception {
        when(documentService.search(null)).thenReturn(List.of());

        mvc.perform(get("/api/documents")).andExpect(status().isNoContent());
    }

    @Test
    void getById_missing_returns404() throws Exception {
        when(documentService.findById(9L)).thenThrow(new NotFoundException("Document 9 not found"));

        mvc.perform(get("/api/documents/9"))
                .andExpect(status().isNotFound());
    }

    @Test
    void search_passesQuery() throws Exception {
        when(documentService.search("hello")).thenReturn(List.of(dto(1, "Hello")));

        mvc.perform(get("/api/documents").param("q", "hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Hello"));
    }

    @Test
    void update_blankTitle_returns400() throws Exception {
        mvc.perform(put("/api/documents/1").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(documentService);
    }

    @Test
    void update_valid_returns200() throws Exception {
        when(documentService.update(eq(1L), any())).thenReturn(dto(1, "New"));

        mvc.perform(put("/api/documents/1").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"New\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New"));
    }

    @Test
    void delete_returns204() throws Exception {
        mvc.perform(delete("/api/documents/1")).andExpect(status().isNoContent());
        verify(documentService).delete(1L);
    }

    @Test
    void addTag_returns201WithLocation() throws Exception {
        when(documentService.addTag(1L, "Finance")).thenReturn("finance");

        mvc.perform(post("/api/documents/1/tags").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Finance\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/documents/1/tags/finance"))
                .andExpect(jsonPath("$.name").value("finance"));
    }

    @Test
    void addTag_blankName_returns400() throws Exception {
        mvc.perform(post("/api/documents/1/tags").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(documentService);
    }

    @Test
    void addTag_tooLong_returns400() throws Exception {
        String body = "{\"name\":\"" + "x".repeat(51) + "\"}";
        mvc.perform(post("/api/documents/1/tags").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(documentService);
    }

    @Test
    void addTag_duplicate_returns409() throws Exception {
        when(documentService.addTag(1L, "finance")).thenThrow(new ConflictException("already tagged"));

        mvc.perform(post("/api/documents/1/tags").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"finance\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void getTags_returnsList() throws Exception {
        when(documentService.getTags(1L)).thenReturn(List.of("finance", "urgent"));

        mvc.perform(get("/api/documents/1/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("finance"));
    }

    @Test
    void getTags_none_returns204() throws Exception {
        when(documentService.getTags(1L)).thenReturn(List.of());

        mvc.perform(get("/api/documents/1/tags")).andExpect(status().isNoContent());
    }

    @Test
    void removeTag_returns204() throws Exception {
        mvc.perform(delete("/api/documents/1/tags/finance")).andExpect(status().isNoContent());
        verify(documentService).removeTag(1L, "finance");
    }

    @Test
    void unknownUrl_returns404() throws Exception {
        mvc.perform(get("/api/gibtsned")).andExpect(status().isNotFound());
    }

    @Test
    void upload_withoutFile_returns400() throws Exception {
        mvc.perform(multipart("/api/documents").param("title", "NoFile"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(documentService);
    }

    @Test
    void unexpectedError_returns500() throws Exception {
        when(documentService.search(null)).thenThrow(new IllegalStateException("boom"));

        mvc.perform(get("/api/documents")).andExpect(status().isInternalServerError());
    }
}
