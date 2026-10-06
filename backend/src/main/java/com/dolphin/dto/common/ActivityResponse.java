package com.dolphin.dto.common;

import java.time.Instant;

/**
 * One item in a "recent activity" feed.
 *
 * @param type PROJECT or LEETCODE
 */
public record ActivityResponse(
        String type,
        String id,
        String studentId,
        String studentName,
        String title,
        String detail,
        Instant timestamp
) {
}
