package com.dolphin.repository;

import com.dolphin.model.Advice;

import java.util.List;
import java.util.Optional;

public interface AdviceRepository {

    Optional<Advice> findById(String id);

    /** Newest first. */
    List<Advice> findByStudentId(String studentId);

    Advice save(Advice advice);

    void deleteById(String id);

    /** Removes advice about a project or LeetCode entry that is being deleted. */
    void deleteByTargetId(String targetId);

    void deleteByStudentId(String studentId);

    void deleteByTeacherId(String teacherId);
}
