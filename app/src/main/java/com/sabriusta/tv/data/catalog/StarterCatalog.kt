package com.sabriusta.tv.data.catalog

import android.content.Context
import com.sabriusta.tv.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class CatalogSource(
    val id: String,
    val name: String,
    val publisher: String = "",
    val url: String,
    val logo: String = "",
    val country: String = "",
    val category: String = "Genel",
    val type: String = "TV",
    val sourcePage: String = "",
    val license: String = ""
)

@Serializable
data class CatalogFile(
    val version: Int = 1,
    val note: String = "",
    val sources: List<CatalogSource> = emptyList()
)

/**
 * Baslangic katalogu tek bir dosyada tutulur (res/raw/starter_catalog.json).
 * Kaynaklar degisirse yalnizca bu dosya guncellenir; uygulama kodu etkilenmez.
 */
@Singleton
class StarterCatalog @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun load(): CatalogFile = withContext(Dispatchers.IO) {
        try {
            val text = context.resources.openRawResource(R.raw.starter_catalog)
                .bufferedReader()
                .use { it.readText() }
            json.decodeFromString(CatalogFile.serializer(), text)
        } catch (e: Exception) {
            // Katalog okunamazsa uygulama bos katalogla acilir, cokmez.
            CatalogFile()
        }
    }
}
