package com.example.demo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.AuthProfileUpdateDto;
import com.example.demo.dto.AuthResponseDto;
import com.example.demo.dto.LoginRequestDto;
import com.example.demo.dto.PasswordChangeDto;
import com.example.demo.dto.PasswordResetCompleteDto;
import com.example.demo.dto.PasswordResetRequestDto;
import com.example.demo.dto.PasswordTokenResponseDto;
import com.example.demo.dto.PasswordTokenStatusDto;
import com.example.demo.security.AppUserPrincipal;
import com.example.demo.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public AuthResponseDto login(@Valid @RequestBody LoginRequestDto dto, HttpServletRequest request) {
        AppUserPrincipal principal = authService.authenticate(dto);
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        request.getSession(true).setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        return authService.toResponse(principal);
    }

    @GetMapping("/me")
    public AuthResponseDto me(@AuthenticationPrincipal AppUserPrincipal principal) {
        return authService.toResponse(principal);
    }

    @PutMapping("/me")
    public AuthResponseDto updateProfile(
            @Valid @RequestBody AuthProfileUpdateDto dto,
            @AuthenticationPrincipal AppUserPrincipal principal,
            HttpServletRequest request
    ) {
        AppUserPrincipal updatedPrincipal = authService.updateProfile(principal, dto);
        storeAuthentication(updatedPrincipal, request);
        return authService.toResponse(updatedPrincipal);
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @Valid @RequestBody PasswordChangeDto dto,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        authService.changePassword(principal, dto);
    }

    @PostMapping("/password-reset/request")
    public PasswordTokenResponseDto requestPasswordReset(@Valid @RequestBody PasswordResetRequestDto dto) {
        return authService.requestPasswordReset(dto);
    }

    @GetMapping("/password-reset/{token}")
    public PasswordTokenStatusDto getPasswordResetStatus(@PathVariable("token") String token) {
        return authService.getPasswordTokenStatus(token);
    }

    @PostMapping("/password-reset/{token}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void completePasswordReset(
            @PathVariable("token") String token,
            @Valid @RequestBody PasswordResetCompleteDto dto
    ) {
        authService.completePasswordReset(token, dto);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        new SecurityContextLogoutHandler().logout(request, response, authentication);
    }

    private void storeAuthentication(AppUserPrincipal principal, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        request.getSession(true).setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    }
}
