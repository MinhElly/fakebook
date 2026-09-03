package com.minh.fakebook.user.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class UserProfileCriteriaTest {

    @Test
    void newUserProfileCriteriaHasAllFiltersNullTest() {
        var userProfileCriteria = new UserProfileCriteria();
        assertThat(userProfileCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void userProfileCriteriaFluentMethodsCreatesFiltersTest() {
        var userProfileCriteria = new UserProfileCriteria();

        setAllFilters(userProfileCriteria);

        assertThat(userProfileCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void userProfileCriteriaCopyCreatesNullFilterTest() {
        var userProfileCriteria = new UserProfileCriteria();
        var copy = userProfileCriteria.copy();

        assertThat(userProfileCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(userProfileCriteria)
        );
    }

    @Test
    void userProfileCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var userProfileCriteria = new UserProfileCriteria();
        setAllFilters(userProfileCriteria);

        var copy = userProfileCriteria.copy();

        assertThat(userProfileCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(userProfileCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var userProfileCriteria = new UserProfileCriteria();

        assertThat(userProfileCriteria).hasToString("UserProfileCriteria{}");
    }

    private static void setAllFilters(UserProfileCriteria userProfileCriteria) {
        userProfileCriteria.id();
        userProfileCriteria.username();
        userProfileCriteria.displayName();
        userProfileCriteria.avatarMediaId();
        userProfileCriteria.coverMediaId();
        userProfileCriteria.createdAt();
        userProfileCriteria.updatedAt();
        userProfileCriteria.distinct();
    }

    private static Condition<UserProfileCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getUsername()) &&
                condition.apply(criteria.getDisplayName()) &&
                condition.apply(criteria.getAvatarMediaId()) &&
                condition.apply(criteria.getCoverMediaId()) &&
                condition.apply(criteria.getCreatedAt()) &&
                condition.apply(criteria.getUpdatedAt()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<UserProfileCriteria> copyFiltersAre(UserProfileCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getUsername(), copy.getUsername()) &&
                condition.apply(criteria.getDisplayName(), copy.getDisplayName()) &&
                condition.apply(criteria.getAvatarMediaId(), copy.getAvatarMediaId()) &&
                condition.apply(criteria.getCoverMediaId(), copy.getCoverMediaId()) &&
                condition.apply(criteria.getCreatedAt(), copy.getCreatedAt()) &&
                condition.apply(criteria.getUpdatedAt(), copy.getUpdatedAt()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
