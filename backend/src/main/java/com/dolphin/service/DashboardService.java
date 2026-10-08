package com.dolphin.service;

import com.dolphin.dto.admin.AdminDashboardResponse;
import com.dolphin.dto.admin.AdminUserResponse;
import com.dolphin.dto.common.ActivityResponse;
import com.dolphin.dto.leetcode.LeetCodeResponse;
import com.dolphin.dto.project.ProjectResponse;
import com.dolphin.dto.student.StudentDashboardResponse;
import com.dolphin.dto.teacher.TeacherDashboardResponse;
import com.dolphin.mapper.LeetCodeMapper;
import com.dolphin.mapper.ProjectMapper;
import com.dolphin.model.ClassEntity;
import com.dolphin.model.LeetCodeEntry;
import com.dolphin.model.LeetCodeStatus;
import com.dolphin.model.Project;
import com.dolphin.model.Role;
import com.dolphin.model.User;
import com.dolphin.repository.ClassRepository;
import com.dolphin.repository.LeetCodeRepository;
import com.dolphin.repository.ProjectRepository;
import com.dolphin.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/** All dashboard numbers are computed from the repositories on every request; nothing is hardcoded. */
@Service
public class DashboardService {

    private static final int RECENT_LIMIT = 5;
    private static final int ACTIVITY_LIMIT = 10;

    private final UserRepository userRepository;
    private final ClassRepository classRepository;
    private final ProjectRepository projectRepository;
    private final LeetCodeRepository leetCodeRepository;
    private final AdminService adminService;
    private final ClassService classService;
    private final StudentProgressAssembler progressAssembler;

    public DashboardService(UserRepository userRepository, ClassRepository classRepository,
                            ProjectRepository projectRepository, LeetCodeRepository leetCodeRepository,
                            AdminService adminService, ClassService classService,
                            StudentProgressAssembler progressAssembler) {
        this.userRepository = userRepository;
        this.classRepository = classRepository;
        this.projectRepository = projectRepository;
        this.leetCodeRepository = leetCodeRepository;
        this.adminService = adminService;
        this.classService = classService;
        this.progressAssembler = progressAssembler;
    }

    public AdminDashboardResponse adminDashboard() {
        List<User> teachers = userRepository.findByRole(Role.TEACHER);
        long solved = leetCodeRepository.findAll().stream()
                .filter(e -> e.getStatus() == LeetCodeStatus.SOLVED)
                .count();
        List<User> recentUsers = userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getCreatedAt).reversed())
                .limit(RECENT_LIMIT)
                .toList();
        List<AdminUserResponse> recent = adminService.toAdminResponses(recentUsers);
        return new AdminDashboardResponse(
                userRepository.count(),
                teachers.size(),
                teachers.stream().filter(User::isActive).count(),
                userRepository.countByRole(Role.STUDENT),
                classRepository.count(),
                projectRepository.count(),
                leetCodeRepository.count(),
                solved,
                recent);
    }

    public TeacherDashboardResponse teacherDashboard(String teacherId) {
        List<ClassEntity> classes = classRepository.findByTeacherId(teacherId);
        Set<String> studentIds = TeacherService.studentIdsOf(classes);
        TeacherScope scope = TeacherScope.of(classes);
        List<Project> projects = projectRepository.findByOwnerIdIn(studentIds).stream()
                .filter(p -> scope.canSee(p.getOwnerId(), p.getClassId()))
                .toList();
        List<LeetCodeEntry> entries = leetCodeRepository.findByStudentIdIn(studentIds).stream()
                .filter(e -> scope.canSee(e.getStudentId(), e.getClassId()))
                .toList();
        long solved = entries.stream().filter(e -> e.getStatus() == LeetCodeStatus.SOLVED).count();
        return new TeacherDashboardResponse(classes.size(), studentIds.size(), projects.size(), entries.size(),
                solved, recentActivity(projects, entries, progressAssembler.namesById(studentIds)));
    }

    public StudentDashboardResponse studentDashboard(String studentId) {
        List<Project> projects = projectRepository.findByOwnerId(studentId);
        List<LeetCodeEntry> entries = leetCodeRepository.findByStudentId(studentId);
        List<ProjectResponse> recentProjects = projects.stream()
                .limit(RECENT_LIMIT).map(ProjectMapper::toResponse).toList();
        List<LeetCodeResponse> recentLeetCode = entries.stream()
                .sorted(Comparator.comparing(LeetCodeEntry::getUpdatedAt).reversed())
                .limit(RECENT_LIMIT).map(LeetCodeMapper::toResponse).toList();
        return new StudentDashboardResponse(classService.listStudentClasses(studentId), projects.size(),
                LeetCodeMapper.toStats(entries), recentProjects, recentLeetCode);
    }

    private List<ActivityResponse> recentActivity(List<Project> projects, List<LeetCodeEntry> entries,
                                                  Map<String, String> names) {
        List<ActivityResponse> items = new ArrayList<>();
        projects.forEach(p -> items.add(new ActivityResponse("PROJECT", p.getId(), p.getOwnerId(),
                names.get(p.getOwnerId()), p.getTitle(),
                p.getUpdatedAt().isAfter(p.getCreatedAt()) ? "Updated a project" : "Added a project",
                p.getUpdatedAt())));
        entries.forEach(e -> items.add(new ActivityResponse("LEETCODE", e.getId(), e.getStudentId(),
                names.get(e.getStudentId()), e.getProblemName(),
                Stream.of(e.getDifficulty().name(), e.getStatus().name().replace('_', ' '))
                        .reduce((a, b) -> a + " · " + b).orElse(""),
                e.getUpdatedAt())));
        return items.stream()
                .sorted(Comparator.comparing(ActivityResponse::timestamp).reversed())
                .limit(ACTIVITY_LIMIT)
                .toList();
    }
}
