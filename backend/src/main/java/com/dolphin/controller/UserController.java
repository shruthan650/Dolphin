package com.dolphin.controller;

import com.dolphin.dto.account.ChangeEmailRequest;
import com.dolphin.dto.account.ChangePasswordRequest;
import com.dolphin.dto.account.DeleteAccountRequest;
import com.dolphin.dto.account.UpdateProfileRequest;
import com.dolphin.dto.auth.LoginResponse;
import com.dolphin.dto.common.UserResponse;
import com.dolphin.security.AuthenticatedUser;
import com.dolphin.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in user's own account, for every role. There is deliberately no endpoint taking another user's id. */
@RestController
@RequestMapping("/api/users/me")
@PreAuthorize("isAuthenticated()")
public class UserController {

    private final AccountService accountService;

    public UserController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
        return accountService.get(user.id());
    }

    @PutMapping
    public UserResponse updateProfile(@AuthenticationPrincipal AuthenticatedUser user,
                                      @Valid @RequestBody UpdateProfileRequest request) {
        return accountService.updateProfile(user.id(), request);
    }

    /** Returns a new token: the old ones stop working once the email changes. */
    @PutMapping("/email")
    public LoginResponse changeEmail(@AuthenticationPrincipal AuthenticatedUser user,
                                     @Valid @RequestBody ChangeEmailRequest request) {
        return accountService.changeEmail(user.id(), request);
    }

    /** Returns a new token: every other session is signed out. */
    @PatchMapping("/password")
    public LoginResponse changePassword(@AuthenticationPrincipal AuthenticatedUser user,
                                        @Valid @RequestBody ChangePasswordRequest request) {
        return accountService.changePassword(user.id(), request);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(@AuthenticationPrincipal AuthenticatedUser user,
                              @Valid @RequestBody DeleteAccountRequest request) {
        accountService.deleteOwnAccount(user.id(), request);
    }
}
