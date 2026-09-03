package com.minh.fakebook.post.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class PostReactionCriteriaTest {

    @Test
    void newPostReactionCriteriaHasAllFiltersNullTest() {
        var postReactionCriteria = new PostReactionCriteria();
        assertThat(postReactionCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void postReactionCriteriaFluentMethodsCreatesFiltersTest() {
        var postReactionCriteria = new PostReactionCriteria();

        setAllFilters(postReactionCriteria);

        assertThat(postReactionCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void postReactionCriteriaCopyCreatesNullFilterTest() {
        var postReactionCriteria = new PostReactionCriteria();
        var copy = postReactionCriteria.copy();

        assertThat(postReactionCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(postReactionCriteria)
        );
    }

    @Test
    void postReactionCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var postReactionCriteria = new PostReactionCriteria();
        setAllFilters(postReactionCriteria);

        var copy = postReactionCriteria.copy();

        assertThat(postReactionCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(postReactionCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var postReactionCriteria = new PostReactionCriteria();

        assertThat(postReactionCriteria).hasToString("PostReactionCriteria{}");
    }

    private static void setAllFilters(PostReactionCriteria postReactionCriteria) {
        postReactionCriteria.id();
        postReactionCriteria.userId();
        postReactionCriteria.reactionType();
        postReactionCriteria.createdAt();
        postReactionCriteria.updatedAt();
        postReactionCriteria.postId();
        postReactionCriteria.distinct();
    }

    private static Condition<PostReactionCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getUserId()) &&
                condition.apply(criteria.getReactionType()) &&
                condition.apply(criteria.getCreatedAt()) &&
                condition.apply(criteria.getUpdatedAt()) &&
                condition.apply(criteria.getPostId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<PostReactionCriteria> copyFiltersAre(
        PostReactionCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getUserId(), copy.getUserId()) &&
                condition.apply(criteria.getReactionType(), copy.getReactionType()) &&
                condition.apply(criteria.getCreatedAt(), copy.getCreatedAt()) &&
                condition.apply(criteria.getUpdatedAt(), copy.getUpdatedAt()) &&
                condition.apply(criteria.getPostId(), copy.getPostId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
