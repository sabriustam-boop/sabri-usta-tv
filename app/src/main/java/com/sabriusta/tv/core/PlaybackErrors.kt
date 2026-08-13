package com.sabriusta.tv.core

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Teknik hatalari kullaniciya gosterilecek Turkce metne cevirir. */
object PlaybackErrors {

    const val NO_INTERNET = "Internet baglantisi yok. Baglantinizi kontrol edip yeniden deneyin."
    const val INVALID_URL = "Yayin adresi gecersiz."
    const val SERVER_NOT_RESPONDING = "Sunucu yanit vermiyor. Yayin gecici olarak kapali olabilir."
    const val UNSUPPORTED_FORMAT = "Yayin bicimi desteklenmiyor."
    const val PLAYBACK_FAILED = "Icerik oynatilamadi."
    const val ACCESS_DENIED = "Erisim reddedildi. Bu yayin icin yetkiniz olmayabilir."
    const val STREAM_OFFLINE = "Yayin gecici olarak kapali."
    const val WIFI_ONLY = "Yalnizca Wi-Fi uzerinden oynatma acik. Mobil veride oynatmak icin Ayarlar'dan kapatin."

    /** Media3 PlaybackException.errorCode degerlerine gore mesaj uretir. */
    fun fromMedia3ErrorCode(errorCode: Int, hasNetwork: Boolean): String {
        if (!hasNetwork) return NO_INTERNET
        return when (errorCode) {
            2001, 2002 -> SERVER_NOT_RESPONDING          // IO_NETWORK_CONNECTION_FAILED / TIMEOUT
            2003 -> INVALID_URL                          // IO_INVALID_HTTP_CONTENT_TYPE
            2004 -> httpStatusMessage()                  // IO_BAD_HTTP_STATUS
            2005 -> STREAM_OFFLINE                       // IO_FILE_NOT_FOUND
            2006 -> ACCESS_DENIED                        // IO_NO_PERMISSION
            2007 -> "Sifresiz HTTP yayini engellendi. Ayarlar'dan izin verebilirsiniz."
            2008 -> STREAM_OFFLINE                       // IO_READ_POSITION_OUT_OF_RANGE
            3001, 3002, 3003, 3004 -> UNSUPPORTED_FORMAT // PARSING_*
            4001, 4002, 4003, 4004, 4005 -> UNSUPPORTED_FORMAT // DECODER_*
            6000, 6001, 6002, 6003, 6004, 6005, 6006, 6007, 6008 -> ACCESS_DENIED // DRM_*
            else -> PLAYBACK_FAILED
        }
    }

    private fun httpStatusMessage(): String = "Sunucu yayini reddetti veya adres artik gecerli degil."

    fun fromThrowable(t: Throwable, hasNetwork: Boolean): String = when {
        !hasNetwork -> NO_INTERNET
        t is UnknownHostException -> "Sunucu adresi bulunamadi."
        t is SocketTimeoutException -> SERVER_NOT_RESPONDING
        t is IOException -> SERVER_NOT_RESPONDING
        else -> t.message?.takeIf { it.isNotBlank() } ?: PLAYBACK_FAILED
    }
}
