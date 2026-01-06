package uz.raqamli_talim.oneedu.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.raqamli_talim.oneedu.model.ClientSystemDto;
import uz.raqamli_talim.oneedu.model.ResponseDto;
import uz.raqamli_talim.oneedu.sevice.ClientSystemService;


@RestController
@RequestMapping("/api/client-systems")
@RequiredArgsConstructor
public class ClientSystemController {

    private final ClientSystemService service;

    // CREATE
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseDto create(@RequestBody ClientSystemDto dto) {
        return service.create(dto);
    }

    // READ by ID  -> ClientSystemDto qaytaryapti (service shunaqa)
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ClientSystemDto getById(@PathVariable Long id) {
        return service.getById(id);
    }

    // READ by apiKey -> ClientSystemDto qaytaryapti
    @GetMapping("/by-api-key/{apiKey}")
    @PreAuthorize("isAuthenticated()")
    public ClientSystemDto getByApiKey(@PathVariable String apiKey) {
        return service.getByApiKey(apiKey);
    }
    // READ ALL (PAGE) -> Page qaytaryapti
    // page ni siz service’da 1 dan boshlab qabul qilyapsiz (page>0 bo‘lsa -1)
    @GetMapping
//    @PreAuthorize("isAuthenticated()")
    public Page<ClientSystemDto> getAllAsPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.getAllAsPage(page, size);
    }

    // UPDATE
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseDto update(@PathVariable Long id,
                              @RequestBody ClientSystemDto dto) {
        return service.update(id, dto);
    }

    // DELETE (soft)
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseDto deactivate(@PathVariable Long id) {
        return service.deactivate(id);
    }
}
