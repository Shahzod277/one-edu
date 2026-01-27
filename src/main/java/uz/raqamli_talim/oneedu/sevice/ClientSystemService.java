package uz.raqamli_talim.oneedu.sevice;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import uz.raqamli_talim.oneedu.domain.ClientSystem;
import uz.raqamli_talim.oneedu.domain.Organization;
import uz.raqamli_talim.oneedu.model.ClientSystemDto;
import uz.raqamli_talim.oneedu.model.ClientSystemRequest;
import uz.raqamli_talim.oneedu.model.ResponseDto;
import uz.raqamli_talim.oneedu.repository.ClientSystemRepository;
import uz.raqamli_talim.oneedu.repository.OrganizationRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ClientSystemService {

    private final ClientSystemRepository repository;
    private final OrganizationRepository organizationRepository;
    private final RsaKeyService rsaKeyService;
    private final HemisAuthConfigService hemisAuthConfigService;


    // CREATE
    @Transactional
    public ResponseDto create(ClientSystemRequest dto) {

        // 1) Shu organization uchun active system bor-yo‘qligini tekshirish
//        if (repository.existsActiveByOrganizationId(dto.getOrganizationId())) {
//            throw new IllegalArgumentException("This organization already has an active client system (api key).");
//        }

        // 2) Organization ni xavfsiz olish
        Organization organization = organizationRepository.findById(dto.getOrganizationId())
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + dto.getOrganizationId()));

        // 3) ApiKey generatsiya + collision bo‘lsa qayta urinish
        String apiKey = generateUniqueApiKey();

        // 4) RSA keylar
        RsaKeyService.RsaKeys rsaKeys = rsaKeyService.generateRSA();

        ClientSystem cs = new ClientSystem();
        cs.setOrganization(organization);
        cs.setApiKey(apiKey);
        cs.setIsPushed(dto.getIsPushed());
        cs.setRedirectUrl(dto.getRedirectUrl());
        cs.setActive(dto.getActive() != null ? dto.getActive() : true);

        cs.setDomen(dto.getDomen());
        cs.setSystemName(dto.getSystemName());

        cs.setPublicKey(rsaKeys.publicKey());
        cs.setPrivateKey(rsaKeys.privateKey());

        ClientSystem saved = repository.save(cs);
        return ResponseDto.success(toDto(saved));
    }

    // Unique apiKey generator (3 marta urunadi)
    private String generateUniqueApiKey() {
        for (int i = 0; i < 3; i++) {
            String key = rsaKeyService.generateApiKey(); // yoki ApiKeyUtil.generateApiKey()
            if (!repository.existsByApiKey(key)) {       // repository'da boolean existsByApiKey(String apiKey) bo‘lsin
                return key;
            }
        }
        throw new IllegalStateException("Could not generate unique apiKey, please retry.");
    }


    // READ by ID
    @Transactional(readOnly = true)
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
    public Page<ClientSystemDto> getAllAsPage(
            int page,
            int size,
            Long organizationId,
            String search,
            Boolean active,
            Boolean isPushed
    ) {
        if (page > 0) page = page - 1;

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

        return repository.findAllWithFilter(
                        organizationId,
                        search,
                        active,
                        isPushed,
                        pageable
                )
                .map(this::toDto);
    }
    // UPDATE (apiKey, publicKey, privateKey o‘zgarmaydi)
    @Transactional
    public ResponseDto update(Long id, ClientSystemRequest dto) {

        ClientSystem cs = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ClientSystem not found: " + id));

        // apiKey/public/private SET QILINMAYDI (o‘zgarmaydi)

        cs.setRedirectUrl(dto.getRedirectUrl());

        if (dto.getActive() != null) {
            cs.setActive(dto.getActive());
        }

        cs.setDomen(dto.getDomen());
        cs.setIsPushed(dto.getIsPushed());
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

        dto.setRedirectUrl(cs.getRedirectUrl());
        dto.setActive(cs.getActive());

        dto.setDomen(cs.getDomen());
        dto.setSystemName(cs.getSystemName());
        dto.setIsPushed(cs.getIsPushed());
        if (!cs.getIsPushed()) {
            dto.setPrivateKey(cs.getPrivateKey());
            dto.setApiKey(cs.getApiKey());
        }
        dto.setOrganizationId(cs.getOrganization().getId());
        dto.setOrganization(cs.getOrganization().getName());

        dto.setIsUpdatedHemis(cs.getIsUpdatedHemis());
        dto.setIsUpdatedHemisTime(cs.getIsUpdatedHemisTime());

        return dto;
    }

    @Transactional
    public ResponseDto postHEMIS(Long id) {

        ClientSystem cs = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ClientSystem not found"));

        try {
            Boolean hemisSuccess = hemisAuthConfigService.setKeys(
                    cs.getPrivateKey(),                   // ⚠️ to‘g‘risi shu
                    cs.getApiKey(),
                    cs.getOrganization().getCode()
            );

            boolean updated = Boolean.TRUE.equals(hemisSuccess);

            cs.setIsUpdatedHemis(updated);
            cs.setIsUpdatedHemisTime(LocalDateTime.now());

            repository.save(cs);

            if (updated) {
                return ResponseDto.success("HEMIS muvaffaqiyatli qabul qildi");
            } else {
                return new ResponseDto(
                        HttpStatus.BAD_REQUEST.value(),
                        "HEMIS success=false qaytardi",
                        false
                );
            }

        } catch (Exception e) {
            cs.setIsUpdatedHemis(false);
            cs.setIsUpdatedHemisTime(LocalDateTime.now());
            repository.save(cs);

            return new ResponseDto(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "HEMIS xatolik: " + e.getMessage(),
                    false
            );
        }
    }


}
