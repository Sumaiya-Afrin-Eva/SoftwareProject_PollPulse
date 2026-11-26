package com.pollpulse.config;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import com.pollpulse.user.Role;
import com.pollpulse.user.RoleName;
import com.pollpulse.user.RoleRepository;
import com.pollpulse.user.User;
import com.pollpulse.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean seedDemoUsers;
    private final String seedPassword;

    public DataInitializer(
            RoleRepository roleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.seed-demo-users:false}") boolean seedDemoUsers,
            @Value("${app.seed-password:}") String seedPassword) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedDemoUsers = seedDemoUsers;
        this.seedPassword = seedPassword;
    }

    @Override
    public void run(String... args) {
        if (seedDemoUsers && seedPassword.isBlank()) {
            throw new IllegalStateException(
                    "SEED_PASSWORD must be set to a non-blank value when SEED_DEMO_USERS is enabled.");
        }

        Map<RoleName, Role> roles = new EnumMap<>(RoleName.class);
        for (RoleName roleName : RoleName.values()) {
            Role role = roleRepository.findByName(roleName)
                    .orElseGet(() -> roleRepository.save(new Role(roleName)));
            roles.put(roleName, role);
        }

        if (seedDemoUsers) {
            seedUser("PollPulse Admin", "admin@pollpulse.test", RoleName.ROLE_ADMIN, roles);
            seedUser("PollPulse Commissioner", "commissioner@pollpulse.test",
                    RoleName.ROLE_COMMISSIONER, roles);
            seedUser("PollPulse Candidate", "candidate@pollpulse.test", RoleName.ROLE_CANDIDATE, roles);
            seedUser("PollPulse Voter", "voter@pollpulse.test", RoleName.ROLE_VOTER, roles);
        }
    }

    private void seedUser(String name, String email, RoleName roleName, Map<RoleName, Role> roles) {
        if (userRepository.existsByEmail(email)) {
            return;
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(seedPassword));
        user.setRoles(Set.of(roles.get(roleName)));
        userRepository.save(user);
    }
}
