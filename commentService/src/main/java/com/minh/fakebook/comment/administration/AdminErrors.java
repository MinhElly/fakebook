package com.minh.fakebook.comment.administration;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
@org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages="com.minh.fakebook.comment")
public class AdminErrors {
    @ExceptionHandler({org.springframework.dao.OptimisticLockingFailureException.class,org.springframework.dao.DuplicateKeyException.class})
    public ResponseEntity<?> conflict(Exception ignored) { return ResponseEntity.status(409).body(java.util.Map.of("status",409,"code","CONCURRENT_CHANGE")); }
}
