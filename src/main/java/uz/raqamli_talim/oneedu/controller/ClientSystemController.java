package uz.raqamli_talim.oneedu.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.raqamli_talim.oneedu.domain.Organization;
import uz.raqamli_talim.oneedu.model.ClientSystemDto;
import uz.raqamli_talim.oneedu.model.ClientSystemRequest;
import uz.raqamli_talim.oneedu.model.ResponseDto;
import uz.raqamli_talim.oneedu.repository.OrganizationRepository;
import uz.raqamli_talim.oneedu.sevice.ClientSystemService;

import java.util.Arrays;
import java.util.List;


@RestController
@RequestMapping("/api/client-systems")
@RequiredArgsConstructor
public class ClientSystemController {

    private final ClientSystemService service;
    private final OrganizationRepository organizationRepository;

    // CREATE
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
            security = {@SecurityRequirement(name = "bearer-key")}
    )
    public ResponseDto create(@RequestBody ClientSystemRequest dto) {
        return service.create(dto);
    }

    // READ by ID  -> ClientSystemDto qaytaryapti (service shunaqa)
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            security = {@SecurityRequirement(name = "bearer-key")}
    )
    public ClientSystemDto getById(@PathVariable Long id) {
        return service.getById(id);
    }

    // READ by apiKey -> ClientSystemDto qaytaryapti
    @GetMapping("/by-api-key/{apiKey}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            security = {@SecurityRequirement(name = "bearer-key")}
    )
    public ClientSystemDto getByApiKey(@PathVariable String apiKey) {
        return service.getByApiKey(apiKey);
    }


    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
            security = {@SecurityRequirement(name = "bearer-key")}
    )
    public Page<ClientSystemDto> getAllAsPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.getAllAsPage(page, size);
    }

    // UPDATE
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            security = {@SecurityRequirement(name = "bearer-key")}
    )
    public ResponseDto update(@PathVariable Long id,
                              @RequestBody ClientSystemRequest dto) {
        return service.update(id, dto);
    }

    // DELETE (soft)
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            security = {@SecurityRequirement(name = "bearer-key")}
    )
    public ResponseDto deactivate(@PathVariable Long id) {
        return service.deactivate(id);
    }


    @GetMapping("/organizations")
    public List<Organization> getOwnerships() {
        return organizationRepository.findAll();
    }

    @PostMapping("/push-hemis/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            security = {@SecurityRequirement(name = "bearer-key")}
    )
    public ResponseDto postHemis(@PathVariable Long id) {
        return service.postHEMIS(id);
    }
}
