package com.dolphin.service;

import com.dolphin.dto.advice.AdviceResponse;
import com.dolphin.dto.advice.CreateAdviceRequest;
import com.dolphin.exception.ForbiddenException;
import com.dolphin.exception.ResourceNotFoundException;
import com.dolphin.model.Advice;
import com.dolphin.model.AdviceTarget;
import com.dolphin.model.LeetCodeEntry;
import com.dolphin.model.Project;
import com.dolphin.repository.AdviceRepository;
import com.dolphin.repository.ClassRepository;
import com.dolphin.repository.LeetCodeRepository;
import com.dolphin.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Teacher advice on a student's projects and LeetCode entries. A teacher may
 * advise only students enrolled in one
 * of their classes and may delete only advice they wrote; students read the
 * advice given to them.
 */
@Service
public class AdviceService {

    private static final Logger log = LoggerFactory.getLogger(AdviceService.class);
    private final AdviceRepository adviceRepository;
    private final ProjectRepository projectRepository;
    private final LeetCodeRepository leetCodeRepository;
    private final ClassRepository classRepository;
    private final StudentProgressAssembler progressAssembler;

    public AdviceService(AdviceRepository adviceRepository, ProjectRepository projectRepository,
            LeetCodeRepository leetCodeRepository, ClassRepository classRepository,
            StudentProgressAssembler progressAssembler) {
        this.adviceRepository = adviceRepository;
        this.projectRepository = projectRepository;
        this.leetCodeRepository = leetCodeRepository;
        this.classRepository = classRepository;
        this.progressAssembler = progressAssembler;
    }

    public AdviceResponse give(String teacherId, CreateAdviceRequest request) {
        String studentId;
        String classId;
        String title;
        if (request.targetType() == AdviceTarget.PROJECT) {
            Project project = projectRepository.findById(request.targetId())
                    .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
            studentId = project.getOwnerId();
            classId = project.getClassId();
            title = project.getTitle();
        } else {
            LeetCodeEntry entry = leetCodeRepository.findById(request.targetId())
                    .orElseThrow(() -> new ResourceNotFoundException("LeetCode entry not found"));
            studentId = entry.getStudentId();
            classId = entry.getClassId();
            title = entry.getProblemName();
        }
        if (!TeacherScope.of(classRepository.findByTeacherId(teacherId)).canSee(studentId, classId)) {
            throw new ForbiddenException("You can only advise students enrolled in your classes");
        }

        Instant now = Instant.now();
        Advice advice = new Advice();
        advice.setTeacherId(teacherId);
        advice.setStudentId(studentId);
        advice.setTargetType(request.targetType());
        advice.setTargetId(request.targetId());
        advice.setMessage(request.message().trim());
        advice.setCreatedAt(now);
        advice.setUpdatedAt(now);
        Advice saved = adviceRepository.save(advice);
        log.info("Teacher {} added advice {} for student {} on {} {}", teacherId, saved.getId(), studentId,
                request.targetType(), request.targetId());
        return toResponse(saved, title, progressAssembler.namesById(List.of(teacherId)).get(teacherId));
    }

    public void delete(String teacherId, String adviceId) {
        Advice advice = adviceRepository.findById(adviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Advice not found"));
        if (!advice.getTeacherId().equals(teacherId)) {
            throw new ForbiddenException("You can only delete advice you wrote");
        }
        adviceRepository.deleteById(adviceId);
        log.info("Teacher {} deleted advice {}", teacherId, adviceId);
    }

    /**
     * All advice given to a student, newest first, with project/problem titles and
     * teacher names.
     */
    public List<AdviceResponse> forStudent(String studentId) {
        return forStudent(studentId, null);
    }

    /**
     * Advice on the student's projects and entries that are visible through the
     * given classes of a teacher.
     */
    List<AdviceResponse> forStudent(String studentId, TeacherScope scope) {
        Map<String, String> titles = new HashMap<>();
        projectRepository.findByOwnerId(studentId).stream()
                .filter(p -> scope == null || scope.canSee(studentId, p.getClassId()))
                .forEach(p -> titles.put(p.getId(), p.getTitle()));
        leetCodeRepository.findByStudentId(studentId).stream()
                .filter(e -> scope == null || scope.canSee(studentId, e.getClassId()))
                .forEach(e -> titles.put(e.getId(), e.getProblemName()));
        Set<String> visibleTargets = new HashSet<>(titles.keySet());
        List<Advice> items = adviceRepository.findByStudentId(studentId).stream()
                .filter(a -> scope == null || visibleTargets.contains(a.getTargetId()))
                .toList();
        if (items.isEmpty()) {
            return List.of();
        }
        Map<String, String> teacherNames = progressAssembler.namesById(
                items.stream().map(Advice::getTeacherId).distinct().toList());
        return items.stream()
                .map(a -> toResponse(a, titles.get(a.getTargetId()), teacherNames.get(a.getTeacherId())))
                .toList();
    }

    private static AdviceResponse toResponse(Advice a, String targetTitle, String teacherName) {
        return new AdviceResponse(a.getId(), a.getTargetType(), a.getTargetId(), targetTitle, a.getStudentId(),
                a.getTeacherId(), teacherName, a.getMessage(), a.getCreatedAt());
    }
}
