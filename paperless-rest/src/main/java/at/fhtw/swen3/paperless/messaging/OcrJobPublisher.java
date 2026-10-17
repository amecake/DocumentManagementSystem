package at.fhtw.swen3.paperless.messaging;

public interface OcrJobPublisher {

    boolean publish(OcrJobMessage job);
}
