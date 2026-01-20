package uz.raqamli_talim.oneedu.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

@Getter
@Setter
public class UniversityApiUrlsResponse {
    private String code;
    private String name;
    private Long tin;
    private String api_url;
    private String student_url;
    private String employee_url;
    private String university_type;
    private String version_type;
    private String logo;
}