package at.fhtw.swen3.paperless.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "documents")
public class DocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "file_size")
    private long fileSize;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", length = 20)
    private ProcessingStatus status;

    @Column(name = "queued_at")
    private Instant queuedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "ocr_text", columnDefinition = "TEXT")
    private String ocrText;

    @ManyToMany
    @JoinTable(
            name = "document_tags",
            joinColumns = @JoinColumn(name = "document_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<TagEntity> tags = new HashSet<>();

    @PrePersist
    void onCreate() {
        uploadedAt = Instant.now();
        updatedAt = uploadedAt;
        if (status == null) {
            status = ProcessingStatus.QUEUED;
            queuedAt = uploadedAt;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public ProcessingStatus getStatus() { return status; }
    public void setStatus(ProcessingStatus status) { this.status = status; }
    public Instant getQueuedAt() { return queuedAt; }
    public void setQueuedAt(Instant queuedAt) { this.queuedAt = queuedAt; }
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
    public String getOcrText() { return ocrText; }
    public void setOcrText(String ocrText) { this.ocrText = ocrText; }
    public Set<TagEntity> getTags() { return tags; }
    public void setTags(Set<TagEntity> tags) { this.tags = tags; }
}
