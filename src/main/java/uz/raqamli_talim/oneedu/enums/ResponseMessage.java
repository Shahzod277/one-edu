package uz.raqamli_talim.oneedu.enums;

import lombok.Getter;

@Getter
public enum ResponseMessage {

    NOT_FOUND("Topilmadi"),
    SUCCESSFULLY_SAVED("Muvaffaqiyatli saqlandi"),
    SUCCESSFULLY("Muvaffaqiyatli bajarildi"),
    ERROR_SAVED("Saqlashda xatolik"),
    SUCCESSFULLY_UPDATE("Muvaffaqiyatli yangilandi"),
    ERROR_UPDATE("Yangilashda xatolik"),
    SUCCESSFULLY_DELETED("Muvaffaqiyatli o'chirildi"),
    ERROR_DELETED("O'chirishda xatolik"),
    ALREADY_EXISTS("Allaqachon saqlangan"),
    SUCCESSFULLY_REJECTED("Muvaffaqiyatli bekor qilindi"),
    ERROR("Xatolik");
    private final String message;

    ResponseMessage(String message) {
        this.message = message;
    }

}
