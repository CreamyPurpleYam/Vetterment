-- ==============================================================================
-- VETterment: database verification queries (READ-ONLY — safe to run)
-- ==============================================================================
-- These checks come from the app-vs-database consistency audit. The repo does
-- not contain the DDL for the core tables, so the live database is the only
-- source of truth for the items below. Run each section in Supabase
-- Dashboard -> SQL Editor and compare the output against the expectations.
--
-- HOW TO RUN: Supabase Dashboard -> SQL Editor -> paste -> Run.
-- ==============================================================================

-- ==============================================================================
-- 1. REGISTRATION SAFETY
-- ==============================================================================

-- 1a. Triggers on auth.users — a handle_new_user trigger would conflict with
--     the app's manual profile insert on registration.
--     EXPECTED: no rows (unless the project intentionally has one).
select tgname, tgenabled
from pg_trigger
where tgrelid = 'auth.users'::regclass
  and not tgisinternal;

-- 1b. Email confirmation setting (cannot be queried via SQL — check the UI):
--     Dashboard -> Authentication -> Providers -> Email -> "Confirm email" toggle.
--     EXPECTED: OFF. If ON, new registrations never create a session, so the
--     app cannot insert the profile row (registerScreen shows an error now,
--     but those accounts can never log in).

-- 1c. Any auth users WITHOUT a profile row — these are dead-end accounts:
--     login shows "Unable to determine user role." and re-registering is
--     blocked with "This email is already registered."
--     EXPECTED: no rows.
select u.id, u.email, u.created_at
from auth.users u
left join public.profiles p on p.id = u.id
where p.id is null;

-- ==============================================================================
-- 2. PROFILES TABLE SHAPE (registration inserts id, full_name, email, role)
-- ==============================================================================
-- Any NOT NULL column without a default beyond the four above breaks
-- registration.
select column_name, data_type, is_nullable, column_default
from information_schema.columns
where table_schema = 'public'
  and table_name = 'profiles'
order by ordinal_position;

-- Role values currently in use (should be: admin, head_vet, vet,
-- clinical_staff, pet_owner — legacy values are acceptable but should be
-- migrated with 20261113000000_role_restructure.sql):
select role, count(*)
from public.profiles
group by role
order by role;

-- ==============================================================================
-- 3. create_staff_account RPC (staff account creation calls this)
-- ==============================================================================
-- EXPECTED: one row whose definition creates the auth user, inserts a profile
-- with the given role, validates the role against head_vet/vet/clinical_staff
-- (legacy values would mean new roles are rejected), and returns the new
-- profile id as text. If this returns no rows, staff creation is broken.
select p.proname,
       pg_get_function_identity_arguments(p.oid) as args,
       pg_get_function_result(p.oid) as result_type,
       pg_get_functiondef(p.oid) as definition
from pg_proc p
join pg_namespace n on n.oid = p.pronamespace
where n.nspname = 'public'
  and p.proname = 'create_staff_account';

-- ==============================================================================
-- 4. patient_list VIEW (staff patient list reads this)
-- ==============================================================================
-- EXPECTED: a view exposing pet_id, pet_name, species, breed, owner_id,
-- owner_name, active_status, photo_url. If it returns no rows, the staff
-- patient list is always empty.
select pg_get_viewdef('public.patient_list'::regclass, true) as view_definition;

-- ==============================================================================
-- 5. MEDICATIONS RLS (after running 20261119000000 migration)
-- ==============================================================================
-- EXPECTED: relrowsecurity = true, and 4 policies:
--   Staff can view/insert/update medications, Owners can view medications of
--   their pets.
select c.relrowsecurity as rls_enabled
from pg_class c
join pg_namespace n on n.oid = c.relnamespace
where n.nspname = 'public' and c.relname = 'medications';

select policyname, cmd, roles, qual, with_check
from pg_policies
where schemaname = 'public' and tablename = 'medications';

-- ==============================================================================
-- 6. CHECK CONSTRAINTS (must cover the app's values)
-- ==============================================================================
-- App writes: appointments.status IN (PENDING, CONFIRMED, CANCELLED, NO_SHOW,
-- CHECKED_IN, COMPLETED); appointments.appointment_type IN (CONSULTATION,
-- CONFINEMENT, SURGERY); cases.case_type IN (CONFINEMENT, SURGERY, EMERGENCY +
-- legacy rows); cases.current_status per CaseStages; pets.species IN (DOG,
-- CAT); pets.sex IN (MALE, FEMALE, UNKNOWN). A CHECK narrower than these
-- breaks the corresponding flow.
select conrelid::regclass as table_name,
       conname,
       pg_get_constraintdef(oid) as definition
from pg_constraint
where contype = 'c'
  and connamespace = 'public'::regnamespace
  and conrelid in (
    'public.appointments'::regclass,
    'public.cases'::regclass,
    'public.case_progress'::regclass,
    'public.pets'::regclass,
    'public.profiles'::regclass,
    'public.medications'::regclass
  );

-- ==============================================================================
-- 7. TIMESTAMP DEFAULTS (app never writes created_at / updated_at)
-- ==============================================================================
-- EXPECTED: every NOT NULL created_at / updated_at / recorded_at column has a
-- default (usually now()); recorded_at on case_progress needs one too.
select table_name, column_name, is_nullable, column_default
from information_schema.columns
where table_schema = 'public'
  and table_name in ('profiles', 'pets', 'appointments', 'cases', 'case_progress', 'medications')
  and column_name in ('created_at', 'updated_at', 'recorded_at')
order by table_name, column_name;

-- ==============================================================================
-- 8. pet owner_id type
-- ==============================================================================
-- CONFIRMED 2026-11-19: live DB stores owner_id columns as uuid (the first
-- run of the RLS script failed with "operator does not exist: uuid = text").
-- The RLS scripts now compare owner_id::text = auth.uid()::text, which works
-- for either type. This query just documents the actual types.
select table_name, column_name, data_type
from information_schema.columns
where table_schema = 'public'
  and table_name in ('pets', 'appointments')
  and column_name in ('owner_id', 'created_by');
