package com.dolphin.dto.project;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/** ownerName and className are only populated in teacher views; classId is null for unassigned projects. */
public record ProjectResponse(
        String id,
        String ownerId,
        @JsonInclude(JsonInclude.Include.NON_NULL) String ownerName,
        String classId,
        @JsonInclude(JsonInclude.Include.NON_NULL) String className,
        String title,
        String description,
        String githubUrl,
        String liveUrl,
        List<String> technologies,
        Instant createdAt,
        Instant updatedAt
) {
}
