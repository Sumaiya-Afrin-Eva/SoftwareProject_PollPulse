package com.pollpulse.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.pollpulse.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockMakers;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

    private static final String ADMIN_EMAIL = "admin@pollpulse.test";

    @Mock(mockMaker = MockMakers.SUBCLASS)
    private UserRepository userRepository;

    @Mock(mockMaker = MockMakers.SUBCLASS)
    private RoleRepository roleRepository;

    private UserService userService;
    private User admin;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userRepository, roleRepository, new BCryptPasswordEncoder());

        adminRole = new Role(RoleName.ROLE_ADMIN);

        admin = new User();
        admin.setId(1L);
        admin.setName("PollPulse Admin");
        admin.setEmail(ADMIN_EMAIL);
        admin.setEnabled(true);
        admin.setRoles(new LinkedHashSet<>(Set.of(adminRole)));
    }

    @Test
    void shouldReplaceRoleSet_whenAdminAssignsRoles() {
        User target = new User();
        target.setId(2L);
        target.setName("Campus Voter");
        target.setEmail("voter@pollpulse.test");
        target.setRoles(new LinkedHashSet<>(
                Set.of(new Role(RoleName.ROLE_CANDIDATE), new Role(RoleName.ROLE_VOTER))));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(roleRepository.findByName(RoleName.ROLE_VOTER))
                .thenReturn(Optional.of(new Role(RoleName.ROLE_VOTER)));

        UserResponse response = userService.updateRoles(
                "other-admin@pollpulse.test",
                2L,
                new UpdateRolesRequest(List.of("ROLE_VOTER")));

        assertEquals(List.of("ROLE_VOTER"), response.roles());
        assertEquals(Set.of(RoleName.ROLE_VOTER), target.getRoles().stream()
                .map(Role::getName)
                .collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void shouldBlockSelfDelete_whenAdminTargetsOwnAccount() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThrows(
                ConflictException.class,
                () -> userService.deleteUser(ADMIN_EMAIL, 1L));

        verify(userRepository, never()).delete(admin);
        verify(userRepository, never()).flush();
    }

    @Test
    void shouldBlockSelfDisable_whenAdminDisablesOwnAccount() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThrows(
                ConflictException.class,
                () -> userService.updateEnabled(
                        ADMIN_EMAIL, 1L, new UpdateEnabledRequest(false)));

        assertTrue(admin.isEnabled());
    }
}
