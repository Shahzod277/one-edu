package uz.raqamli_talim.oneedu.api_integration.one_id_api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class OneIdResponseUserInfo {

    @JsonProperty("mid_name") private String midName;
    @JsonProperty("pport_expr_date") private String pportExprDate;
    @JsonProperty("pport_issue_place") private String pportIssuePlace;
    @JsonProperty("birth_date") private String birthDate;
    private String ctzn;
    @JsonProperty("per_adr") private String perAdr;
    @JsonProperty("sur_name") private String surName;
    private String valid;
    @JsonProperty("pport_issue_date") private String pportIssueDate;
    @JsonProperty("mob_phone_no") private String mobPhoneNo;
    @JsonProperty("user_type") private String userType;
    private String pin;
    private String tin;
    @JsonProperty("sess_id") private String sessId;
    private String gd;
    @JsonProperty("first_name") private String firstName;
    private String natn;
    private String email;
    @JsonProperty("birth_place") private String birthPlace;
    @JsonProperty("pport_no") private String pportNo;
    @JsonProperty("ret_cd") private String retCd;
    @JsonProperty("full_name") private String fullName;
    @JsonProperty("user_id") private String userId;
}
