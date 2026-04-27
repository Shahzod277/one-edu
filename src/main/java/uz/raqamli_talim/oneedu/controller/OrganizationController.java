package uz.raqamli_talim.oneedu.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.raqamli_talim.oneedu.model.OrganizationDto;
import uz.raqamli_talim.oneedu.model.OrganizationRequest;
import uz.raqamli_talim.oneedu.model.ResponseDto;
import uz.raqamli_talim.oneedu.sevice.OrganizationService;

import java.util.List;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(security = {@SecurityRequirement(name = "bearer-key")})
    public ResponseDto create(@Valid @RequestBody OrganizationRequest request) {
        return organizationService.create(request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(security = {@SecurityRequirement(name = "bearer-key")})
    public OrganizationDto getById(@PathVariable Long id) {
        return organizationService.getById(id);
    }

    @GetMapping("/list")
    @PreAuthorize("isAuthenticated()")
    @Operation(security = {@SecurityRequirement(name = "bearer-key")})
    public List<OrganizationDto> getAllActive() {
        return organizationService.getAllActive();
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(security = {@SecurityRequirement(name = "bearer-key")})
    public Page<OrganizationDto> getAllAsPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search
    ) {
        return organizationService.getAllAsPage(page, size, search);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(security = {@SecurityRequirement(name = "bearer-key")})
    public ResponseDto update(@PathVariable Long id,
                              @Valid @RequestBody OrganizationRequest request) {
        return organizationService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(security = {@SecurityRequirement(name = "bearer-key")})
    public ResponseDto delete(@PathVariable Long id) {
        return organizationService.delete(id);
    }
}