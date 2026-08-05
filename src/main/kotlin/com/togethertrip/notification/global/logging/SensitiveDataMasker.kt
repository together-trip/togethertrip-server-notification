package com.togethertrip.notification.global.logging

object SensitiveDataMasker {
    private const val MASK = "***"

    private val quotedSecret = Regex(
        "(?i)(\"(?:password|passwd|pwd|token|pushToken|deviceToken|authorization|secret|credential|apiKey|payload|body)\"\\s*:\\s*\")([^\"]*)(\")",
    )
    private val keyValueSecret = Regex(
        "(?i)(password|passwd|pwd|token|pushToken|deviceToken|authorization|secret|credential|apiKey|payload|body)=([^&\\s,)}]+)",
    )
    private val bearerToken = Regex("(?i)Bearer\\s+[^\\s,]+")
    private val phoneNumber = Regex("(?<!\\d)(?:\\+8210\\d{8}|010[- ]?\\d{4}[- ]?\\d{4})(?!\\d)")
    private val email = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")

    fun mask(value: String): String =
        value
            .replace(bearerToken, "Bearer $MASK")
            .replace(quotedSecret) { match ->
                "${match.groupValues[1]}$MASK${match.groupValues[3]}"
            }
            .replace(keyValueSecret) { match -> "${match.groupValues[1]}=$MASK" }
            .replace(phoneNumber, MASK)
            .replace(email, MASK)
}
