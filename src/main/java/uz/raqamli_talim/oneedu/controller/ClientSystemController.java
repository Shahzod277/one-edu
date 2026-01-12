package uz.raqamli_talim.oneedu.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.raqamli_talim.oneedu.model.ClientSystemDto;
import uz.raqamli_talim.oneedu.model.ClientSystemRequest;
import uz.raqamli_talim.oneedu.model.ResponseDto;
import uz.raqamli_talim.oneedu.sevice.ClientSystemService;


@RestController
@RequestMapping("/api/client-systems")
@RequiredArgsConstructor
public class ClientSystemController {

    private final ClientSystemService service;

    // CREATE
    @PostMapping
//    @PreAuthorize("isAuthenticated()")
//    @Operation(
//            security = {@SecurityRequirement(name = "bearer-key")}
//    )
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
    // READ ALL (PAGE) -> Page qaytaryapti
    // page ni siz service’da 1 dan boshlab qabul qilyapsiz (page>0 bo‘lsa -1)
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
}
