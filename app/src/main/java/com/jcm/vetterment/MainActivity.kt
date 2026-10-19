package com.jcm.vetterment

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jcm.vetterment.ui.theme.VettermentTheme
import com.jcm.vetterment.navigation.appNavigation
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

val supabase = createSupabaseClient(
    supabaseUrl = "https://igbecdbdlkkhldwwbcvs.supabase.co",
    supabaseKey = "sb_publishable_cITxS1pawoAJtITjZ75QvQ_mI1aE7qw"
) {
    install(Postgrest)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VettermentTheme {
                appNavigation()
            }
        }
    }
}