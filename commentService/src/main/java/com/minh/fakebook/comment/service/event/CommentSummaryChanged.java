package com.minh.fakebook.comment.service.event;

import java.util.UUID;

public record CommentSummaryChanged(UUID postId) {}
