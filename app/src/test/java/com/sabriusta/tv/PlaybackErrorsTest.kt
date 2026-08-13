package com.sabriusta.tv

import com.google.common.truth.Truth.assertThat
import com.sabriusta.tv.core.PlaybackErrors
import org.junit.Test
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class PlaybackErrorsTest {

    @Test
    fun `internet yoksa her hata kodu ayni mesaji verir`() {
        listOf(2001, 3001, 4001, 6000).forEach { code ->
            assertThat(PlaybackErrors.fromMedia3ErrorCode(code, hasNetwork = false))
                .isEqualTo(PlaybackErrors.NO_INTERNET)
        }
    }

    @Test
    fun `ayristirma hatalari desteklenmeyen bicim mesaji verir`() {
        assertThat(PlaybackErrors.fromMedia3ErrorCode(3001, hasNetwork = true))
            .isEqualTo(PlaybackErrors.UNSUPPORTED_FORMAT)
        assertThat(PlaybackErrors.fromMedia3ErrorCode(4003, hasNetwork = true))
            .isEqualTo(PlaybackErrors.UNSUPPORTED_FORMAT)
    }

    @Test
    fun `izin hatalari erisim reddedildi mesaji verir`() {
        assertThat(PlaybackErrors.fromMedia3ErrorCode(2006, hasNetwork = true))
            .isEqualTo(PlaybackErrors.ACCESS_DENIED)
        assertThat(PlaybackErrors.fromMedia3ErrorCode(6002, hasNetwork = true))
            .isEqualTo(PlaybackErrors.ACCESS_DENIED)
    }

    @Test
    fun `bilinmeyen kod genel mesaj verir`() {
        assertThat(PlaybackErrors.fromMedia3ErrorCode(9999, hasNetwork = true))
            .isEqualTo(PlaybackErrors.PLAYBACK_FAILED)
    }

    @Test
    fun `istisnalar turkce mesaja cevrilir`() {
        assertThat(PlaybackErrors.fromThrowable(UnknownHostException(), true))
            .isEqualTo("Sunucu adresi bulunamadi.")
        assertThat(PlaybackErrors.fromThrowable(SocketTimeoutException(), true))
            .isEqualTo(PlaybackErrors.SERVER_NOT_RESPONDING)
        assertThat(PlaybackErrors.fromThrowable(RuntimeException("hata"), false))
            .isEqualTo(PlaybackErrors.NO_INTERNET)
    }
}
