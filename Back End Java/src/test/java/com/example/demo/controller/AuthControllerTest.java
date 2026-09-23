package com.example.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import com.example.demo.dto.AuthResponseDto;
import com.example.demo.dto.LoginRequestDto;
import com.example.demo.security.AppUserPrincipal;
import com.example.demo.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpSession session;

    @InjectMocks
    private AuthController authController;

    @Test
    void loginRotatesExistingSessionBeforeStoringAuthentication() {
        LoginRequestDto credentials = LoginRequestDto.builder()
                .email("membre@terra.test")
                .motDePasse("mot-de-passe")
                .build();
        AppUserPrincipal principal = AppUserPrincipal.builder()
                .id(4L)
                .email("membre@terra.test")
                .role("membre")
                .actif(true)
                .build();
        AuthResponseDto response = AuthResponseDto.builder()
                .id(4L)
                .email("membre@terra.test")
                .role("membre")
                .actif(true)
                .build();

        when(authService.authenticate(credentials)).thenReturn(principal);
        when(authService.toResponse(principal)).thenReturn(response);
        when(request.getSession(false)).thenReturn(session);
        when(request.getSession(true)).thenReturn(session);

        AuthResponseDto result = authController.login(credentials, request);

        assertThat(result).isSameAs(response);
        verify(request).changeSessionId();
        verify(session).setAttribute(
                eq(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY),
                argThat(value -> value instanceof SecurityContext context
                        && context.getAuthentication() != null
                        && context.getAuthentication().getPrincipal() == principal)
        );
    }
}
