package com.pollpulse.user;

import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<User> hasRole(RoleName roleName) {
        return (root, query, criteriaBuilder) -> {
            query.distinct(true);
            return criteriaBuilder.equal(root.join("roles").get("name"), roleName);
        };
    }

    public static Specification<User> hasDepartment(String department) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("department"), department);
    }

    public static Specification<User> hasBatch(String batch) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("batch"), batch);
    }

    public static Specification<User> search(String search) {
        String pattern = "%" + escapeLike(search.toLowerCase(Locale.ROOT)) + "%";
        return (root, query, criteriaBuilder) -> criteriaBuilder.or(
                criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("name")), pattern, '\\'),
                criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("email")), pattern, '\\'));
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
