package com.aifinancial.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.aifinancial.mobile.pairing.AuthTokenStore
import com.aifinancial.mobile.ui.AppNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // A plain EncryptedSharedPreferences read is fast, local, and
    // synchronous enough to check once at startup without a loading state —
    // unlike Room/network reads, this is not something the UI should show a
    // spinner for.
    @Inject
    lateinit var authTokenStore: AuthTokenStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    AppNavHost(startPaired = authTokenStore.isPaired())
                }
            }
        }
    }
}
