package com.dolphin.controller;

import com.dolphin.dto.publicprofile.PublicStudentDirectoryResponse;
import com.dolphin.dto.publicprofile.PublicStudentProfileResponse;
import com.dolphin.security.AuthenticatedUser;
import com.dolphin.service.PublicStudentProfileService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/students")
@PreAuthorize("hasRole('STUDENT')")
public class PublicStudentController {

    private final PublicStudentProfileService profileService;

    public PublicStudentController(PublicStudentProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/public")
    public PublicStudentDirectoryResponse directory(@AuthenticationPrincipal AuthenticatedUser user,
                                                     @RequestParam(required = false) String search,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "12") int size) {
        return profileService.directory(user.id(), search, page, size);
    }

    @GetMapping("/{profileId}/public-profile")
    public PublicStudentProfileResponse profile(@AuthenticationPrincipal AuthenticatedUser user,
                                                @PathVariable String profileId) {
        return profileService.profile(user.id(), profileId);
    }
}
