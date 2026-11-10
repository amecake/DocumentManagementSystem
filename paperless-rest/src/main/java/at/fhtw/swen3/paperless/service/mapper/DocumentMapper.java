package at.fhtw.swen3.paperless.service.mapper;

import at.fhtw.swen3.paperless.persistence.entity.DocumentEntity;
import at.fhtw.swen3.paperless.persistence.entity.TagEntity;
import at.fhtw.swen3.paperless.service.dto.DocumentDto;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    DocumentDto toDto(DocumentEntity entity);

    List<DocumentDto> toDtos(List<DocumentEntity> entities);

    // MapStruct uses this for the "tags" property: the DTO only carries the tag names, sorted
    default List<String> tagNames(Set<TagEntity> tags) {
        if (tags == null) {
            return List.of();
        }
        return tags.stream().map(TagEntity::getName).sorted().toList();
    }
}
