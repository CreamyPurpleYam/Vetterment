package com.jcm.vetterment

import android.app.Application
import com.jcm.vetterment.supabase.SupabaseClient

class VettermentApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Supabase client with Android context for auth persistence
        // Auth module automatically persists/restores session via SharedPreferences
        SupabaseClient.initialize(this)
    }
}
