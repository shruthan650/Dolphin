package com.dolphin.service;

import com.dolphin.dto.classdto.ClassResponse;
import com.dolphin.dto.leetcode.LeetCodeResponse;
import com.dolphin.dto.project.ProjectResponse;
import com.dolphin.dto.teacher.TeacherStudentDetailResponse;
import com.dolphin.dto.teacher.TeacherStudentResponse;
import com.dolphin.exception.ForbiddenException;
import com.dolphin.exception.ResourceNotFoundException;
import com.dolphin.mapper.ClassMapper;
import com.dolphin.mapper.LeetCodeMapper;
import com.dolphin.mapper.ProjectMapper;
import com.dolphin.model.ClassEntity;
import com.dolphin.model.LeetCodeEntry;
import com.dolphin.model.Project;
import com.dolphin.model.User;
import com.dolphin.repository.ClassRepository;
import com.dolphin.repository.LeetCodeRepository;
import com.dolphin.repository.ProjectRepository;
import com.dolphin.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Teacher read access to students. A teacher only ever sees students enrolled in classes they own, and only their
 * projects and LeetCode entries of those classes (plus unassigned ones; see {@link TeacherScope}).
 */
@Service
public class TeacherService {

    private final ClassRepository classRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final LeetCodeRepository leetCodeRepository;
    private final StudentProgressAssembler progressAssembler;
    private final AdviceService adviceService;

    public TeacherService(ClassRepository classRepository, UserRepository userRepository,
                          ProjectRepository projectRepository, LeetCodeRepository leetCodeRepository,
                          StudentProgressAssembler progressAssembler, AdviceService adviceService) {
        this.classRepository = classRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.leetCodeRepository = leetCodeRepository;
        this.progressAssembler = progressAssembler;
        this.adviceService = adviceService;
    }

    public List<TeacherStudentResponse> listStudents(String teacherId) {
        List<ClassEntity> classes = classRepository.findByTeacherId(teacherId);
        return progressAssembler.summarize(studentIdsOf(classes), classes);
    }

    public TeacherStudentDetailResponse getStudent(String teacherId, String studentId) {
        List<ClassEntity> sharedClasses = requireStudentAccess(teacherId, studentId);
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        String teacherName = userRepository.findById(teacherId).map(User::getName).orElse(null);
        List<ClassResponse> classes = sharedClasses.stream()
                .map(c -> ClassMapper.toResponse(c, teacherName))
                .toList();
        TeacherScope scope = TeacherScope.of(sharedClasses);
        List<LeetCodeEntry> entries = visibleEntries(scope, leetCodeRepository.findByStudentId(studentId));
        List<ProjectResponse> projects = visibleProjects(scope, projectRepository.findByOwnerId(studentId)).stream()
                .map(p -> ProjectMapper.toResponse(p, null, scope.className(p.getClassId())))
                .toList();
        return new TeacherStudentDetailResponse(student.getId(), student.getName(), student.getEmail(),
                student.getGithubUrl(), student.getLeetCodeUrl(), student.isActive(), student.getCreatedAt(), classes,
                LeetCodeMapper.toStats(entries), projects,
                entries.stream().map(e -> LeetCodeMapper.toResponse(e, null, scope.className(e.getClassId()))).toList(),
                adviceService.forStudent(studentId, scope));
    }

    public List<ProjectResponse> getStudentProjects(String teacherId, String studentId) {
        TeacherScope scope = TeacherScope.of(requireStudentAccess(teacherId, studentId));
        return visibleProjects(scope, projectRepository.findByOwnerId(studentId)).stream()
                .map(p -> ProjectMapper.toResponse(p, null, scope.className(p.getClassId())))
                .toList();
    }

    public List<LeetCodeResponse> getStudentLeetCode(String teacherId, String studentId) {
        TeacherScope scope = TeacherScope.of(requireStudentAccess(teacherId, studentId));
        return visibleEntries(scope, leetCodeRepository.findByStudentId(studentId)).stream()
                .map(e -> LeetCodeMapper.toResponse(e, null, scope.className(e.getClassId())))
                .toList();
    }

    /** Projects from students across the teacher's classes (only those of the teacher's classes, or unassigned). */
    public List<ProjectResponse> getAllStudentProjects(String teacherId) {
        List<ClassEntity> classes = classRepository.findByTeacherId(teacherId);
        TeacherScope scope = TeacherScope.of(classes);
        Set<String> studentIds = studentIdsOf(classes);
        Map<String, String> names = progressAssembler.namesById(studentIds);
        return visibleProjects(scope, projectRepository.findByOwnerIdIn(studentIds)).stream()
                .map(p -> ProjectMapper.toResponse(p, names.get(p.getOwnerId()), scope.className(p.getClassId())))
                .toList();
    }

    /** LeetCode entries from students across the teacher's classes (same visibility rule as projects). */
    public List<LeetCodeResponse> getAllStudentLeetCode(String teacherId) {
        List<ClassEntity> classes = classRepository.findByTeacherId(teacherId);
        TeacherScope scope = TeacherScope.of(classes);
        Set<String> studentIds = studentIdsOf(classes);
        Map<String, String> names = progressAssembler.namesById(studentIds);
        return visibleEntries(scope, leetCodeRepository.findByStudentIdIn(studentIds)).stream()
                .map(e -> LeetCodeMapper.toResponse(e, names.get(e.getStudentId()), scope.className(e.getClassId())))
                .toList();
    }

    private static List<Project> visibleProjects(TeacherScope scope, List<Project> projects) {
        return projects.stream().filter(p -> scope.canSee(p.getOwnerId(), p.getClassId())).toList();
    }

    private static List<LeetCodeEntry> visibleEntries(TeacherScope scope, List<LeetCodeEntry> entries) {
        return entries.stream().filter(e -> scope.canSee(e.getStudentId(), e.getClassId())).toList();
    }

    static Set<String> studentIdsOf(List<ClassEntity> classes) {
        Set<String> ids = new LinkedHashSet<>();
        classes.forEach(c -> ids.addAll(c.getStudentIds()));
        return ids;
    }

    /**
     * Ownership check: the student must be enrolled in at least one class owned by this teacher.
     *
     * @return the teacher's classes that the student belongs to
     */
    private List<ClassEntity> requireStudentAccess(String teacherId, String studentId) {
        List<ClassEntity> shared = classRepository.findByTeacherId(teacherId).stream()
                .filter(c -> c.getStudentIds().contains(studentId))
                .toList();
        if (shared.isEmpty()) {
            throw new ForbiddenException("This student is not enrolled in any of your classes");
        }
        return shared;
    }
}
