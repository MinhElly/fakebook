package com.minh.fakebook.user.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class FriendRequestCriteriaTest {

    @Test
    void newFriendRequestCriteriaHasAllFiltersNullTest() {
        var friendRequestCriteria = new FriendRequestCriteria();
        assertThat(friendRequestCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void friendRequestCriteriaFluentMethodsCreatesFiltersTest() {
        var friendRequestCriteria = new FriendRequestCriteria();

        setAllFilters(friendRequestCriteria);

        assertThat(friendRequestCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void friendRequestCriteriaCopyCreatesNullFilterTest() {
        var friendRequestCriteria = new FriendRequestCriteria();
        var copy = friendRequestCriteria.copy();

        assertThat(friendRequestCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(friendRequestCriteria)
        );
    }

    @Test
    void friendRequestCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var friendRequestCriteria = new FriendRequestCriteria();
        setAllFilters(friendRequestCriteria);

        var copy = friendRequestCriteria.copy();

        assertThat(friendRequestCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(friendRequestCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var friendRequestCriteria = new FriendRequestCriteria();

        assertThat(friendRequestCriteria).hasToString("FriendRequestCriteria{}");
    }

    private static void setAllFilters(FriendRequestCriteria friendRequestCriteria) {
        friendRequestCriteria.id();
        friendRequestCriteria.status();
        friendRequestCriteria.createdAt();
        friendRequestCriteria.respondedAt();
        friendRequestCriteria.senderId();
        friendRequestCriteria.receiverId();
        friendRequestCriteria.distinct();
    }

    private static Condition<FriendRequestCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getStatus()) &&
                condition.apply(criteria.getCreatedAt()) &&
                condition.apply(criteria.getRespondedAt()) &&
                condition.apply(criteria.getSenderId()) &&
                condition.apply(criteria.getReceiverId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<FriendRequestCriteria> copyFiltersAre(
        FriendRequestCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getStatus(), copy.getStatus()) &&
                condition.apply(criteria.getCreatedAt(), copy.getCreatedAt()) &&
                condition.apply(criteria.getRespondedAt(), copy.getRespondedAt()) &&
                condition.apply(criteria.getSenderId(), copy.getSenderId()) &&
                condition.apply(criteria.getReceiverId(), copy.getReceiverId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
