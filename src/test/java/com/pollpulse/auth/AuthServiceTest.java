package com.pollpulse.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import com.pollpulse.exception.ConflictException;
import com.pollpulse.user.Role;
import com.pollpulse.user.RoleName;
import com.pollpulse.user.RoleRepository;
import com.pollpulse.user.User;
import com.pollpulse.user.UserRepository;
import com.pollpulse.user.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockMakers;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String RAW_PASSWORD = "StrongPass1!";

    @Mock(mockMaker = MockMakers.SUBCLASS)
    private UserRepository userRepository;

    @Mock(mockMaker = MockMakers.SUBCLASS)
    private RoleRepository roleRepository;

    private AuthService authService;
    private Role voterRole;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, roleRepository, new BCryptPasswordEncoder());
        voterRole = new Role(RoleName.ROLE_VOTER);
        voterRole.setId(4L);
    }

    @Test
    void shouldThrowConflict_whenEmailIsAlreadyRegistered() {
        when(userRepository.existsByEmail("person@example.com")).thenReturn(true);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> authService.register(request("Person", "person@example.com", RAW_PASSWORD)));

        assertEquals("Email already registered", exception.getMessage());
    }

    @Test
    void shouldStoreBcryptHash_whenRegisteringWithValidPassword() {
        prepareSuccessfulSave();

        authService.register(request("Person", "person@example.com", RAW_PASSWORD));

        User savedUser = captureSavedUser();
        assertNotEquals(RAW_PASSWORD, savedUser.getPasswordHash());
        assertTrue(savedUser.getPasswordHash().startsWith("$2"));
        assertTrue(new BCryptPasswordEncoder().matches(RAW_PASSWORD, savedUser.getPasswordHash()));
    }

    @Test
    void shouldAssignVoterRole_whenRegisteringNewUser() {
        prepareSuccessfulSave();

        UserResponse response = authService.register(request("Person", "person@example.com", RAW_PASSWORD));

        User savedUser = captureSavedUser();
        assertEquals(Set.of(voterRole), savedUser.getRoles());
        assertEquals(java.util.List.of("ROLE_VOTER"), response.roles());
    }

    @Test
    void shouldStoreLowercaseEmail_whenSubmittedEmailHasWhitespaceAndUppercase() {
        prepareSuccessfulSave();

        UserResponse response =
                authService.register(request("Person", "  Person@Example.COM  ", RAW_PASSWORD));

        assertEquals("person@example.com", response.email());
        assertEquals("person@example.com", captureSavedUser().getEmail());
        verify(userRepository).existsByEmail("person@example.com");
    }

    private void prepareSuccessfulSave() {
        when(userRepository.existsByEmail("person@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_VOTER)).thenReturn(Optional.of(voterRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(12L);
            return user;
        });
    }

    private User captureSavedUser() {
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        return captor.getValue();
    }

    private RegisterRequest request(String name, String email, String password) {
        return new RegisterRequest(name, email, password, "Computing", "2026", "North Hall");
    }
}
