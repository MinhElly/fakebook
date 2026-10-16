package com.minh.fakebook.post.service.dto;

import com.minh.fakebook.post.domain.enumeration.ReactionType;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Cache DTO for reaction counts of a post.
 * Completely separate from viewer-specific data (myReaction).
 * Serializable for Redisson SerializationCodec.
 */
public record ReactionCountsCacheDTO(Map<ReactionType, Long> counts) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public ReactionCountsCacheDTO {
        EnumMap<ReactionType, Long> complete = new EnumMap<>(ReactionType.class);
        for (ReactionType type : ReactionType.values()) {
            complete.put(type, counts != null ? counts.getOrDefault(type, 0L) : 0L);
        }
        counts = Collections.unmodifiableMap(complete);
    }

    public static ReactionCountsCacheDTO empty() {
        EnumMap<ReactionType, Long> map = new EnumMap<>(ReactionType.class);
        for (ReactionType type : ReactionType.values()) {
            map.put(type, 0L);
        }
        return new ReactionCountsCacheDTO(map);
    }
}
