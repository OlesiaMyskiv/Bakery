package com.bakery.Bakery.model;

public enum VerificationStatus {
    NONE,       // Звичайний клієнт (йому нічого не треба підтверджувати)
    PENDING,    // ЗСУ/ДСНС: документи завантажено, очікують перевірки адміном
    APPROVED,   // Адмін підтвердив документи
    REJECTED    // Адмін відхилив документи
}