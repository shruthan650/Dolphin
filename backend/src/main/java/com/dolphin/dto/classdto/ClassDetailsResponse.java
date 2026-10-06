package com.dolphin.dto.classdto;

import com.dolphin.dto.teacher.TeacherStudentResponse;

import java.util.List;

/** A teacher's view of one of their own classes, including enrolled students and their progress summary. */
public record ClassDetailsResponse(
        ClassResponse classInfo,
        List<TeacherStudentResponse> students
) {
}
