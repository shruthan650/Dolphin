package com.dolphin.service;

import com.dolphin.dto.leetcode.LeetCodeStatsResponse;
import com.dolphin.dto.leetcode.LeetCodeStreakResponse;
import com.dolphin.dto.publicprofile.PublicLeetCodeStatsResponse;
import com.dolphin.dto.publicprofile.PublicProfileVisibilityResponse;
import com.dolphin.dto.publicprofile.PublicStudentDirectoryResponse;
import com.dolphin.dto.publicprofile.PublicStudentProfileResponse;
import com.dolphin.dto.publicprofile.PublicStudentProjectResponse;
import com.dolphin.dto.publicprofile.PublicStudentSummaryResponse;
import com.dolphin.dto.publicprofile.UpdatePublicProfileVisibilityRequest;
import com.dolphin.exception.BadRequestException;
import com.dolphin.exception.ForbiddenException;
import com.dolphin.exception.ResourceNotFoundException;
import com.dolphin.mapper.LeetCodeMapper;
import com.dolphin.model.LeetCodeEntry;
import com.dolphin.model.Project;
import com.dolphin.model.Role;
import com.dolphin.repository.LeetCodeRepository;
import com.dolphin.repository.ProjectRepository;
import com.dolphin.repository.UserRepository;
import com.dolphin.repository.UserRepository.PublicStudentCandidate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PublicStudentProfileService {

    private static final int DEFAULT_PAGE_SIZE = 12;
    private static final int MAX_PAGE_SIZE = 48;
    private static final int MAX_PAGE = 100_000;
    private static final int MAX_SEARCH_LENGTH = 80;
    private static final Pattern EMAIL_ADDRESS =
            Pattern.compile("(?i)[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}");
    private static final Pattern PUBLIC_PROFILE_ID =
            Pattern.compile("(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    private static final Pattern SENSITIVE_ASSIGNMENT = Pattern.compile(
            "(?i)\\b(password|passphrase|secret|api[_ -]?key|access[_ -]?token|refresh[_ -]?token|jwt)"
                    + "(\\s*[:=]\\s*)[^\\s,;]+");
    private static final Pattern JWT_VALUE =
            Pattern.compile("\\beyJ[a-zA-Z0-9_-]{10,}\\.[a-zA-Z0-9_-]{10,}\\.[a-zA-Z0-9_-]{10,}\\b");

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final LeetCodeRepository leetCodeRepository;
    private final LeetCodeStreakService streakService;

    public PublicStudentProfileService(UserRepository userRepository, ProjectRepository projectRepository,
                                       LeetCodeRepository leetCodeRepository, LeetCodeStreakService streakService) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.leetCodeRepository = leetCodeRepository;
        this.streakService = streakService;
    }

    public PublicStudentDirectoryResponse directory(String viewerId, String search, int page, int size) {
        String term = search == null ? "" : search.trim();
        if (term.length() > MAX_SEARCH_LENGTH) {
            throw new BadRequestException("Search must be at most 80 characters");
        }
        if (page < 0 || page > MAX_PAGE) {
            throw new BadRequestException("Page must be between 0 and 100000");
        }
        int pageSize = size == 0 ? DEFAULT_PAGE_SIZE : size;
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new BadRequestException("Page size must be between 1 and 48");
        }

        int offset = Math.multiplyExact(page, pageSize);
        List<PublicStudentCandidate> students =
                userRepository.findPublicStudents(term, viewerId, pageSize, offset);
        long total = userRepository.countPublicStudents(term, viewerId);
        List<String> ids = students.stream().map(PublicStudentCandidate::id).toList();
        Map<String, List<LeetCodeEntry>> entriesByStudent = leetCodeRepository.findByStudentIdIn(ids).stream()
                .collect(Collectors.groupingBy(LeetCodeEntry::getStudentId));
        Map<String, Long> projectsByStudent = projectRepository.findPublicByOwnerIdIn(ids).stream()
                .collect(Collectors.groupingBy(Project::getOwnerId, Collectors.counting()));
        List<PublicStudentSummaryResponse> summaries = students.stream().map(student -> {
            List<LeetCodeEntry> entries = entriesByStudent.getOrDefault(student.id(), List.of());
            return new PublicStudentSummaryResponse(student.profileId(), publicDisplayName(student.name()),
                    LeetCodeMapper.toStats(entries).solved(),
                    streakService.calculate(entries).currentStreak(),
                    projectsByStudent.getOrDefault(student.id(), 0L));
        }).toList();
        return new PublicStudentDirectoryResponse(summaries, page, pageSize, total,
                total == 0 ? 0 : (int) Math.ceil((double) total / pageSize));
    }

    public PublicStudentProfileResponse profile(String viewerId, String profileId) {
        if (profileId == null || !PUBLIC_PROFILE_ID.matcher(profileId).matches()) {
            throw new ResourceNotFoundException("Public student profile not found");
        }
        PublicStudentCandidate student = userRepository.findPublicStudentByProfileId(profileId)
                .filter(candidate -> !candidate.id().equals(viewerId))
                .orElseThrow(() -> new ResourceNotFoundException("Public student profile not found"));
        List<LeetCodeEntry> entries = leetCodeRepository.findByStudentId(student.id());
        List<Project> projects = projectRepository.findPublicByOwnerIdIn(List.of(student.id()));
        LeetCodeStatsResponse stats = LeetCodeMapper.toStats(entries);
        LeetCodeStreakResponse streak = streakService.calculate(entries);
        return new PublicStudentProfileResponse(student.profileId(), publicDisplayName(student.name()), student.joinedAt(),
                new PublicLeetCodeStatsResponse(stats.solved(), stats.easySolved(), stats.mediumSolved(),
                        stats.hardSolved(),
                        streak.currentStreak(), streak.longestStreak(), streak.totalActiveDays(),
                        streak.mostRecentSolvedDate(), streak.recentActivity()),
                projects.stream().map(this::toPublicProject).toList());
    }

    public PublicProfileVisibilityResponse getVisibility(String userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != Role.STUDENT) {
            throw new ForbiddenException("Only students can manage a public profile");
        }
        return new PublicProfileVisibilityResponse(user.getPublicProfileId() != null);
    }

    public PublicProfileVisibilityResponse updateVisibility(String userId,
                                                             UpdatePublicProfileVisibilityRequest request) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != Role.STUDENT) {
            throw new ForbiddenException("Only students can manage a public profile");
        }
        if (request.isPublic() && user.getPublicProfileId() == null) {
            user.setPublicProfileId(UUID.randomUUID().toString());
        } else if (!request.isPublic()) {
            user.setPublicProfileId(null);
        }
        userRepository.save(user);
        return new PublicProfileVisibilityResponse(user.getPublicProfileId() != null);
    }

    private PublicStudentProjectResponse toPublicProject(Project project) {
        return new PublicStudentProjectResponse(publicText(project.getTitle()), publicText(project.getDescription()),
                safePublicUrl(project.getGithubUrl()), safePublicUrl(project.getLiveUrl()),
                project.getTechnologies().stream().map(PublicStudentProfileService::publicText).toList());
    }

    private static String publicDisplayName(String name) {
        String displayName = publicText(name);
        return displayName.isBlank() ? "Student" : displayName;
    }

    private static String publicText(String value) {
        if (value == null) {
            return null;
        }
        String withoutEmail = EMAIL_ADDRESS.matcher(value).replaceAll("");
        String withoutAssignments = SENSITIVE_ASSIGNMENT.matcher(withoutEmail).replaceAll("$1$2[redacted]");
        return JWT_VALUE.matcher(withoutAssignments).replaceAll("[redacted]").trim();
    }

    private static String safePublicUrl(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            if (("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && uri.getHost() != null && uri.getUserInfo() == null
                    && uri.getRawQuery() == null && uri.getRawFragment() == null) {
                return uri.toASCIIString();
            }
        } catch (URISyntaxException ignored) {
            return null;
        }
        return null;
    }
}
