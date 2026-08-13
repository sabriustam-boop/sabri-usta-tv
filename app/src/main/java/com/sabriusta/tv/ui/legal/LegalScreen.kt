package com.sabriusta.tv.ui.legal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sabriusta.tv.ui.theme.Altin

@Composable
fun LegalScreen(viewModel: LegalViewModel = hiltViewModel()) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Sabri Usta TV",
                style = MaterialTheme.typography.displaySmall,
                color = Altin,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "TV • Radyo • Film",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(28.dp))
            Text(
                text = "Yasal Kullanim Bildirimi",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Sabri Usta TV yalnizca resmi, ucretsiz, kamu mali veya kullanicinin erisim hakkina " +
                    "sahip oldugu yayinlari oynatmak amaciyla gelistirilmistir. Kullanici ekledigi yayin " +
                    "kaynaklarinin kullanim hakkindan sorumludur.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Uygulama icinde korsan yayin listesi, izinsiz film sitesi veya DRM asma araci " +
                    "bulunmaz. Eklediginiz listelerin icerigi tarafimizca saglanmaz ve denetlenmez.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = viewModel::accept,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Okudum, kabul ediyorum", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Kabul edilmeden ozel M3U listesi ekleme ekrani acilmaz.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
