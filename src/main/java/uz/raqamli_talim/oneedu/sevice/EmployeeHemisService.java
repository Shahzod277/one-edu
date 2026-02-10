package uz.raqamli_talim.oneedu.sevice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;
import uz.raqamli_talim.oneedu.api_integration.one_id_api.OneIdResponseUserInfo;
import uz.raqamli_talim.oneedu.api_integration.one_id_api.OneIdServiceApiAdmin;
import uz.raqamli_talim.oneedu.api_integration.one_id_api.OneIdTokenResponse;
import uz.raqamli_talim.oneedu.exception.NotFoundException;
import uz.raqamli_talim.oneedu.model.UniversityApiUrlsResponse;
import uz.raqamli_talim.oneedu.repository.ClientSystemRepository;

import java.net.URI;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeHemisService {

    private final OneIdServiceApiAdmin oneIdServiceApiAdmin;
    private final ClientSystemRepository clientSystemRepository;
    private final AuthService authService;
    private final HemisAuthConfigService hemisAuthConfigService;

    @Transactional
    public URI oneIdAdminSignInAndRedirect(String code, String apiKey,String universityCode,String type) {

        var clientSystem = clientSystemRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new NotFoundException("Sizga ruxsat yo‘q"));

        if (!Boolean.TRUE.equals(clientSystem.getActive())) {
            authService.saveAudit(clientSystem, null, null, true, "Sizga ruxsat yo‘q", null);
            throw new NotFoundException("Sizga ruxsat yo‘q");
        }

        // ✅ catchda ham ishlatish uchun oldindan e’lon qilamiz
        var userInfoHolder = new Object[]{null}; // (Java var uchun kichik hack)
        var universityResponse = new Object[]{null}; // (Java var uchun kichik hack)
        // yaxshiroq variant: OneIdResponseUserInfo userInfo = null; (agar type import qilsangiz)

        try {
            OneIdTokenResponse token = oneIdServiceApiAdmin.getAccessAndRefreshToken(code);
            var userInfo = oneIdServiceApiAdmin.getUserInfo(token.getAccess_token());
            userInfoHolder[0] = userInfo;
            UniversityApiUrlsResponse universityApiUrlsResponse = hemisAuthConfigService.getUniversityBaseByPinflEmployee(userInfo.getPin(),universityCode);
            universityResponse[0] = universityApiUrlsResponse;

            if (universityApiUrlsResponse == null) {


            }
            var tokens = hemisAuthConfigService.eduIdLoginEmployee(userInfo.getPin(), userInfo.getPportNo(), universityApiUrlsResponse,type);

            URI callbackUri = UriComponentsBuilder
                    .fromUriString("https://tyutor.hemis.uz/auth/one-id-callback")
                    .queryParam("token", tokens.token())
                    .queryParam("refreshToken", tokens.refreshToken())
                    .queryParam("api_url", tokens.apiUrl())
                    .build(true)
                    .toUri();

            assert universityApiUrlsResponse != null;
            authService.saveAudit(clientSystem, userInfo.getPin(), userInfo.getPportNo(), false, null, universityApiUrlsResponse.getCode());
            return callbackUri;

        } catch (Exception e) {
            String msg = authService.extractHemisErrorMessage(e);

            // ✅ userInfo bor bo‘lsa — auditga yozamiz
            String pin = null;
            String serial = null;

            if (userInfoHolder[0] != null) {
                var ui = (OneIdResponseUserInfo) userInfoHolder[0];
                pin = ui.getPin();
                serial = ui.getPportNo();
            }
            if (universityResponse[0] != null) {
                var ui = (UniversityApiUrlsResponse) universityResponse[0];
                universityCode = ui.getCode();
            }

            authService.saveAudit(clientSystem, pin, serial, true, msg, universityCode);

            return UriComponentsBuilder
                    .fromUriString("https://tyutor.hemis.uz/auth/notFound")
                    .build(true)
                    .toUri();
        }
    }

}
