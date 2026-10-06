package com.dolphin.controller;

import com.dolphin.dto.project.CreateProjectRequest;
import com.dolphin.dto.project.ProjectResponse;
import com.dolphin.dto.project.UpdateProjectRequest;
import com.dolphin.security.AuthenticatedUser;
import com.dolphin.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@PreAuthorize("hasRole('STUDENT')")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(@AuthenticationPrincipal AuthenticatedUser user,
                                  @Valid @RequestBody CreateProjectRequest request) {
        return projectService.create(user.id(), request);
    }

    @GetMapping("/my")
    public List<ProjectResponse> mine(@AuthenticationPrincipal AuthenticatedUser user) {
        return projectService.listOwn(user.id());
    }

    @GetMapping("/{id}")
    public ProjectResponse get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String id) {
        return projectService.getOwn(user.id(), id);
    }

    @PutMapping("/{id}")
    public ProjectResponse update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String id,
                                  @Valid @RequestBody UpdateProjectRequest request) {
        return projectService.update(user.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String id) {
        projectService.delete(user.id(), id);
    }
}
