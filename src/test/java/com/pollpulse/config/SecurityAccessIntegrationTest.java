package com.pollpulse.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.pollpulse.user.Role;
import com.pollpulse.user.RoleName;
import com.pollpulse.user.RoleRepository;
import com.pollpulse.user.User;
import com.pollpulse.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAccessIntegrationTest {

    private static final String PASSWORD = "SecurityTestPass1!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminEmail;
    private String voterAEmail;
    private String voterBEmail;
    private Long voterAId;
    private Long voterBId;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        roleRepository.deleteAll();

        String unique = UUID.randomUUID().toString();
        adminEmail = "admin-" + unique + "@pollpulse.test";
        voterAEmail = "voter-a-" + unique + "@pollpulse.test";
        voterBEmail = "voter-b-" + unique + "@pollpulse.test";

        Map<RoleName, Role> roles = new EnumMap<>(RoleName.class);
        for (RoleName roleName : RoleName.values()) {
            roles.put(roleName, roleRepository.save(new Role(roleName)));
        }

        saveUser("PollPulse Admin", adminEmail, "Administration", "2025", "Main Hall",
                roles.get(RoleName.ROLE_ADMIN));
        voterAId = saveUser("Voter A", voterAEmail, "Computing", "2025", "North Hall",
                roles.get(RoleName.ROLE_VOTER)).getId();
        voterBId = saveUser("Voter B", voterBEmail, "Science", "2024", "South Hall",
                roles.get(RoleName.ROLE_VOTER)).getId();
    }

    @Test
    void shouldReturn401Json_whenAnonymousCallsProtectedEndpoints() throws Exception {
        List<MockHttpServletRequestBuilder> requests = List.of(
                get("/api/users/me"),
                put("/api/users/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Anonymous Update"}
                                """),
                get("/api/users"),
                get("/api/users/{id}", voterAId),
                post("/api/auth/logout").with(csrf()));

        for (MockHttpServletRequestBuilder request : requests) {
            mockMvc.perform(request)
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(header().doesNotExist("Location"))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.message").value("Authentication required"));
        }
    }

    @Test
    void shouldReturn403ForVoterAndAllowAdmin_onAdminEndpoints() throws Exception {
        List<MockHttpServletRequestBuilder> voterRequests =
                adminEndpointRequests(voterAEmail, "VOTER", voterAEmail, voterAId);
        for (MockHttpServletRequestBuilder request : voterRequests) {
            mockMvc.perform(request)
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(403));
        }

        List<MockHttpServletRequestBuilder> adminRequests =
                adminEndpointRequests(adminEmail, "ADMIN", voterAEmail, voterAId);
        for (MockHttpServletRequestBuilder request : adminRequests) {
            mockMvc.perform(request)
                    .andExpect(result -> assertFalse(
                            result.getResponse().getStatus() == 401
                                    || result.getResponse().getStatus() == 403,
                            "Admin endpoint unexpectedly denied with status "
                                    + result.getResponse().getStatus()));
        }
    }

    @Test
    void shouldKeepProfileAccessToOwnerOnly() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .with(user(voterAEmail).roles("VOTER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(voterAEmail))
                .andExpect(jsonPath("$.name").value("Voter A"));

        mockMvc.perform(put("/api/users/me")
                        .with(user(voterAEmail).roles("VOTER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Voter A Updated",
                                  "department": "Updated Department",
                                  "batch": "2027",
                                  "hall": "Updated Hall",
                                  "email": "changed-email@pollpulse.test",
                                  "roles": ["ROLE_ADMIN"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Voter A Updated"))
                .andExpect(jsonPath("$.email").value(voterAEmail))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_VOTER"));

        User voterA = userRepository.findById(voterAId).orElseThrow();
        assertEquals("Voter A Updated", voterA.getName());
        assertEquals(voterAEmail, voterA.getEmail());
        assertEquals("Updated Department", voterA.getDepartment());
        assertEquals("2027", voterA.getBatch());
        assertEquals("Updated Hall", voterA.getHall());
        assertEquals(Set.of(RoleName.ROLE_VOTER), roleNames(voterA));

        User voterB = userRepository.findById(voterBId).orElseThrow();
        assertEquals("Voter B", voterB.getName());
        assertEquals(voterBEmail, voterB.getEmail());
        assertEquals("Science", voterB.getDepartment());
        assertEquals("2024", voterB.getBatch());
        assertEquals("South Hall", voterB.getHall());
        assertEquals(Set.of(RoleName.ROLE_VOTER), roleNames(voterB));

        mockMvc.perform(get("/api/users/{id}", voterBId)
                        .with(user(voterAEmail).roles("VOTER")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403));
    }

    private List<MockHttpServletRequestBuilder> adminEndpointRequests(
            String email, String role, String targetEmail, Long targetId) {
        List<MockHttpServletRequestBuilder> requests = new ArrayList<>();
        requests.add(get("/api/users").with(user(email).roles(role)));
        requests.add(get("/api/users/{id}", targetId).with(user(email).roles(role)));
        requests.add(put("/api/users/{id}", targetId)
                .with(user(email).roles(role))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Updated Target",
                          "email": "%s",
                          "department": "Computing",
                          "batch": "2025",
                          "hall": "North Hall"
                        }
                        """.formatted(targetEmail)));
        requests.add(patch("/api/users/{id}/roles", targetId)
                .with(user(email).roles(role))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"roles": ["ROLE_VOTER"]}
                        """));
        requests.add(patch("/api/users/{id}/enabled", targetId)
                .with(user(email).roles(role))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"enabled": true}
                        """));
        requests.add(delete("/api/users/{id}", targetId)
                .with(user(email).roles(role))
                .with(csrf()));
        return requests;
    }

    private User saveUser(
            String name,
            String email,
            String department,
            String batch,
            String hall,
            Role role) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setDepartment(department);
        user.setBatch(batch);
        user.setHall(hall);
        user.setRoles(Set.of(role));
        return userRepository.save(user);
    }

    private Set<RoleName> roleNames(User user) {
        return user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
    }
}
