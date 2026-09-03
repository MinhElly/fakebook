package com.minh.fakebook.post.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class PostCriteriaTest {

    @Test
    void newPostCriteriaHasAllFiltersNullTest() {
        var postCriteria = new PostCriteria();
        assertThat(postCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void postCriteriaFluentMethodsCreatesFiltersTest() {
        var postCriteria = new PostCriteria();

        setAllFilters(postCriteria);

        assertThat(postCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void postCriteriaCopyCreatesNullFilterTest() {
        var postCriteria = new PostCriteria();
        var copy = postCriteria.copy();

        assertThat(postCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(postCriteria)
        );
    }

    @Test
    void postCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var postCriteria = new PostCriteria();
        setAllFilters(postCriteria);

        var copy = postCriteria.copy();

        assertThat(postCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(postCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var postCriteria = new PostCriteria();

        assertThat(postCriteria).hasToString("PostCriteria{}");
    }

    private static void setAllFilters(PostCriteria postCriteria) {
        postCriteria.id();
        postCriteria.authorId();
        postCriteria.visibility();
        postCriteria.status();
        postCriteria.createdAt();
        postCriteria.updatedAt();
        postCriteria.distinct();
    }

    private static Condition<PostCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getAuthorId()) &&
                condition.apply(criteria.getVisibility()) &&
                condition.apply(criteria.getStatus()) &&
                condition.apply(criteria.getCreatedAt()) &&
                condition.apply(criteria.getUpdatedAt()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<PostCriteria> copyFiltersAre(PostCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getAuthorId(), copy.getAuthorId()) &&
                condition.apply(criteria.getVisibility(), copy.getVisibility()) &&
                condition.apply(criteria.getStatus(), copy.getStatus()) &&
                condition.apply(criteria.getCreatedAt(), copy.getCreatedAt()) &&
                condition.apply(criteria.getUpdatedAt(), copy.getUpdatedAt()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
