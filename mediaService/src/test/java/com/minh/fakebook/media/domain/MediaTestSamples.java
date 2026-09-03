package com.minh.fakebook.media.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class MediaTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    public static Media getMediaSample1() {
        return new Media()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .ownerId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .fileName("fileName1")
            .mimeType("mimeType1")
            .fileSize(1L)
            .storageKey("storageKey1")
            .url("url1");
    }

    public static Media getMediaSample2() {
        return new Media()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .ownerId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .fileName("fileName2")
            .mimeType("mimeType2")
            .fileSize(2L)
            .storageKey("storageKey2")
            .url("url2");
    }

    public static Media getMediaRandomSampleGenerator() {
        return new Media()
            .id(UUID.randomUUID())
            .ownerId(UUID.randomUUID())
            .fileName(UUID.randomUUID().toString())
            .mimeType(UUID.randomUUID().toString())
            .fileSize(longCount.incrementAndGet())
            .storageKey(UUID.randomUUID().toString())
            .url(UUID.randomUUID().toString());
    }
}
