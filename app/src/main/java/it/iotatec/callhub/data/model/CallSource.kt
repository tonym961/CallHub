package it.iotatec.callhub.data.model

import androidx.annotation.StringRes
import it.iotatec.callhub.R

/**
 * Where a call event came from. [PHONE] is read from the native CallLog;
 * everything else is inferred from that app's notifications. [labelRes] is a
 * localized string resource (IT / EN / DE).
 */
enum class CallSource(@StringRes val labelRes: Int, val packageName: String?) {
    PHONE(R.string.source_phone, null),
    WHATSAPP(R.string.source_whatsapp, "com.whatsapp"),
    WHATSAPP_BUSINESS(R.string.source_whatsapp_business, "com.whatsapp.w4b"),
    TELEGRAM(R.string.source_telegram, "org.telegram.messenger"),
    TELEGRAM_X(R.string.source_telegram_x, "org.thunderdog.challegram"),
    NEKOGRAM(R.string.source_nekogram, "tw.nekomimi.nekogram"),
    PLUS_MESSENGER(R.string.source_plus_messenger, "org.telegram.plus"),
    OTHER(R.string.source_other, null);

    companion object {
        fun fromPackage(pkg: String?): CallSource =
            entries.firstOrNull { it.packageName == pkg } ?: OTHER
    }
}
