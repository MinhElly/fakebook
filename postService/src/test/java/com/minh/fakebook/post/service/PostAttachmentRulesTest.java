package com.minh.fakebook.post.service;
import static org.assertj.core.api.Assertions.*;
import java.util.*;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class PostAttachmentRulesTest {
    @Test void acceptsEmptyAndThirtyOrderedIds() {
        var ids = IntStream.range(0, 30).mapToObj(i -> UUID.randomUUID()).toList();
        assertThatCode(() -> PostAttachmentRules.validate(null)).doesNotThrowAnyException();
        assertThatCode(() -> PostAttachmentRules.validate(List.of())).doesNotThrowAnyException();
        assertThatCode(() -> PostAttachmentRules.validate(ids)).doesNotThrowAnyException();
        assertThat(ids).hasSize(30);
    }
    @Test void rejectsThirtyOneEvenWhenSomeIdsAreDuplicates() {
        assertThatThrownBy(() -> PostAttachmentRules.validate(Collections.nCopies(31, UUID.randomUUID())))
            .isInstanceOf(ResponseStatusException.class);
    }
    @Test void rejectsDuplicatesAndNullEntries() {
        var id = UUID.randomUUID();
        assertThatThrownBy(() -> PostAttachmentRules.validate(List.of(id, id))).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> PostAttachmentRules.validate(Arrays.asList(id, null))).isInstanceOf(ResponseStatusException.class);
    }
}
