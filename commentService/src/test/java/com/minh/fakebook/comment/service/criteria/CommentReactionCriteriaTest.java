package com.minh.fakebook.comment.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class CommentReactionCriteriaTest {

    @Test
    void newCommentReactionCriteriaHasAllFiltersNullTest() {
        var commentReactionCriteria = new CommentReactionCriteria();
        assertThat(commentReactionCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void commentReactionCriteriaFluentMethodsCreatesFiltersTest() {
        var commentReactionCriteria = new CommentReactionCriteria();

        setAllFilters(commentReactionCriteria);

        assertThat(commentReactionCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void commentReactionCriteriaCopyCreatesNullFilterTest() {
        var commentReactionCriteria = new CommentReactionCriteria();
        var copy = commentReactionCriteria.copy();

        assertThat(commentReactionCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(commentReactionCriteria)
        );
    }

    @Test
    void commentReactionCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var commentReactionCriteria = new CommentReactionCriteria();
        setAllFilters(commentReactionCriteria);

        var copy = commentReactionCriteria.copy();

        assertThat(commentReactionCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(commentReactionCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var commentReactionCriteria = new CommentReactionCriteria();

        assertThat(commentReactionCriteria).hasToString("CommentReactionCriteria{}");
    }

    private static void setAllFilters(CommentReactionCriteria commentReactionCriteria) {
        commentReactionCriteria.id();
        commentReactionCriteria.userId();
        commentReactionCriteria.reactionType();
        commentReactionCriteria.createdAt();
        commentReactionCriteria.updatedAt();
        commentReactionCriteria.commentId();
        commentReactionCriteria.distinct();
    }

    private static Condition<CommentReactionCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getUserId()) &&
                condition.apply(criteria.getReactionType()) &&
                condition.apply(criteria.getCreatedAt()) &&
                condition.apply(criteria.getUpdatedAt()) &&
                condition.apply(criteria.getCommentId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<CommentReactionCriteria> copyFiltersAre(
        CommentReactionCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getUserId(), copy.getUserId()) &&
                condition.apply(criteria.getReactionType(), copy.getReactionType()) &&
                condition.apply(criteria.getCreatedAt(), copy.getCreatedAt()) &&
                condition.apply(criteria.getUpdatedAt(), copy.getUpdatedAt()) &&
                condition.apply(criteria.getCommentId(), copy.getCommentId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
