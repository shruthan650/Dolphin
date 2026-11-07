package com.dolphin.dto.publicprofile;

import java.util.List;

public record PublicStudentProjectResponse(
        String title,
        String description,
        String githubUrl,
        String liveUrl,
        List<String> technologies
) {
}
