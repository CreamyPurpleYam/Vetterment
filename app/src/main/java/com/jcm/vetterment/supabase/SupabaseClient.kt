package com.jcm.vetterment.supabase

import android.app.Application
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseClient {
    @Volatile
    private var _client: io.github.jan.supabase.SupabaseClient? = null

    val client: io.github.jan.supabase.SupabaseClient
        get() {
            if (_client == null) {
                throw IllegalStateException("SupabaseClient not initialized. Call initialize(Application) first.")
            }
            return _client!!
        }

    fun initialize(application: Application) {
        if (_client == null) {
            synchronized(this) {
                if (_client == null) {
                    _client = createSupabaseClient(
                        supabaseUrl = "https://igbecdbdlkkhldwwbcvs.supabase.co",
                        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImlnYmVjZGJkbGtraGxkd3diY3ZzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODAyMTIyNzYsImV4cCI6MjA5NTc4ODI3Nn0.mX-ihNnibaYTlnCy1DEUfBpVF0YFdRHS9yIvDjtYVEw"
                    ) {
                        install(Auth)
                        install(Postgrest)
                    }
                }
            }
        }
    }
}