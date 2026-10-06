package com.dolphin.repository;

import com.dolphin.model.LeetCodeEntry;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LeetCodeRepository {

    Optional<LeetCodeEntry> findById(String id);

    List<LeetCodeEntry> findByStudentId(String studentId);

    List<LeetCodeEntry> findByStudentIdIn(Collection<String> studentIds);

    List<LeetCodeEntry> findAll();

    LeetCodeEntry save(LeetCodeEntry entry);

    void deleteById(String id);

    void deleteByStudentId(String studentId);

    long count();
}
