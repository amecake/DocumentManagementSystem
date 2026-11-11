package at.fhtw.swen3.paperless.service.dto;

import java.io.InputStream;

public record DocumentContent(String filename, String contentType, InputStream stream) {
}
