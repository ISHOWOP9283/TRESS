package com.example.treemap.data.remote

import android.content.Context
import android.content.SharedPreferences
import com.example.treemap.BuildConfig

object SupabaseConfig {
    private const val PREF_NAME = "supabase_config_prefs"
    private const val KEY_SUPABASE_URL = "custom_supabase_url"
    private const val KEY_SUPABASE_ANON_KEY = "custom_supabase_anon_key"
    private const val KEY_LAST_SYNC_TIME = "last_sync_timestamp"

    private const val DEFAULT_SUPABASE_URL = "https://kxtraihcjocmzunvtdiq.supabase.co"
    private const val DEFAULT_SUPABASE_ANON_KEY = "sb_publishable_2ktLrSH8ViGgg87IYOIoLg_tkzaLrbV"

    /**
     * Retrieves the Supabase project URL from SharedPreferences, BuildConfig, or default
     */
    fun getSupabaseUrl(context: Context): String {
        val prefs = getPrefs(context)
        val customUrl = prefs.getString(KEY_SUPABASE_URL, "")?.trim()
        if (!customUrl.isNullOrBlank()) return customUrl

        val buildConfigUrl = try {
            val field = BuildConfig::class.java.getField("SUPABASE_URL")
            (field.get(null) as? String)?.trim().orEmpty()
        } catch (e: Exception) {
            ""
        }
        if (buildConfigUrl.isNotBlank()) return buildConfigUrl

        return DEFAULT_SUPABASE_URL
    }

    /**
     * Retrieves the Supabase Anon / Public Key from SharedPreferences, BuildConfig, or default
     */
    fun getSupabaseAnonKey(context: Context): String {
        val prefs = getPrefs(context)
        val customKey = prefs.getString(KEY_SUPABASE_ANON_KEY, "")?.trim()
        if (!customKey.isNullOrBlank()) return customKey

        val buildConfigKey = try {
            val field = BuildConfig::class.java.getField("SUPABASE_ANON_KEY")
            (field.get(null) as? String)?.trim().orEmpty()
        } catch (e: Exception) {
            ""
        }
        if (buildConfigKey.isNotBlank()) return buildConfigKey

        return DEFAULT_SUPABASE_ANON_KEY
    }

    fun isConfigured(context: Context): Boolean {
        val url = getSupabaseUrl(context)
        val key = getSupabaseAnonKey(context)
        return url.isNotBlank() && key.isNotBlank() && (url.startsWith("http://") || url.startsWith("https://"))
    }

    fun saveCustomCredentials(context: Context, url: String, anonKey: String) {
        val cleanUrl = url.trim().removeSuffix("/")
        getPrefs(context).edit()
            .putString(KEY_SUPABASE_URL, cleanUrl)
            .putString(KEY_SUPABASE_ANON_KEY, anonKey.trim())
            .apply()
    }

    fun clearCustomCredentials(context: Context) {
        getPrefs(context).edit()
            .remove(KEY_SUPABASE_URL)
            .remove(KEY_SUPABASE_ANON_KEY)
            .apply()
    }

    fun setLastSyncTime(context: Context, timeMillis: Long = System.currentTimeMillis()) {
        getPrefs(context).edit().putLong(KEY_LAST_SYNC_TIME, timeMillis).apply()
    }

    fun getLastSyncTime(context: Context): Long {
        return getPrefs(context).getLong(KEY_LAST_SYNC_TIME, 0L)
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    const val SQL_SETUP_SCRIPT = """-- =======================================================
-- MAPTREE: SUPABASE DATABASE & STORAGE INITIALIZATION
-- =======================================================

-- 1. Create Field Entries Table (Observations & Reports)
create table if not exists public.entries (
  id bigint primary key,
  title text not null default 'Monitoring Station',
  species text not null default 'Rhizophora mangle',
  category text not null default 'fair_growth',
  notes text,
  reporter text not null default 'Field Observer',
  zone_id text not null default 'zone_a',
  lat double precision not null,
  lng double precision not null,
  image_urls text default '',
  date bigint not null,
  created_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- 2. Create User Accounts Table (For Multi-Device Login)
create table if not exists public.user_accounts (
  id bigint primary key,
  email text not null,
  username text not null,
  password_hash text not null,
  display_name text not null,
  role text not null default 'VOLUNTEER',
  is_active boolean not null default true,
  is_google_account boolean not null default false,
  created_at bigint not null
);

-- 3. Enable RLS and Permissive Policies for Field Work
alter table public.entries enable row level security;
alter table public.user_accounts enable row level security;

create policy "Allow all access to entries" on public.entries for all using (true) with check (true);
create policy "Allow all access to user_accounts" on public.user_accounts for all using (true) with check (true);

-- 4. Create Public Storage Bucket for HD Field Photos
insert into storage.buckets (id, name, public) 
values ('tree-photos', 'tree-photos', true)
on conflict (id) do nothing;

create policy "Public Access to tree-photos" on storage.objects for all using (bucket_id = 'tree-photos') with check (bucket_id = 'tree-photos');
"""
}
