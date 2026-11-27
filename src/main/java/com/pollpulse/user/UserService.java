package com.pollpulse.user;

import com.pollpulse.exception.BadRequestException;
import com.pollpulse.exception.ResourceNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
