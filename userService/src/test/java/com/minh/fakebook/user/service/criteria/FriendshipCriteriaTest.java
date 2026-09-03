package com.minh.fakebook.user.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class FriendshipCriteriaTest {

    @Test
    void newFriendshipCriteriaHasAllFiltersNullTest() {
        var friendshipCriteria = new FriendshipCriteria();
        assertThat(friendshipCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void friendshipCriteriaFluentMethodsCreatesFiltersTest() {
        var friendshipCriteria = new FriendshipCriteria();

        setAllFilters(friendshipCriteria);

        assertThat(friendshipCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void friendshipCriteriaCopyCreatesNullFilterTest() {
        var friendshipCriteria = new FriendshipCriteria();
        var copy = friendshipCriteria.copy();

        assertThat(friendshipCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(friendshipCriteria)
        );
    }

    @Test
    void friendshipCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var friendshipCriteria = new FriendshipCriteria();
        setAllFilters(friendshipCriteria);

        var copy = friendshipCriteria.copy();

        assertThat(friendshipCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(friendshipCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var friendshipCriteria = new FriendshipCriteria();

        assertThat(friendshipCriteria).hasToString("FriendshipCriteria{}");
    }

    private static void setAllFilters(FriendshipCriteria friendshipCriteria) {
        friendshipCriteria.id();
        friendshipCriteria.createdAt();
        friendshipCriteria.userId();
        friendshipCriteria.friendId();
        friendshipCriteria.distinct();
    }

    private static Condition<FriendshipCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getCreatedAt()) &&
                condition.apply(criteria.getUserId()) &&
                condition.apply(criteria.getFriendId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<FriendshipCriteria> copyFiltersAre(FriendshipCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getCreatedAt(), copy.getCreatedAt()) &&
                condition.apply(criteria.getUserId(), copy.getUserId()) &&
                condition.apply(criteria.getFriendId(), copy.getFriendId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
