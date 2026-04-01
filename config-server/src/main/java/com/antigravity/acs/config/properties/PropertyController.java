package com.antigravity.acs.config.properties;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/properties")
public class PropertyController {

    private final PropertyRepository repository;
    private final RestClient restClient;

    public PropertyController(PropertyRepository repository) {
        this.repository = repository;
        this.restClient = RestClient.builder()
                .baseUrl("http://localhost:8888")
                .build();
    }

    @GetMapping
    public List<PropertyEntity> list(
            @RequestParam(required = false) String application,
            @RequestParam(required = false) String profile,
            @RequestParam(required = false) String label) {
        return repository.findByFilters(application, profile, label);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PropertyEntity> get(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<PropertyEntity> create(@RequestBody PropertyCreateRequest request) {
        PropertyEntity created = repository.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PropertyEntity> update(@PathVariable Long id, @RequestBody PropertyUpdateRequest request) {
        return repository.update(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (repository.delete(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/batch")
    public ResponseEntity<List<PropertyEntity>> batchCreate(@RequestBody List<PropertyCreateRequest> requests) {
        List<PropertyEntity> created = requests.stream().map(repository::create).toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/batch")
    public ResponseEntity<Void> batchDelete(
            @RequestParam String application,
            @RequestParam String profile,
            @RequestParam String label,
            @RequestParam String keyPrefix) {
        repository.deleteByKeyPrefix(application, profile, label, keyPrefix);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh() {
        try {
            restClient.post()
                    .uri("/actuator/busrefresh")
                    .retrieve()
                    .toBodilessEntity();
            return ResponseEntity.ok(Map.of("status", "refreshed"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @GetMapping("/applications")
    public List<String> applications() {
        return repository.findDistinctApplications();
    }

    @GetMapping("/profiles")
    public List<String> profiles() {
        return repository.findDistinctProfiles();
    }

    @GetMapping("/labels")
    public List<String> labels() {
        return repository.findDistinctLabels();
    }
}
