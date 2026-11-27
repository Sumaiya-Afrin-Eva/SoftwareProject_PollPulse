package com.pollpulse.user;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
class ProfileIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String email;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        roleRepository.deleteAll();
        email = "profile-" + UUID.randomUUID() + "@pollpulse.test";

        Map<RoleName, Role> roles = new EnumMap<>(RoleName.class);
        for (RoleName roleName : RoleName.values()) {
            roles.put(roleName, roleRepository.save(new Role(roleName)));
        }

        User user = new User();
        user.setName("Profile User");
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("ProfilePass1!"));
        user.setDepartment("Computing");
        user.setBatch("2026");
        user.setHall("North Hall");
        user.setRoles(Set.of(roles.get(RoleName.ROLE_VOTER)));
        userRepository.save(user);
    }

    @Test
    void shouldReadAndUpdateOwnProfile_whenAuthenticated() throws Exception {
        mockMvc.perform(get("/profile").with(user(email)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"profile-form\"")))
                .andExpect(content().string(containsString("name=\"_csrf_header\" content=\"X-CSRF-TOKEN\"")));

        mockMvc.perform(get("/api/users/me").with(user(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.name").value("Profile User"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_VOTER"));

        mockMvc.perform(put("/api/users/me")
                        .with(user(email))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": " Updated Profile ",
                                  "department": "  Mathematics ",
                                  "batch": " ",
                                  "hall": "South Hall"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Profile"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_VOTER"))
                .andExpect(jsonPath("$.department").value("Mathematics"))
                .andExpect(jsonPath("$.batch").doesNotExist())
                .andExpect(jsonPath("$.hall").value("South Hall"));
    }

    @Test
    void shouldReturnUnauthorizedJson_whenAnonymousReadsProfile() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(containsString("\"message\":\"Authentication required\"")));
    }
}
