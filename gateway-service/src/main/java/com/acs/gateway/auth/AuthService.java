package com.acs.gateway.auth;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class AuthService {

    private static final Map<String, List<String>> EMPLOYEE_APPS = Map.of(
            "EMP001", List.of("portal", "admin", "hr"),
            "EMP002", List.of("portal", "sales"),
            "EMP003", List.of("portal")
    );

    public Mono<List<String>> authenticate(String employeeNumber, String password) {
        List<String> apps = EMPLOYEE_APPS.get(employeeNumber);
        if (apps == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown employee"));
        }
        return Mono.just(apps);
    }
}
