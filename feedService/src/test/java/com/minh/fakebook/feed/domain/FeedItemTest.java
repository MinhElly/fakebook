package com.minh.fakebook.feed.domain;

import static com.minh.fakebook.feed.domain.FeedItemTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.minh.fakebook.feed.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class FeedItemTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(FeedItem.class);
        FeedItem feedItem1 = getFeedItemSample1();
        FeedItem feedItem2 = new FeedItem();
        assertThat(feedItem1).isNotEqualTo(feedItem2);

        feedItem2.setId(feedItem1.getId());
        assertThat(feedItem1).isEqualTo(feedItem2);

        feedItem2 = getFeedItemSample2();
        assertThat(feedItem1).isNotEqualTo(feedItem2);
    }
}
