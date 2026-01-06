package uz.raqamli_talim.oneedu.sevice;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.raqamli_talim.oneedu.domain.ClientSystem;
import uz.raqamli_talim.oneedu.model.ClientSystemDto;
import uz.raqamli_talim.oneedu.model.ResponseDto;
import uz.raqamli_talim.oneedu.repository.ClientSystemRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientSystemService {

    private final ClientSystemRepository repository;

    // CREATE
    @Transactional
    public ResponseDto create(ClientSystemDto dto) {

        repository.findByApiKey(dto.getApiKey()).ifPresent(c -> {
            throw new IllegalArgumentException("apiKey already exists and active");
        });

        ClientSystem cs = new ClientSystem();
        cs.setApiKey(dto.getApiKey());
        cs.setRedirectUrl(dto.getRedirectUrl());
        cs.setPostCallbackUrl(dto.getPostCallbackUrl());
        cs.setActive(dto.getActive() != null ? dto.getActive() : true);

        // qo'shimcha fieldlar
        cs.setOrg(dto.getOrg());
        cs.setOrgInn(dto.getOrgInn());
        cs.setDomen(dto.getDomen());
        cs.setSystemName(dto.getSystemName());

        ClientSystem saved = repository.save(cs);
        return ResponseDto.success(toDto(saved));
    }


    // READ by ID
    public ClientSystemDto getById(Long id) {
        ClientSystem cs = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ClientSystem not found"));

        return toDto(cs);
    }

    // READ by apiKey (FAQAT ACTIVE)
    public ClientSystemDto getByApiKey(String apiKey) {
        ClientSystem cs = repository.findByApiKey(apiKey)
                .orElseThrow(() -> new EntityNotFoundException("Active ClientSystem not found"));

        return toDto(cs);
    }

    @Transactional(readOnly = true)
    public Page<ClientSystemDto> getAllAsPage(int page, int size) {
        if (page > 0) page = page - 1;
        PageRequest pageRequest = PageRequest.of(page, size);

        return repository.findAllActive(pageRequest)
                .map(this::toDto);
    }

    // UPDATE
    @Transactional
    public ResponseDto update(Long id, ClientSystemDto dto) {

        ClientSystem cs = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ClientSystem not found"));

        // apiKey boshqa active tizimda band emasligini tekshirish
        repository.findByApiKey(dto.getApiKey()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new IllegalArgumentException("apiKey already exists and active");
            }
        });

        // hamma field set (PUT kabi)
        cs.setApiKey(dto.getApiKey());
        cs.setRedirectUrl(dto.getRedirectUrl());
        cs.setPostCallbackUrl(dto.getPostCallbackUrl());
        cs.setActive(dto.getActive());

        cs.setOrg(dto.getOrg());
        cs.setOrgInn(dto.getOrgInn());
        cs.setDomen(dto.getDomen());
        cs.setSystemName(dto.getSystemName());

        ClientSystem saved = repository.save(cs);
        return ResponseDto.success(toDto(saved));
    }


    // ❌ DELETE (soft delete)
    @Transactional
    public ResponseDto deactivate(Long id) {
        ClientSystem cs = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ClientSystem not found"));

        cs.setActive(false);
        repository.save(cs);

        return ResponseDto.success(); // data qaytarmasin desang
        // yoki: return ResponseDto.success(toDto(cs));
    }

    private ClientSystemDto toDto(ClientSystem cs) {
        ClientSystemDto dto = new ClientSystemDto();
        dto.setId(cs.getId());
        dto.setApiKey(cs.getApiKey());
        dto.setRedirectUrl(cs.getRedirectUrl());
        dto.setPostCallbackUrl(cs.getPostCallbackUrl());
        dto.setActive(cs.getActive());
        return dto;
    }
}
