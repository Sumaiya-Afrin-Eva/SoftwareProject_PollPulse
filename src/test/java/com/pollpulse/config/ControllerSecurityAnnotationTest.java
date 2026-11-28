package com.pollpulse.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class ControllerSecurityAnnotationTest {

    private static final Set<String> PUBLIC_ENDPOINTS = Set.of(
            "AuthApiController.register", // POST /api/auth/register
            "AuthApiController.login", // POST /api/auth/login
            "AuthApiController.csrf" // GET /api/auth/csrf
    );

    @Test
    void shouldHavePreAuthorize_onEveryRestEndpoint_exceptWhitelisted() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        List<String> offendingMethods = new ArrayList<>();
        for (var candidate : scanner.findCandidateComponents("com.pollpulse")) {
            Class<?> controller = ClassUtils.forName(
                    candidate.getBeanClassName(), ClassUtils.getDefaultClassLoader());
            boolean classIsSecured =
                    AnnotatedElementUtils.hasAnnotation(controller, PreAuthorize.class);

            ReflectionUtils.doWithMethods(controller, method -> {
                if (!isRequestMapped(method)) {
                    return;
                }

                String endpoint = controller.getSimpleName() + "." + method.getName();
                if (PUBLIC_ENDPOINTS.contains(endpoint)
                        || classIsSecured
                        || AnnotatedElementUtils.hasAnnotation(method, PreAuthorize.class)) {
                    return;
                }
                offendingMethods.add(endpoint);
            }, ReflectionUtils.USER_DECLARED_METHODS);
        }

        offendingMethods.sort(Comparator.naturalOrder());
        assertTrue(
                offendingMethods.isEmpty(),
                "Missing @PreAuthorize on REST methods: " + String.join(", ", offendingMethods));
    }

    private boolean isRequestMapped(Method method) {
        return AnnotatedElementUtils.hasAnnotation(method, GetMapping.class)
                || AnnotatedElementUtils.hasAnnotation(method, PostMapping.class)
                || AnnotatedElementUtils.hasAnnotation(method, PutMapping.class)
                || AnnotatedElementUtils.hasAnnotation(method, PatchMapping.class)
                || AnnotatedElementUtils.hasAnnotation(method, DeleteMapping.class)
                || AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class);
    }
}
