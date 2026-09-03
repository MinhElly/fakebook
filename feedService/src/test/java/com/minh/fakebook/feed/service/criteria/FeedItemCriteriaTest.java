package com.minh.fakebook.feed.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class FeedItemCriteriaTest {

    @Test
    void newFeedItemCriteriaHasAllFiltersNullTest() {
        var feedItemCriteria = new FeedItemCriteria();
        assertThat(feedItemCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void feedItemCriteriaFluentMethodsCreatesFiltersTest() {
        var feedItemCriteria = new FeedItemCriteria();

        setAllFilters(feedItemCriteria);

        assertThat(feedItemCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void feedItemCriteriaCopyCreatesNullFilterTest() {
        var feedItemCriteria = new FeedItemCriteria();
        var copy = feedItemCriteria.copy();

        assertThat(feedItemCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(feedItemCriteria)
        );
    }

    @Test
    void feedItemCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var feedItemCriteria = new FeedItemCriteria();
        setAllFilters(feedItemCriteria);

        var copy = feedItemCriteria.copy();

        assertThat(feedItemCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(feedItemCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var feedItemCriteria = new FeedItemCriteria();

        assertThat(feedItemCriteria).hasToString("FeedItemCriteria{}");
    }

    private static void setAllFilters(FeedItemCriteria feedItemCriteria) {
        feedItemCriteria.id();
        feedItemCriteria.userId();
        feedItemCriteria.postId();
        feedItemCriteria.createdAt();
        feedItemCriteria.distinct();
    }

    private static Condition<FeedItemCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getUserId()) &&
                condition.apply(criteria.getPostId()) &&
                condition.apply(criteria.getCreatedAt()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<FeedItemCriteria> copyFiltersAre(FeedItemCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getUserId(), copy.getUserId()) &&
                condition.apply(criteria.getPostId(), copy.getPostId()) &&
                condition.apply(criteria.getCreatedAt(), copy.getCreatedAt()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
