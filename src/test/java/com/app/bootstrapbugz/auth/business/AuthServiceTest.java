package com.app.bootstrapbugz.auth.business;

import com.app.bootstrapbugz.constant.JwtPurpose;
import com.app.bootstrapbugz.dto.model.user.UserDto;
import com.app.bootstrapbugz.dto.request.auth.ForgotPasswordRequest;
import com.app.bootstrapbugz.dto.request.auth.ResendConfirmationEmailRequest;
import com.app.bootstrapbugz.dto.request.auth.ResetPasswordRequest;
import com.app.bootstrapbugz.dto.request.auth.SignUpRequest;
import com.app.bootstrapbugz.error.exception.ForbiddenException;
import com.app.bootstrapbugz.error.exception.ResourceNotFound;
import com.app.bootstrapbugz.hal.user.UserDtoModelAssembler;
import com.app.bootstrapbugz.model.user.Role;
import com.app.bootstrapbugz.model.user.RoleName;
import com.app.bootstrapbugz.model.user.User;
import com.app.bootstrapbugz.repository.user.RoleRepository;
import com.app.bootstrapbugz.repository.user.UserRepository;
import com.app.bootstrapbugz.security.jwt.JwtUtilities;
import com.app.bootstrapbugz.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Spy
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private MessageSource messageSource;
    @Mock
    private JwtUtilities jwtUtilities;
    @Spy
    private UserDtoModelAssembler assembler;

    @InjectMocks
    private AuthServiceImpl authService;

    private Role userRole;
    private User user;

    @BeforeEach
    void init() {
        userRole = new Role(1L, RoleName.ROLE_USER);
        user = new User()
                .setId(1L)
                .setFirstName("User")
                .setLastName("User")
                .setUsername("user")
                .setEmail("user@localhost.com")
                .setPassword(bCryptPasswordEncoder.encode("123"))
                .setActivated(true)
                .setRoles(Set.of(userRole));
    }

    @Test
    void signUp_ok() {
        when(roleRepository.findByName(RoleName.ROLE_USER)).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenReturn(user);
        SignUpRequest signUpRequest = signUpRequest();
        UserDto createdUser = authService.signUp(signUpRequest);
        assertThat(createdUser).isNotNull();
        assertEquals(signUpRequest.getUsername(), createdUser.getUsername());
        assertEquals(signUpRequest.getEmail(), createdUser.getEmail());
    }

    private SignUpRequest signUpRequest() {
        return new SignUpRequest()
                .setFirstName("User")
                .setLastName("User")
                .setUsername("user")
                .setEmail("user@localhost.com")
                .setPassword("123")
                .setConfirmPassword("123");
    }

    @Test
    void confirmRegistration_ok() {
        user.setActivated(false);
        LocalDateTime beforeUpdate = user.getUpdatedAt();
        when(userRepository.findByUsername(null)).thenReturn(Optional.ofNullable(user));
        authService.confirmRegistration("");
        assertTrue(user.isActivated());
        assertNotEquals(beforeUpdate, user.getUpdatedAt());
    }

    @Test
    void confirmRegistration_invalidToken_forbidden() {
        assertThrows(ForbiddenException.class, () -> authService.confirmRegistration(""));
    }

    @Test
    void resendConfirmationEmail_ok() {
        user.setActivated(false);
        when(userRepository.findByUsername("user")).thenReturn(Optional.ofNullable(user));
        ResendConfirmationEmailRequest resendConfirmationEmailRequest = new ResendConfirmationEmailRequest("user");
        authService.resendConfirmationEmail(resendConfirmationEmailRequest);
        verify(jwtUtilities, times(1)).createToken(user, JwtPurpose.CONFIRM_REGISTRATION);
    }

    @Test
    void resendConfirmationEmail_userAlreadyActivated_forbidden() {
        when(userRepository.findByUsername("user")).thenReturn(Optional.ofNullable(user));
        ResendConfirmationEmailRequest resendConfirmationEmailRequest = new ResendConfirmationEmailRequest("user");
        assertThrows(ForbiddenException.class, () -> authService.resendConfirmationEmail(resendConfirmationEmailRequest));
    }

    @Test
    void resendConfirmationEmail_notFound() {
        ResendConfirmationEmailRequest resendConfirmationEmailRequest = new ResendConfirmationEmailRequest("notFound");
        assertThrows(ResourceNotFound.class, () -> authService.resendConfirmationEmail(resendConfirmationEmailRequest));
    }

    @Test
    void forgotPassword_ok() {
        when(userRepository.findByEmail("user@localhost.com")).thenReturn(Optional.ofNullable(user));
        ForgotPasswordRequest forgotPasswordRequest = new ForgotPasswordRequest("user@localhost.com");
        authService.forgotPassword(forgotPasswordRequest);
        verify(jwtUtilities, times(1)).createToken(user, JwtPurpose.FORGOT_PASSWORD);
    }

    @Test
    void forgotPassword_notFound() {
        ForgotPasswordRequest forgotPasswordRequest = new ForgotPasswordRequest("notFound@localhost.com");
        assertThrows(ResourceNotFound.class, () -> authService.forgotPassword(forgotPasswordRequest));
    }

    @Test
    void resetPassword_ok() {
        LocalDateTime beforeUpdate = user.getUpdatedAt();
        when(userRepository.findByUsername(null)).thenReturn(Optional.ofNullable(user));
        ResetPasswordRequest resetPasswordRequest = new ResetPasswordRequest("", "1234", "1234");
        authService.resetPassword(resetPasswordRequest);
        assertTrue(bCryptPasswordEncoder.matches("1234", user.getPassword()));
        assertFalse(bCryptPasswordEncoder.matches("123", user.getPassword()));
        assertNotEquals(beforeUpdate, user.getUpdatedAt());
    }

    @Test
    void resetPassword_invalidToken_forbidden() {
        ResetPasswordRequest resetPasswordRequest = new ResetPasswordRequest("", "123", "123");
        assertThrows(ForbiddenException.class, () -> authService.resetPassword(resetPasswordRequest));
    }
}
