package com.minh.fakebook.post.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class PostMediaTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + 2 * Short.MAX_VALUE);

    public static PostMedia getPostMediaSample1() {
        return new PostMedia()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .mediaId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .displayOrder(1);
    }

    public static PostMedia getPostMediaSample2() {
        return new PostMedia()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .mediaId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .displayOrder(2);
    }

    public static PostMedia getPostMediaRandomSampleGenerator() {
        return new PostMedia().id(UUID.randomUUID()).mediaId(UUID.randomUUID()).displayOrder(intCount.incrementAndGet());
    }
}
