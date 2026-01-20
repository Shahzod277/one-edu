package uz.raqamli_talim.oneedu.model;

public record UserPassportData(
        String pinfl,           // JShShIR (PINFL)
        String passportSerial   // Pasport seriya + raqam (AA1234567)
) {}
