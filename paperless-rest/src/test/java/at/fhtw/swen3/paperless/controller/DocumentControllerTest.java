package at.fhtw.swen3.paperless.controller;

import at.fhtw.swen3.paperless.exception.InvalidFileException;
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
        when(documentService.upload(eq("A"), any())).thenReturn(dto(1, "A"));

        mvc.perform(multipart("/api/documents").file(file).param("title", "A"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("A"));
    }

    @Test
    void upload_invalidFile_returns400() throws Exception {
        var file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1});
        when(documentService.upload(any(), any())).thenThrow(new InvalidFileException("Only PDF"));

        mvc.perform(multipart("/api/documents").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Only PDF"));
    }

    @Test
    void getAll_returnsList() throws Exception {
        when(documentService.findAll()).thenReturn(List.of(dto(1, "A"), dto(2, "B")));

        mvc.perform(get("/api/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
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

        mvc.perform(get("/api/documents/search").param("q", "hello"))
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
    void unexpectedError_returns500() throws Exception {
        when(documentService.findAll()).thenThrow(new IllegalStateException("boom"));

        mvc.perform(get("/api/documents")).andExpect(status().isInternalServerError());
    }
}
