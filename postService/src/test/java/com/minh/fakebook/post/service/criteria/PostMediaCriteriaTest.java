package com.minh.fakebook.post.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class PostMediaCriteriaTest {

    @Test
    void newPostMediaCriteriaHasAllFiltersNullTest() {
        var postMediaCriteria = new PostMediaCriteria();
        assertThat(postMediaCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void postMediaCriteriaFluentMethodsCreatesFiltersTest() {
        var postMediaCriteria = new PostMediaCriteria();

        setAllFilters(postMediaCriteria);

        assertThat(postMediaCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void postMediaCriteriaCopyCreatesNullFilterTest() {
        var postMediaCriteria = new PostMediaCriteria();
        var copy = postMediaCriteria.copy();

        assertThat(postMediaCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(postMediaCriteria)
        );
    }

    @Test
    void postMediaCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var postMediaCriteria = new PostMediaCriteria();
        setAllFilters(postMediaCriteria);

        var copy = postMediaCriteria.copy();

        assertThat(postMediaCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(postMediaCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var postMediaCriteria = new PostMediaCriteria();

        assertThat(postMediaCriteria).hasToString("PostMediaCriteria{}");
    }

    private static void setAllFilters(PostMediaCriteria postMediaCriteria) {
        postMediaCriteria.id();
        postMediaCriteria.mediaId();
        postMediaCriteria.displayOrder();
        postMediaCriteria.createdAt();
        postMediaCriteria.postId();
        postMediaCriteria.distinct();
    }

    private static Condition<PostMediaCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getMediaId()) &&
                condition.apply(criteria.getDisplayOrder()) &&
                condition.apply(criteria.getCreatedAt()) &&
                condition.apply(criteria.getPostId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<PostMediaCriteria> copyFiltersAre(PostMediaCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getMediaId(), copy.getMediaId()) &&
                condition.apply(criteria.getDisplayOrder(), copy.getDisplayOrder()) &&
                condition.apply(criteria.getCreatedAt(), copy.getCreatedAt()) &&
                condition.apply(criteria.getPostId(), copy.getPostId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
