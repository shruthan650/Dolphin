package com.dolphin.service;

import com.dolphin.dto.project.CreateProjectRequest;
import com.dolphin.dto.project.ProjectResponse;
import com.dolphin.dto.project.UpdateProjectRequest;
import com.dolphin.exception.ForbiddenException;
import com.dolphin.exception.ResourceNotFoundException;
import com.dolphin.mapper.ProjectMapper;
import com.dolphin.model.Project;
import com.dolphin.repository.AdviceRepository;
import com.dolphin.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final AdviceRepository adviceRepository;
    private final ClassService classService;

    public ProjectService(ProjectRepository projectRepository, AdviceRepository adviceRepository,
                          ClassService classService) {
        this.projectRepository = projectRepository;
        this.adviceRepository = adviceRepository;
        this.classService = classService;
    }

    /** ownerId always comes from the authenticated student; the class must be one the student has joined. */
    public ProjectResponse create(String studentId, CreateProjectRequest request) {
        classService.requireEnrolled(studentId, request.classId().trim());
        Project project = ProjectMapper.toEntity(request, studentId, Instant.now());
        return ProjectMapper.toResponse(projectRepository.save(project));
    }

    public List<ProjectResponse> listOwn(String studentId) {
        return projectRepository.findByOwnerId(studentId).stream().map(ProjectMapper::toResponse).toList();
    }

    public ProjectResponse getOwn(String studentId, String projectId) {
        return ProjectMapper.toResponse(requireOwned(studentId, projectId, "view"));
    }

    public ProjectResponse update(String studentId, String projectId, UpdateProjectRequest request) {
        Project project = requireOwned(studentId, projectId, "modify");
        if (request.classId() != null && !request.classId().isBlank()) {
            classService.requireEnrolled(studentId, request.classId().trim());
            project.setClassId(request.classId().trim());
        }
        ProjectMapper.applyUpdate(project, request, Instant.now());
        return ProjectMapper.toResponse(projectRepository.save(project));
    }

    public void delete(String studentId, String projectId) {
        requireOwned(studentId, projectId, "delete");
        adviceRepository.deleteByTargetId(projectId);
        projectRepository.deleteById(projectId);
    }

    private Project requireOwned(String studentId, String projectId, String action) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        if (!project.getOwnerId().equals(studentId)) {
            throw new ForbiddenException("You are not allowed to " + action + " this project");
        }
        return project;
    }
}
