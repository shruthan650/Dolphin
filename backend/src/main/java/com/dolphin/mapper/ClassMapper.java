package com.dolphin.mapper;

import com.dolphin.dto.classdto.ClassResponse;
import com.dolphin.dto.classdto.CreateClassRequest;
import com.dolphin.dto.classdto.StudentClassResponse;
import com.dolphin.dto.classdto.UpdateClassRequest;
import com.dolphin.model.ClassEntity;

import java.time.Instant;

public final class ClassMapper {

    private ClassMapper() {
    }

    public static ClassEntity toEntity(CreateClassRequest request, String teacherId, String classCode, Instant now) {
        ClassEntity entity = new ClassEntity();
        entity.setClassName(request.className().trim());
        entity.setSemester(request.semester());
        entity.setBranch(request.branch().trim());
        entity.setSection(request.section().trim());
        entity.setTeacherId(teacherId);
        entity.setClassCode(classCode);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    public static void applyUpdate(ClassEntity entity, UpdateClassRequest request, Instant now) {
        entity.setClassName(request.className().trim());
        entity.setSemester(request.semester());
        entity.setBranch(request.branch().trim());
        entity.setSection(request.section().trim());
        entity.setUpdatedAt(now);
    }

    public static ClassResponse toResponse(ClassEntity entity, String teacherName) {
        return new ClassResponse(entity.getId(), entity.getClassName(), entity.getSemester(), entity.getBranch(),
                entity.getSection(), entity.getClassCode(), entity.getTeacherId(), teacherName,
                entity.getStudentIds().size(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public static StudentClassResponse toStudentResponse(ClassEntity entity, String teacherName) {
        return new StudentClassResponse(entity.getId(), entity.getClassName(), entity.getSemester(),
                entity.getBranch(), entity.getSection(), entity.getClassCode(), teacherName,
                entity.getStudentIds().size());
    }
}
