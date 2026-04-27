package uz.raqamli_talim.oneedu.sevice;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.raqamli_talim.oneedu.domain.Organization;
import uz.raqamli_talim.oneedu.model.OrganizationDto;
import uz.raqamli_talim.oneedu.model.OrganizationRequest;
import uz.raqamli_talim.oneedu.model.ResponseDto;
import uz.raqamli_talim.oneedu.repository.OrganizationRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;

    @Transactional
    public ResponseDto create(OrganizationRequest request) {
        if (organizationRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Bu code allaqachon mavjud: " + request.getCode());
        }

        Organization organization = new Organization();
        organization.setCode(request.getCode());
        organization.setName(request.getName());
        organization.setOwnership(request.getOwnership());

        Organization saved = organizationRepository.save(organization);
        return ResponseDto.success(toDto(saved));
    }

    @Transactional(readOnly = true)
    public OrganizationDto getById(Long id) {
        Organization organization = organizationRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new EntityNotFoundException("Organization topilmadi: " + id));
        return toDto(organization);
    }

    @Transactional(readOnly = true)
    public List<OrganizationDto> getAllActive() {
        return organizationRepository.findAllByIsActiveTrue()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<OrganizationDto> getAllAsPage(int page, int size, String search) {
        if (page > 0) page = page - 1;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        return organizationRepository.findAllWithFilter(search, pageable)
                .map(this::toDto);
    }

    @Transactional
    public ResponseDto update(Long id, OrganizationRequest request) {
        Organization organization = organizationRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new EntityNotFoundException("Organization topilmadi: " + id));

        // code o'zgargan bo'lsa, duplicate tekshirish
        if (!organization.getCode().equals(request.getCode())
                && organizationRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Bu code allaqachon mavjud: " + request.getCode());
        }

        organization.setCode(request.getCode());
        organization.setName(request.getName());
        organization.setOwnership(request.getOwnership());

        Organization saved = organizationRepository.save(organization);
        return ResponseDto.success(toDto(saved));
    }

    @Transactional
    public ResponseDto delete(Long id) {
        Organization organization = organizationRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new EntityNotFoundException("Organization topilmadi: " + id));

        organization.setIsActive(false);
        organizationRepository.save(organization);
        return ResponseDto.success();
    }

    private OrganizationDto toDto(Organization org) {
        OrganizationDto dto = new OrganizationDto();
        dto.setId(org.getId());
        dto.setCode(org.getCode());
        dto.setName(org.getName());
        dto.setOwnership(org.getOwnership());
        return dto;
    }
}
