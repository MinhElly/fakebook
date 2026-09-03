package com.minh.fakebook.media.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class MediaCriteriaTest {

    @Test
    void newMediaCriteriaHasAllFiltersNullTest() {
        var mediaCriteria = new MediaCriteria();
        assertThat(mediaCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void mediaCriteriaFluentMethodsCreatesFiltersTest() {
        var mediaCriteria = new MediaCriteria();

        setAllFilters(mediaCriteria);

        assertThat(mediaCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void mediaCriteriaCopyCreatesNullFilterTest() {
        var mediaCriteria = new MediaCriteria();
        var copy = mediaCriteria.copy();

        assertThat(mediaCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(mediaCriteria)
        );
    }

    @Test
    void mediaCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var mediaCriteria = new MediaCriteria();
        setAllFilters(mediaCriteria);

        var copy = mediaCriteria.copy();

        assertThat(mediaCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(mediaCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var mediaCriteria = new MediaCriteria();

        assertThat(mediaCriteria).hasToString("MediaCriteria{}");
    }

    private static void setAllFilters(MediaCriteria mediaCriteria) {
        mediaCriteria.id();
        mediaCriteria.ownerId();
        mediaCriteria.fileName();
        mediaCriteria.mediaType();
        mediaCriteria.mimeType();
        mediaCriteria.fileSize();
        mediaCriteria.storageProvider();
        mediaCriteria.storageKey();
        mediaCriteria.url();
        mediaCriteria.status();
        mediaCriteria.createdAt();
        mediaCriteria.updatedAt();
        mediaCriteria.distinct();
    }

    private static Condition<MediaCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getOwnerId()) &&
                condition.apply(criteria.getFileName()) &&
                condition.apply(criteria.getMediaType()) &&
                condition.apply(criteria.getMimeType()) &&
                condition.apply(criteria.getFileSize()) &&
                condition.apply(criteria.getStorageProvider()) &&
                condition.apply(criteria.getStorageKey()) &&
                condition.apply(criteria.getUrl()) &&
                condition.apply(criteria.getStatus()) &&
                condition.apply(criteria.getCreatedAt()) &&
                condition.apply(criteria.getUpdatedAt()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<MediaCriteria> copyFiltersAre(MediaCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getOwnerId(), copy.getOwnerId()) &&
                condition.apply(criteria.getFileName(), copy.getFileName()) &&
                condition.apply(criteria.getMediaType(), copy.getMediaType()) &&
                condition.apply(criteria.getMimeType(), copy.getMimeType()) &&
                condition.apply(criteria.getFileSize(), copy.getFileSize()) &&
                condition.apply(criteria.getStorageProvider(), copy.getStorageProvider()) &&
                condition.apply(criteria.getStorageKey(), copy.getStorageKey()) &&
                condition.apply(criteria.getUrl(), copy.getUrl()) &&
                condition.apply(criteria.getStatus(), copy.getStatus()) &&
                condition.apply(criteria.getCreatedAt(), copy.getCreatedAt()) &&
                condition.apply(criteria.getUpdatedAt(), copy.getUpdatedAt()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
