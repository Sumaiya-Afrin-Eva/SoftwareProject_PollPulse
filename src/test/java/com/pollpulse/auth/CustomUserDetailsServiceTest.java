package com.pollpulse.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import com.pollpulse.user.Role;
import com.pollpulse.user.RoleName;
import com.pollpulse.user.User;
import com.pollpulse.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    private CustomUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        userDetailsService = new CustomUserDetailsService(userRepository);
    }

    @Test
    void shouldLoadUser_whenEmailExists() {
        User user = createUser("voter@pollpulse.test", true, Set.of(new Role(RoleName.ROLE_VOTER)));
        when(userRepository.findByEmailIgnoreCase("voter@pollpulse.test")).thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername("voter@pollpulse.test");

        assertEquals("voter@pollpulse.test", result.getUsername());
        assertEquals("hashed-password", result.getPassword());
        verify(userRepository).findByEmailIgnoreCase("voter@pollpulse.test");
    }

    @Test
    void shouldMapAuthorities_whenUserHasRoles() {
        User user = createUser("admin@pollpulse.test", true,
                Set.of(new Role(RoleName.ROLE_ADMIN), new Role(RoleName.ROLE_COMMISSIONER)));
        when(userRepository.findByEmailIgnoreCase("admin@pollpulse.test")).thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername("admin@pollpulse.test");

        assertEquals(Set.of(
                        new SimpleGrantedAuthority("ROLE_ADMIN"),
                        new SimpleGrantedAuthority("ROLE_COMMISSIONER")),
                Set.copyOf(result.getAuthorities()));
    }

    @Test
    void shouldReportDisabled_whenUserIsDisabled() {
        User user = createUser("voter@pollpulse.test", false, Set.of(new Role(RoleName.ROLE_VOTER)));
        when(userRepository.findByEmailIgnoreCase("voter@pollpulse.test")).thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername("voter@pollpulse.test");

        assertFalse(result.isEnabled());
    }

    @Test
    void shouldThrowUsernameNotFoundException_whenEmailIsUnknown() {
        when(userRepository.findByEmailIgnoreCase("missing@pollpulse.test")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("missing@pollpulse.test"));
    }

    private User createUser(String email, boolean enabled, Set<Role> roles) {
        User user = new User();
        user.setName("Test User");
        user.setEmail(email);
        user.setPasswordHash("hashed-password");
        user.setEnabled(enabled);
        user.setRoles(roles);
        return user;
    }
}
