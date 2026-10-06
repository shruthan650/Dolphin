package com.dolphin.controller;

import com.dolphin.dto.advice.AdviceResponse;
import com.dolphin.dto.advice.CreateAdviceRequest;
import com.dolphin.security.AuthenticatedUser;
import com.dolphin.service.AdviceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/advice")
public class AdviceController {

    private final AdviceService adviceService;

    public AdviceController(AdviceService adviceService) {
        this.adviceService = adviceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TEACHER')")
    public AdviceResponse give(@AuthenticationPrincipal AuthenticatedUser user,
                               @Valid @RequestBody CreateAdviceRequest request) {
        return adviceService.give(user.id(), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('TEACHER')")
    public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String id) {
        adviceService.delete(user.id(), id);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public List<AdviceResponse> mine(@AuthenticationPrincipal AuthenticatedUser user) {
        return adviceService.forStudent(user.id());
    }
}
