package com.dolphin.controller;

import com.dolphin.dto.leetcode.CreateLeetCodeRequest;
import com.dolphin.dto.leetcode.LeetCodeResponse;
import com.dolphin.dto.leetcode.UpdateLeetCodeRequest;
import com.dolphin.security.AuthenticatedUser;
import com.dolphin.service.LeetCodeService;
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
@RequestMapping("/api/leetcode")
@PreAuthorize("hasRole('STUDENT')")
public class LeetCodeController {

    private final LeetCodeService leetCodeService;

    public LeetCodeController(LeetCodeService leetCodeService) {
        this.leetCodeService = leetCodeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LeetCodeResponse create(@AuthenticationPrincipal AuthenticatedUser user,
                                   @Valid @RequestBody CreateLeetCodeRequest request) {
        return leetCodeService.create(user.id(), request);
    }

    @GetMapping("/my")
    public List<LeetCodeResponse> mine(@AuthenticationPrincipal AuthenticatedUser user) {
        return leetCodeService.listOwn(user.id());
    }

    @PutMapping("/{id}")
    public LeetCodeResponse update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String id,
                                   @Valid @RequestBody UpdateLeetCodeRequest request) {
        return leetCodeService.update(user.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String id) {
        leetCodeService.delete(user.id(), id);
    }
}
