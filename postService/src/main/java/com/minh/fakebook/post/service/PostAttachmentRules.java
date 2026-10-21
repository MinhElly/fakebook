package com.minh.fakebook.post.service;

import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class PostAttachmentRules {
    private PostAttachmentRules() {}
    public static void validate(List<UUID> ids) {
        if (ids != null && (ids.size() > 30 || ids.stream().anyMatch(Objects::isNull) || new HashSet<>(ids).size() != ids.size())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At most 30 distinct media attachments are allowed");
        }
    }
}
