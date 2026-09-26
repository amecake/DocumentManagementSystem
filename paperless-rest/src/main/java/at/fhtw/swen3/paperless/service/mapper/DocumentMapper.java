package at.fhtw.swen3.paperless.service.mapper;

import at.fhtw.swen3.paperless.persistence.entity.DocumentEntity;
import at.fhtw.swen3.paperless.service.dto.DocumentDto;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    DocumentDto toDto(DocumentEntity entity);

    List<DocumentDto> toDtos(List<DocumentEntity> entities);
}
