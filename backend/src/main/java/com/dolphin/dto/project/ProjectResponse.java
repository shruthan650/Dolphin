package com.dolphin.dto.project;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/** ownerName is only populated in teacher views. */
public record ProjectResponse(
        String id,
        String ownerId,
        @JsonInclude(JsonInclude.Include.NON_NULL) String ownerName,
        String title,
        String description,
        String githubUrl,
        String liveUrl,
        List<String> technologies,
        Instant createdAt,
        Instant updatedAt
) {
}
