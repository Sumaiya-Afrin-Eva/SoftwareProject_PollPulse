package com.pollpulse.auth;

import java.util.Locale;
import java.util.Set;

import com.pollpulse.exception.ConflictException;
import com.pollpulse.user.Role;
import com.pollpulse.user.RoleName;
import com.pollpulse.user.RoleRepository;
import com.pollpulse.user.User;
import com.pollpulse.user.UserRepository;
import com.pollpulse.user.UserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already registered");
        }

        Role voterRole = roleRepository.findByName(RoleName.ROLE_VOTER)
                .orElseThrow(() -> new IllegalStateException("Required role ROLE_VOTER is missing"));

        User user = new User();
        user.setName(request.name());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDepartment(request.department());
        user.setBatch(request.batch());
        user.setHall(request.hall());
        user.setRoles(Set.of(voterRole));

        User savedUser = userRepository.save(user);
        return toResponse(savedUser);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getDepartment(),
                user.getBatch(),
                user.getHall(),
                user.isEnabled(),
                user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .sorted()
                        .toList());
    }
}
