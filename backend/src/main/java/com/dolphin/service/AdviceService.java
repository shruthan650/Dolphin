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
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Teacher advice on a student's projects and LeetCode entries. A teacher may advise only students enrolled in one
 * of their classes and may delete only advice they wrote; students read the advice given to them.
 */
@Service
public class AdviceService {

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
        String title;
        if (request.targetType() == AdviceTarget.PROJECT) {
            Project project = projectRepository.findById(request.targetId())
                    .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
            studentId = project.getOwnerId();
            title = project.getTitle();
        } else {
            LeetCodeEntry entry = leetCodeRepository.findById(request.targetId())
                    .orElseThrow(() -> new ResourceNotFoundException("LeetCode entry not found"));
            studentId = entry.getStudentId();
            title = entry.getProblemName();
        }
        boolean teachesStudent = classRepository.findByTeacherId(teacherId).stream()
                .anyMatch(c -> c.getStudentIds().contains(studentId));
        if (!teachesStudent) {
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
        return toResponse(saved, title, progressAssembler.namesById(List.of(teacherId)).get(teacherId));
    }

    public void delete(String teacherId, String adviceId) {
        Advice advice = adviceRepository.findById(adviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Advice not found"));
        if (!advice.getTeacherId().equals(teacherId)) {
            throw new ForbiddenException("You can only delete advice you wrote");
        }
        adviceRepository.deleteById(adviceId);
    }

    /** All advice given to a student, newest first, with project/problem titles and teacher names. */
    public List<AdviceResponse> forStudent(String studentId) {
        List<Advice> items = adviceRepository.findByStudentId(studentId);
        if (items.isEmpty()) {
            return List.of();
        }
        Map<String, String> titles = new HashMap<>();
        projectRepository.findByOwnerId(studentId).forEach(p -> titles.put(p.getId(), p.getTitle()));
        leetCodeRepository.findByStudentId(studentId).forEach(e -> titles.put(e.getId(), e.getProblemName()));
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
