package com.dolphin.service;

import com.dolphin.dto.teacher.TeacherStudentResponse;
import com.dolphin.model.ClassEntity;
import com.dolphin.model.LeetCodeEntry;
import com.dolphin.model.LeetCodeStatus;
import com.dolphin.model.Project;
import com.dolphin.model.User;
import com.dolphin.repository.LeetCodeRepository;
import com.dolphin.repository.ProjectRepository;
import com.dolphin.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Builds per-student progress summaries for teacher-facing views. */
@Component
public class StudentProgressAssembler {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final LeetCodeRepository leetCodeRepository;

    public StudentProgressAssembler(UserRepository userRepository, ProjectRepository projectRepository,
                                    LeetCodeRepository leetCodeRepository) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.leetCodeRepository = leetCodeRepository;
    }

    /**
     * @param studentIds students to summarise
     * @param classes    the teacher's classes, used to list which of them each student belongs to
     */
    public List<TeacherStudentResponse> summarize(Collection<String> studentIds, List<ClassEntity> classes) {
        if (studentIds.isEmpty()) {
            return List.of();
        }
        Map<String, Long> projectCounts = projectRepository.findByOwnerIdIn(studentIds).stream()
                .collect(Collectors.groupingBy(Project::getOwnerId, Collectors.counting()));
        Map<String, List<LeetCodeEntry>> entries = leetCodeRepository.findByStudentIdIn(studentIds).stream()
                .collect(Collectors.groupingBy(LeetCodeEntry::getStudentId));

        return userRepository.findAllById(studentIds).stream()
                .sorted(Comparator.comparing(User::getName, String.CASE_INSENSITIVE_ORDER))
                .map(student -> {
                    List<LeetCodeEntry> own = entries.getOrDefault(student.getId(), List.of());
                    long solved = own.stream().filter(e -> e.getStatus() == LeetCodeStatus.SOLVED).count();
                    List<String> classNames = classes.stream()
                            .filter(c -> c.getStudentIds().contains(student.getId()))
                            .map(ClassEntity::getClassName)
                            .toList();
                    return new TeacherStudentResponse(student.getId(), student.getName(), student.getEmail(),
                            student.isActive(), classNames, projectCounts.getOrDefault(student.getId(), 0L),
                            own.size(), solved);
                })
                .toList();
    }

    public Map<String, String> namesById(Collection<String> userIds) {
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName, (a, b) -> a));
    }
}
