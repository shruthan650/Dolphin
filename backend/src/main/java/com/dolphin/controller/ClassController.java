package com.dolphin.controller;

import com.dolphin.dto.classdto.ClassDetailsResponse;
import com.dolphin.dto.classdto.ClassResponse;
import com.dolphin.dto.classdto.CreateClassRequest;
import com.dolphin.dto.classdto.JoinClassResponse;
import com.dolphin.dto.classdto.UpdateClassRequest;
import com.dolphin.security.AuthenticatedUser;
import com.dolphin.service.ClassService;
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
@RequestMapping("/api/classes")
public class ClassController {

    private final ClassService classService;

    public ClassController(ClassService classService) {
        this.classService = classService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TEACHER')")
    public ClassResponse create(@AuthenticationPrincipal AuthenticatedUser user,
                                @Valid @RequestBody CreateClassRequest request) {
        return classService.createClass(user.id(), request);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('TEACHER')")
    public List<ClassResponse> mine(@AuthenticationPrincipal AuthenticatedUser user) {
        return classService.listTeacherClasses(user.id());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ClassDetailsResponse details(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String id) {
        return classService.getClassDetails(user.id(), id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ClassResponse update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String id,
                                @Valid @RequestBody UpdateClassRequest request) {
        return classService.updateClass(user.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('TEACHER')")
    public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String id) {
        classService.deleteClass(user.id(), id);
    }

    @DeleteMapping("/{id}/students/{studentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('TEACHER')")
    public void removeStudent(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String id,
                              @PathVariable String studentId) {
        classService.removeStudent(user.id(), id, studentId);
    }

    @PostMapping("/join/{classCode}")
    @PreAuthorize("hasRole('STUDENT')")
    public JoinClassResponse join(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String classCode) {
        return classService.joinClass(user.id(), classCode.trim());
    }
}
