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
import com.dolphin.model.Role;
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
 * Teacher read access to students. A teacher only ever sees students enrolled in classes they own.
 */
@Service
public class TeacherService {

    private final ClassRepository classRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final LeetCodeRepository leetCodeRepository;
    private final StudentProgressAssembler progressAssembler;
    private final AccountDeletionService accountDeletionService;
    private final AdviceService adviceService;

    public TeacherService(ClassRepository classRepository, UserRepository userRepository,
                          ProjectRepository projectRepository, LeetCodeRepository leetCodeRepository,
                          StudentProgressAssembler progressAssembler, AccountDeletionService accountDeletionService,
                          AdviceService adviceService) {
        this.classRepository = classRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.leetCodeRepository = leetCodeRepository;
        this.progressAssembler = progressAssembler;
        this.accountDeletionService = accountDeletionService;
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
        List<LeetCodeEntry> entries = leetCodeRepository.findByStudentId(studentId);
        List<ProjectResponse> projects = projectRepository.findByOwnerId(studentId).stream()
                .map(ProjectMapper::toResponse)
                .toList();
        return new TeacherStudentDetailResponse(student.getId(), student.getName(), student.getEmail(),
                student.getGithubUrl(), student.getLeetCodeUrl(), student.isActive(), student.getCreatedAt(), classes, LeetCodeMapper.toStats(entries), projects,
                entries.stream().map(LeetCodeMapper::toResponse).toList(), adviceService.forStudent(studentId));
    }

    public List<ProjectResponse> getStudentProjects(String teacherId, String studentId) {
        requireStudentAccess(teacherId, studentId);
        return projectRepository.findByOwnerId(studentId).stream().map(ProjectMapper::toResponse).toList();
    }

    public List<LeetCodeResponse> getStudentLeetCode(String teacherId, String studentId) {
        requireStudentAccess(teacherId, studentId);
        return leetCodeRepository.findByStudentId(studentId).stream().map(LeetCodeMapper::toResponse).toList();
    }

    /**
     * Permanently deletes a student enrolled in one of this teacher's classes, including their projects,
     * LeetCode entries and enrolments in every class (also other teachers' classes).
     */
    public void deleteStudent(String teacherId, String studentId) {
        requireStudentAccess(teacherId, studentId);
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        if (student.getRole() != Role.STUDENT) {
            throw new ForbiddenException("Only student accounts can be deleted");
        }
        accountDeletionService.deleteStudent(studentId);
    }

    /** All projects from students across the teacher's classes. */
    public List<ProjectResponse> getAllStudentProjects(String teacherId) {
        Set<String> studentIds = studentIdsOf(classRepository.findByTeacherId(teacherId));
        Map<String, String> names = progressAssembler.namesById(studentIds);
        return projectRepository.findByOwnerIdIn(studentIds).stream()
                .map(p -> ProjectMapper.toResponse(p, names.get(p.getOwnerId())))
                .toList();
    }

    /** All LeetCode entries from students across the teacher's classes. */
    public List<LeetCodeResponse> getAllStudentLeetCode(String teacherId) {
        Set<String> studentIds = studentIdsOf(classRepository.findByTeacherId(teacherId));
        Map<String, String> names = progressAssembler.namesById(studentIds);
        return leetCodeRepository.findByStudentIdIn(studentIds).stream()
                .map(e -> LeetCodeMapper.toResponse(e, names.get(e.getStudentId())))
                .toList();
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
