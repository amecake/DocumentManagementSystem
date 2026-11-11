package at.fhtw.swen3.paperless.storage;

import at.fhtw.swen3.paperless.exception.NotFoundException;
import at.fhtw.swen3.paperless.exception.StorageException;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.ErrorResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.UUID;

@Component
public class MinioFileStorage implements FileStorage {

    private static final Logger log = LoggerFactory.getLogger(MinioFileStorage.class);

    private final MinioClient client;
    private final String bucket;
    private volatile boolean bucketReady;

    public MinioFileStorage(MinioClient client, @Value("${minio.bucket}") String bucket) {
        this.client = client;
        this.bucket = bucket;
    }

    @Override
    public String store(InputStream content, long size, String contentType) {
        String key = UUID.randomUUID().toString();
        try {
            ensureBucket();
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .stream(content, size, -1)
                    .contentType(contentType)
                    .build());
            log.info("Stored file in MinIO: bucket={}, key={}, size={} bytes", bucket, key, size);
            return key;
        } catch (Exception ex) {
            log.error("Could not store file in MinIO bucket '{}'", bucket, ex);
            throw new StorageException("File could not be stored", ex);
        }
    }

    @Override
    public InputStream retrieve(String storageKey) {
        try {
            return client.getObject(GetObjectArgs.builder().bucket(bucket).object(storageKey).build());
        } catch (ErrorResponseException ex) {
            if ("NoSuchKey".equals(ex.errorResponse().code())) {
                throw new NotFoundException("Stored file " + storageKey + " not found");
            }
            throw new StorageException("File could not be read", ex);
        } catch (Exception ex) {
            log.error("Could not read file {} from MinIO", storageKey, ex);
            throw new StorageException("File could not be read", ex);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(storageKey).build());
            log.info("Deleted file from MinIO: bucket={}, key={}", bucket, storageKey);
        } catch (Exception ex) {
            log.error("Could not delete file {} from MinIO", storageKey, ex);
            throw new StorageException("File could not be deleted", ex);
        }
    }

    private void ensureBucket() throws Exception {
        if (bucketReady) {
            return;
        }
        if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            log.info("Created MinIO bucket '{}'", bucket);
        }
        bucketReady = true;
    }
}
