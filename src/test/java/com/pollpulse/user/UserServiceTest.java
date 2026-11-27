package com.pollpulse.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.pollpulse.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockMakers;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final String EMAIL = "profile-user@pollpulse.test";
    private static final String CURRENT_PASSWORD = "CurrentPass1!";
    private static final String NEW_PASSWORD = "NewPassword2!";

    @Mock(mockMaker = MockMakers.SUBCLASS)
    private UserRepository userRepository;

    private BCryptPasswordEncoder passwordEncoder;
    private UserService userService;
    private User user;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        userService = new UserService(userRepository, passwordEncoder);
        user = new User();
        user.setEmail(EMAIL);
        user.setPasswordHash(passwordEncoder.encode(CURRENT_PASSWORD));
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
    }

    @Test
    void shouldRejectPasswordChange_whenCurrentPasswordIsWrong() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> userService.changePassword(
                        EMAIL, new ChangePasswordRequest("WrongPass1!", NEW_PASSWORD)));

        assertEquals("Current password is incorrect", exception.getMessage());
        assertTrue(passwordEncoder.matches(CURRENT_PASSWORD, user.getPasswordHash()));
    }

    @Test
    void shouldStoreBcryptHash_whenChangingPassword() {
        userService.changePassword(
                EMAIL, new ChangePasswordRequest(CURRENT_PASSWORD, NEW_PASSWORD));

        assertNotEquals(NEW_PASSWORD, user.getPasswordHash());
        assertTrue(user.getPasswordHash().startsWith("$2"));
        assertTrue(passwordEncoder.matches(NEW_PASSWORD, user.getPasswordHash()));
        verify(userRepository).findByEmailIgnoreCase(EMAIL);
    }
}
