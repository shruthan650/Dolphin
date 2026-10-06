package com.dolphin.repository;

import com.dolphin.model.ClassEntity;

import java.util.List;
import java.util.Optional;

public interface ClassRepository {

    Optional<ClassEntity> findById(String id);

    /** Class code lookup is case-insensitive. */
    Optional<ClassEntity> findByClassCode(String classCode);

    boolean existsByClassCode(String classCode);

    List<ClassEntity> findByTeacherId(String teacherId);

    List<ClassEntity> findByStudentId(String studentId);

    List<ClassEntity> findAll();

    ClassEntity save(ClassEntity classEntity);

    void deleteById(String id);

    long count();
}
