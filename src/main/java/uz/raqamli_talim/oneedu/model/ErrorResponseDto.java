package uz.raqamli_talim.oneedu.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ErrorResponseDto {

    private int code;
    private String message;
    private boolean success = false;
    private LocalDateTime timeStamp = LocalDateTime.now();

    public ErrorResponseDto(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
