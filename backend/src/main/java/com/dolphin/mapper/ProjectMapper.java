package com.dolphin.mapper;

import com.dolphin.dto.project.CreateProjectRequest;
import com.dolphin.dto.project.ProjectResponse;
import com.dolphin.dto.project.UpdateProjectRequest;
import com.dolphin.model.Project;

import java.time.Instant;

import static com.dolphin.mapper.MapperUtils.cleanList;
import static com.dolphin.mapper.MapperUtils.trimToNull;

public final class ProjectMapper {

    private ProjectMapper() {
    }

    public static Project toEntity(CreateProjectRequest request, String ownerId, Instant now) {
        Project project = new Project();
        project.setOwnerId(ownerId);
        project.setClassId(request.classId().trim());
        project.setTitle(request.title().trim());
        project.setDescription(trimToNull(request.description()));
        project.setGithubUrl(trimToNull(request.githubUrl()));
        project.setLiveUrl(trimToNull(request.liveUrl()));
        project.setTechnologies(cleanList(request.technologies()));
        project.setCreatedAt(now);
        project.setUpdatedAt(now);
        return project;
    }

    public static void applyUpdate(Project project, UpdateProjectRequest request, Instant now) {
        project.setTitle(request.title().trim());
        project.setDescription(trimToNull(request.description()));
        project.setGithubUrl(trimToNull(request.githubUrl()));
        project.setLiveUrl(trimToNull(request.liveUrl()));
        project.setTechnologies(cleanList(request.technologies()));
        project.setUpdatedAt(now);
    }

    public static ProjectResponse toResponse(Project project) {
        return toResponse(project, null);
    }

    public static ProjectResponse toResponse(Project project, String ownerName) {
        return toResponse(project, ownerName, null);
    }

    public static ProjectResponse toResponse(Project project, String ownerName, String className) {
        return new ProjectResponse(project.getId(), project.getOwnerId(), ownerName, project.getClassId(), className,
                project.getTitle(),
                project.getDescription(), project.getGithubUrl(), project.getLiveUrl(),
                project.getTechnologies(), project.getCreatedAt(), project.getUpdatedAt());
    }
}
