package com.dolphin.service;

import com.dolphin.repository.AdviceRepository;
import com.dolphin.repository.LeetCodeRepository;
import com.dolphin.repository.ProjectRepository;
import com.dolphin.repository.UserRepository;
import org.springframework.stereotype.Service;

/**
 * Permanently deletes accounts together with the data that belongs to them. Callers do the authorization checks.
 * Dependent data is removed first and the user last, so a failure part-way can simply be retried.
 */
@Service
public class AccountDeletionService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final LeetCodeRepository leetCodeRepository;
    private final ClassService classService;
    private final AdviceRepository adviceRepository;

    public AccountDeletionService(UserRepository userRepository, ProjectRepository projectRepository,
                                  LeetCodeRepository leetCodeRepository, ClassService classService,
                                  AdviceRepository adviceRepository) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.leetCodeRepository = leetCodeRepository;
        this.classService = classService;
        this.adviceRepository = adviceRepository;
    }

    /**
     * Deletes the teacher, their classes and the advice they wrote. Students of those classes keep their
     * accounts and data.
     */
    void deleteTeacher(String teacherId) {
        adviceRepository.deleteByTeacherId(teacherId);
        classService.deleteClassesOfTeacher(teacherId);
        userRepository.deleteById(teacherId);
    }

    /** Deletes the student, their projects, LeetCode entries, advice received and enrolment in every class. */
    void deleteStudent(String studentId) {
        adviceRepository.deleteByStudentId(studentId);
        projectRepository.deleteByOwnerId(studentId);
        leetCodeRepository.deleteByStudentId(studentId);
        classService.removeStudentFromAllClasses(studentId);
        userRepository.deleteById(studentId);
    }
}
