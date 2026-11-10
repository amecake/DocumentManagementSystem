package at.fhtw.swen3.paperless.service.mapper;

import at.fhtw.swen3.paperless.persistence.entity.DocumentEntity;
import at.fhtw.swen3.paperless.persistence.entity.TagEntity;
import at.fhtw.swen3.paperless.service.dto.DocumentDto;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentMapperTest {

    private final DocumentMapper mapper = Mappers.getMapper(DocumentMapper.class);

    private static DocumentEntity document() {
        DocumentEntity e = new DocumentEntity();
        e.setId(1L);
        e.setTitle("Invoice");
        e.setOriginalFilename("invoice.pdf");
        return e;
    }

    @Test
    void toDto_mapsTagNamesSorted() {
        DocumentEntity entity = document();
        entity.getTags().add(new TagEntity("urgent"));
        entity.getTags().add(new TagEntity("finance"));

        DocumentDto dto = mapper.toDto(entity);

        assertThat(dto.tags()).containsExactly("finance", "urgent");
        assertThat(dto.title()).isEqualTo("Invoice");
    }

    @Test
    void toDto_withoutTags_returnsEmptyList() {
        assertThat(mapper.toDto(document()).tags()).isEmpty();
    }
}
