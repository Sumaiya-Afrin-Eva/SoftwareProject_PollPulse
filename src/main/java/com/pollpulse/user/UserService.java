package com.pollpulse.user;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.pollpulse.exception.BadRequestException;
import com.pollpulse.exception.ConflictException;
import com.pollpulse.exception.PageResponse;
import com.pollpulse.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getUsers(
            String actingAdminEmail,
            int page,
            int size,
            String sort,
            String role,
            String department,
            String batch,
            String search) {
        if (page < 0) {
            throw new BadRequestException("Page must not be negative");
        }
        if (size < 1 || size > 100) {
            throw new BadRequestException("Page size must be between 1 and 100");
        }
        List<String> allowedSorts = List.of("id", "name", "email", "department", "batch");
        if (!allowedSorts.contains(sort)) {
            throw new BadRequestException("Unsupported sort field: " + sort);
        }

        Specification<User> specification = Specification.where(null);
        if (role != null && !role.isBlank()) {
            String roleValue = role.trim();
            RoleName roleName;
            try {
                roleName = RoleName.valueOf(roleValue);
            } catch (IllegalArgumentException exception) {
                throw new BadRequestException("Unknown role: " + roleValue);
            }
            specification = specification.and(UserSpecifications.hasRole(roleName));
        }
        if (department != null && !department.isBlank()) {
            specification = specification.and(UserSpecifications.hasDepartment(department.trim()));
        }
        if (batch != null && !batch.isBlank()) {
            specification = specification.and(UserSpecifications.hasBatch(batch.trim()));
        }
        if (search != null && !search.isBlank()) {
            specification = specification.and(UserSpecifications.search(search.trim()));
        }

        Page<UserResponse> users = userRepository.findAll(
                        specification,
                        PageRequest.of(page, size, Sort.by(sort)))
                .map(UserResponse::from);
        return PageResponse.from(users);
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(String actingAdminEmail, Long id) {
        return UserResponse.from(findUser(id));
    }

    @Transactional
    public UserResponse updateUser(
            String actingAdminEmail,
            Long id,
            AdminUpdateUserRequest request) {
        User user = findUser(id);
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        if (isSameUser(user, actingAdminEmail)
                && !user.getEmail().equalsIgnoreCase(normalizedEmail)) {
            throw new ConflictException("You cannot change your own email");
        }
        userRepository.findByEmailIgnoreCase(normalizedEmail)
                .filter(existing -> !existing.getId().equals(user.getId()))
                .ifPresent(existing -> {
                    throw new ConflictException("Email already registered");
                });

        user.setName(request.name().trim());
        user.setEmail(normalizedEmail);
        user.setDepartment(normalizeOptional(request.department()));
        user.setBatch(normalizeOptional(request.batch()));
        user.setHall(normalizeOptional(request.hall()));
        return UserResponse.from(user);
    }

    @Transactional
    public void deleteUser(String actingAdminEmail, Long id) {
        User user = findUser(id);
        if (isSameUser(user, actingAdminEmail)) {
            throw new ConflictException("You cannot delete your own account");
        }
        userRepository.delete(user);
        userRepository.flush();
    }

    @Transactional
    public UserResponse updateRoles(
            String actingAdminEmail,
            Long id,
            UpdateRolesRequest request) {
        if (request.roles() == null || request.roles().isEmpty()) {
            throw new BadRequestException("At least one role is required");
        }
        User user = findUser(id);
        Set<Role> roles = new LinkedHashSet<>();
        for (String roleValue : request.roles()) {
            RoleName roleName;
            try {
                roleName = RoleName.valueOf(roleValue);
            } catch (IllegalArgumentException | NullPointerException exception) {
                throw new BadRequestException("Unknown role: " + roleValue);
            }
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new BadRequestException("Unknown role: " + roleValue));
            roles.add(role);
        }
        if (isSameUser(user, actingAdminEmail)
                && hasRole(user, RoleName.ROLE_ADMIN)
                && roles.stream().noneMatch(role -> role.getName() == RoleName.ROLE_ADMIN)) {
            throw new ConflictException("You cannot remove your own ROLE_ADMIN role");
        }
        user.setRoles(roles);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateEnabled(
            String actingAdminEmail,
            Long id,
            UpdateEnabledRequest request) {
        User user = findUser(id);
        if (isSameUser(user, actingAdminEmail) && !request.enabled()) {
            throw new ConflictException("You cannot disable your own account");
        }
        user.setEnabled(request.enabled());
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {
        return UserResponse.from(findUser(email));
    }

    @Transactional
    public UserResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = findUser(email);
        user.setName(request.name().trim());
        user.setDepartment(normalizeOptional(request.department()));
        user.setBatch(normalizeOptional(request.batch()));
        user.setHall(normalizeOptional(request.hall()));
        return UserResponse.from(user);
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = findUser(email);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password must be different");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    private User findUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private boolean isSameUser(User user, String email) {
        return user.getEmail().equalsIgnoreCase(email);
    }

    private boolean hasRole(User user, RoleName roleName) {
        return user.getRoles().stream().anyMatch(role -> role.getName() == roleName);
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
