package com.dolphin.dto.publicprofile;

import java.util.List;

public record PublicStudentDirectoryResponse(
        List<PublicStudentSummaryResponse> students,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
