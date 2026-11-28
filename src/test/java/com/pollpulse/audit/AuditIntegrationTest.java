package com.pollpulse.audit;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.pollpulse.user.Role;
import com.pollpulse.user.RoleName;
import com.pollpulse.user.RoleRepository;
import com.pollpulse.user.User;
import com.pollpulse.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditIntegrationTest {

    private static final String PASSWORD = "ValidPass1!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditService auditService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User admin;
    private String adminEmail;
    private String voterEmail;

    @BeforeEach
    void setUp() {
        clearAuditLogs();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        Map<RoleName, Role> roles = new EnumMap<>(RoleName.class);
        for (RoleName roleName : RoleName.values()) {
            roles.put(roleName, roleRepository.save(new Role(roleName)));
        }

        String unique = UUID.randomUUID().toString();
        adminEmail = "admin-" + unique + "@pollpulse.test";
        voterEmail = "voter-" + unique + "@pollpulse.test";

        admin = userRepository.save(
                newUser("PollPulse Admin", adminEmail, roles.get(RoleName.ROLE_ADMIN)));
        userRepository.save(
                newUser("Campus Voter", voterEmail, roles.get(RoleName.ROLE_VOTER)));
    }

    @AfterEach
    void tearDown() {
        // audit_logs has a foreign key to users, and AuditLogRepository deliberately
        // exposes no delete, so the rows are removed with JdbcTemplate. Leaving them
        // behind would break other integration tests that call userRepository.deleteAll().
        clearAuditLogs();
        userRepository.deleteAll();
        roleRepository.deleteAll();
    }

    @Test
    void shouldListAuditLog_whenAdminRequestsIt() throws Exception {
        auditService.log(admin, AuditAction.ELECTION_CREATED, "Election", 5L, "Election created");

        mockMvc.perform(get("/api/audit")
                        .with(user(adminEmail).roles("ADMIN"))
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].action").value("ELECTION_CREATED"))
                .andExpect(jsonPath("$.content[0].actorName").value("PollPulse Admin"))
                .andExpect(jsonPath("$.content[0].details").value("Election created"));
    }

    @Test
    void shouldForbidAuditLog_whenVoterRequestsIt() throws Exception {
        mockMvc.perform(get("/api/audit")
                        .with(user(voterEmail).roles("VOTER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReportBrokenChain_whenRecordIsModifiedInDatabase() throws Exception {
        Long firstId = auditService
                .log(admin, AuditAction.ELECTION_CREATED, "Election", 1L, "Election created")
                .getId();
        auditService.log(admin, AuditAction.VOTE_CAST, "Election", 1L, "Ballot cast");

        mockMvc.perform(get("/api/audit/verify")
                        .with(user(adminEmail).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.checkedCount").value(2));

        jdbcTemplate.update(
                "update audit_logs set details = ? where id = ?",
                "altered directly in the database",
                firstId);

        mockMvc.perform(get("/api/audit/verify")
                        .with(user(adminEmail).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.firstBrokenId").value(firstId.intValue()));
    }

    private void clearAuditLogs() {
        jdbcTemplate.update("delete from audit_logs");
    }

    private User newUser(String name, String email, Role role) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setRoles(Set.of(role));
        return user;
    }
}
