package com.jcm.vetterment.supabase

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseClient
{
    val client = createSupabaseClient(
        supabaseUrl = "https://igbecdbdlkkhldwwbcvs.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImlnYmVjZGJkbGtraGxkd3diY3ZzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODAyMTIyNzYsImV4cCI6MjA5NTc4ODI3Nn0.mX-ihNnibaYTlnCy1DEUfBpVF0YFdRHS9yIvDjtYVEw"
    )
    {
        install(Auth)
        install(Postgrest)
    }
}