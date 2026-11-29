package com.pollpulse.user;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserAdminIntegrationTest {

    private static final String PASSWORD = "ValidPass1!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminEmail;
    private String voterEmail;
    private Long voterId;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        roleRepository.deleteAll();

        String unique = UUID.randomUUID().toString();
        adminEmail = "admin-" + unique + "@pollpulse.test";
        voterEmail = "voter-" + unique + "@pollpulse.test";

        Map<RoleName, Role> roles = new EnumMap<>(RoleName.class);
        for (RoleName roleName : RoleName.values()) {
            roles.put(roleName, roleRepository.save(new Role(roleName)));
        }

        User admin = new User();
        admin.setName("PollPulse Admin");
        admin.setEmail(adminEmail);
        admin.setPasswordHash(passwordEncoder.encode(PASSWORD));
        admin.setRoles(Set.of(roles.get(RoleName.ROLE_ADMIN)));
        userRepository.save(admin);

        User voter = new User();
        voter.setName("Campus Voter");
        voter.setEmail(voterEmail);
        voter.setPasswordHash(passwordEncoder.encode(PASSWORD));
        voter.setRoles(Set.of(roles.get(RoleName.ROLE_VOTER)));
        voterId = userRepository.save(voter).getId();
    }

    @Test
    void shouldListUsersWithPage_whenAdminFiltersByRole() throws Exception {
        mockMvc.perform(get("/api/users")
                        .with(user(adminEmail).roles("ADMIN"))
                        .param("role", "ROLE_VOTER")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value(voterEmail));
    }

    @Test
    void shouldForbidUserList_whenVoterRequestsIt() throws Exception {
        mockMvc.perform(get("/api/users")
                        .with(user(voterEmail).roles("VOTER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldDisableAccountAndRejectLogin_whenAdminDisablesVoter() throws Exception {
        mockMvc.perform(patch("/api/users/{id}/enabled", voterId)
                        .with(user(adminEmail).roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"enabled": false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(formLogin("/login")
                        .user("email", voterEmail)
                        .password(PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }
}
