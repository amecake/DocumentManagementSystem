package at.fhtw.swen3.paperless.storage;

import java.io.InputStream;

public interface FileStorage {

    /** Stores the content and returns the unique storage key (object name). */
    String store(InputStream content, long size, String contentType);

    InputStream retrieve(String storageKey);

    void delete(String storageKey);
}
