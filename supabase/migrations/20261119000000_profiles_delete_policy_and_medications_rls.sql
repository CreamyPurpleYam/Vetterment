-- ==============================================================================
-- VETterment: profiles DELETE policy + medications RLS
-- ==============================================================================
-- WHAT THIS FIXES (found during the app-vs-database consistency audit):
--
-- 1. profiles had no DELETE policy, so "Staff List -> Delete" in the app
--    always failed with a row-level-security error even for the head vet.
--
-- 2. medications had no RLS policies tracked in the repo. Without them:
--    - if RLS was enabled: pet owners saw empty medication lists and staff
--      prescribing failed silently;
--    - if RLS was disabled: any authenticated user could read all medications.
--
-- 3. appointments had no owner UPDATE policy, so "Cancel Appointment" on the
--    pet-owner side always failed (the only UPDATE policy was staff-only).
--
--    owner_id comparisons cast both sides to text: the live DB stores
--    owner_id columns as uuid, and this form also works if any table uses
--    text.
--
-- 4. pet owners fetch the assigned vet's profile on case details
--    (PetOwnerCaseDetailScreen), but with RLS on they could only read their
--    own profile — the vet name would come back blank.
--
-- HOW TO RUN: Supabase Dashboard -> SQL Editor -> paste -> Run.
--
-- KNOWN LIMITATION (documented, not fixed here): deleting a profile row does
-- NOT delete the matching auth.users row. The email stays registered and that
-- account can still log in, but with no profile the app shows "Unable to
-- determine user role." Any pets owned by the deleted profile are also left
-- without an owner. Prefer deactivating accounts over deleting where possible.
-- ==============================================================================

BEGIN;

-- ==============================================================================
-- 1. PROFILES: allow head vet / admin to delete staff profiles
-- ==============================================================================
DROP POLICY IF EXISTS "Head vet can delete staff profiles" ON public.profiles;

CREATE POLICY "Head vet can delete staff profiles"
ON public.profiles FOR DELETE
TO authenticated
USING (public.is_head_vet_or_admin(auth.uid()));

-- ==============================================================================
-- 2. MEDICATIONS: enable RLS and add policies
--    Staff manage medications; pet owners can read medications of their own
--    pets' cases (via cases -> pets -> owner_id).
-- ==============================================================================
ALTER TABLE public.medications ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Staff can view medications" ON public.medications;
DROP POLICY IF EXISTS "Staff can insert medications" ON public.medications;
DROP POLICY IF EXISTS "Staff can update medications" ON public.medications;
DROP POLICY IF EXISTS "Owners can view medications of their pets" ON public.medications;

CREATE POLICY "Staff can view medications"
ON public.medications FOR SELECT
TO authenticated
USING (public.is_staff(auth.uid()));

CREATE POLICY "Staff can insert medications"
ON public.medications FOR INSERT
TO authenticated
WITH CHECK (public.is_staff(auth.uid()));

CREATE POLICY "Staff can update medications"
ON public.medications FOR UPDATE
TO authenticated
USING (public.is_staff(auth.uid()));

CREATE POLICY "Owners can view medications of their pets"
ON public.medications FOR SELECT
TO authenticated
USING (
  EXISTS (
    SELECT 1
    FROM public.cases
    JOIN public.pets ON pets.id = cases.pet_id
    WHERE cases.id = medications.case_id
      AND pets.owner_id::text = auth.uid()::text
  )
);

-- ==============================================================================
-- 3. APPOINTMENTS: allow owners to cancel their own appointments
--    The pet-owner Cancel flow (PetOwnerAppointmentDetailScreen) performs an
--    UPDATE, but the base RLS script only granted UPDATE to staff. Owners may
--    change their own appointment only while it is still cancellable
--    (PENDING / CONFIRMED / RESCHEDULED) and only into CANCELLED — nothing
--    else, so owners can never mark an appointment COMPLETED or edit it after
--    check-in.
-- ==============================================================================
DROP POLICY IF EXISTS "Owners can cancel own appointments" ON public.appointments;

CREATE POLICY "Owners can cancel own appointments"
ON public.appointments FOR UPDATE
TO authenticated
USING (
  owner_id::text = auth.uid()::text
  AND status IN ('PENDING', 'CONFIRMED', 'RESCHEDULED')
)
WITH CHECK (
  owner_id::text = auth.uid()::text
  AND status = 'CANCELLED'
);

-- ==============================================================================
-- 4. PROFILES: allow any authenticated user to read STAFF profiles
--    Pet owners see the assigned veterinarian on case details
--    (PetOwnerCaseDetailScreen reads profiles by id). This only exposes rows
--    whose role is a staff role — owners still cannot read other owners.
--    Legacy role values are included to match is_staff().
-- ==============================================================================
DROP POLICY IF EXISTS "Users can view staff profiles" ON public.profiles;

CREATE POLICY "Users can view staff profiles"
ON public.profiles FOR SELECT
TO authenticated
USING (
  role IN ('head_vet', 'vet', 'clinical_staff', 'admin',
           'branch_manager', 'nurse', 'assistant_vet')
);

COMMIT;

-- Verify afterwards:
-- select policyname, cmd, roles from pg_policies where tablename = 'medications';
-- select policyname, cmd from pg_policies where tablename = 'profiles' and cmd = 'DELETE';
-- select policyname, cmd from pg_policies where tablename = 'appointments';
