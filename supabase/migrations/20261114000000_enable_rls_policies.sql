-- ==============================================================================
-- VETterment: Row Level Security (RLS) Policy Setup
-- ==============================================================================
-- This script enables RLS on core tables and sets up policies so:
-- 1. Head Veterinarian and Admin can manage and view all staff and records.
-- 2. Staff (Head Vet, Vet, Clinical Staff) can view/manage patients, cases, appointments.
-- 3. Pet Owners can only access their own profile, their pets, and their appointments.
--
-- NOTE: the live database stores owner_id columns as uuid, so every owner_id
-- comparison casts both sides to text (owner_id::text = auth.uid()::text) —
-- this works whether the column is uuid or text.
--
-- HOW TO RUN:
-- Open Supabase Dashboard -> Project -> SQL Editor -> New Query -> Paste & Run.
-- ==============================================================================

BEGIN;

-- 1. Helper Function to check if a user is Staff
CREATE OR REPLACE FUNCTION public.is_staff(user_id uuid)
RETURNS boolean
LANGUAGE sql
SECURITY DEFINER
STABLE
AS $$
  SELECT EXISTS (
    SELECT 1 FROM public.profiles
    WHERE id = user_id 
      AND role IN ('head_vet', 'vet', 'clinical_staff', 'admin', 'branch_manager')
  );
$$;

-- Helper Function to check if a user is Head Vet or Admin
CREATE OR REPLACE FUNCTION public.is_head_vet_or_admin(user_id uuid)
RETURNS boolean
LANGUAGE sql
SECURITY DEFINER
STABLE
AS $$
  SELECT EXISTS (
    SELECT 1 FROM public.profiles
    WHERE id = user_id 
      AND role IN ('head_vet', 'admin', 'branch_manager')
  );
$$;

-- ==============================================================================
-- 2. PROFILES TABLE RLS
-- ==============================================================================
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

-- Drop existing policies if any
DROP POLICY IF EXISTS "Users can read own profile" ON public.profiles;
DROP POLICY IF EXISTS "Staff can read all profiles" ON public.profiles;
DROP POLICY IF EXISTS "Head vet can insert/update staff profiles" ON public.profiles;
DROP POLICY IF EXISTS "Head vet can insert staff profiles" ON public.profiles;
DROP POLICY IF EXISTS "Head vet can update staff profiles" ON public.profiles;
DROP POLICY IF EXISTS "Users can update own profile" ON public.profiles;

-- Anyone authenticated can view their own profile
CREATE POLICY "Users can read own profile"
ON public.profiles FOR SELECT
TO authenticated
USING (id = auth.uid());

-- Staff and Head Vets can read all profiles (allows staff list, owner searches, patient owner info)
CREATE POLICY "Staff can read all profiles"
ON public.profiles FOR SELECT
TO authenticated
USING (public.is_staff(auth.uid()));

-- Users can update their own profile info (name, etc.)
CREATE POLICY "Users can update own profile"
ON public.profiles FOR UPDATE
TO authenticated
USING (id = auth.uid());

-- Head Vet and Admin can insert or update staff accounts
CREATE POLICY "Head vet can insert staff profiles"
ON public.profiles FOR INSERT
TO authenticated
WITH CHECK (public.is_head_vet_or_admin(auth.uid()) OR id = auth.uid());

CREATE POLICY "Head vet can update staff profiles"
ON public.profiles FOR UPDATE
TO authenticated
USING (public.is_head_vet_or_admin(auth.uid()) OR id = auth.uid());

-- ==============================================================================
-- 3. PETS TABLE RLS
-- ==============================================================================
ALTER TABLE public.pets ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Owners can view own pets" ON public.pets;
DROP POLICY IF EXISTS "Staff can view all pets" ON public.pets;
DROP POLICY IF EXISTS "Owners can create own pets" ON public.pets;
DROP POLICY IF EXISTS "Staff can create pets" ON public.pets;
DROP POLICY IF EXISTS "Owners can update own pets" ON public.pets;
DROP POLICY IF EXISTS "Staff can update pets" ON public.pets;

-- Owners can view their own pets
CREATE POLICY "Owners can view own pets"
ON public.pets FOR SELECT
TO authenticated
USING (owner_id::text = auth.uid()::text);

-- Staff can view all pets
CREATE POLICY "Staff can view all pets"
ON public.pets FOR SELECT
TO authenticated
USING (public.is_staff(auth.uid()));

-- Owners can register their own pets
CREATE POLICY "Owners can create own pets"
ON public.pets FOR INSERT
TO authenticated
WITH CHECK (owner_id::text = auth.uid()::text);

-- Staff can register walk-in pets for any owner
CREATE POLICY "Staff can create pets"
ON public.pets FOR INSERT
TO authenticated
WITH CHECK (public.is_staff(auth.uid()));

-- Owners can update basic info of their pets
CREATE POLICY "Owners can update own pets"
ON public.pets FOR UPDATE
TO authenticated
USING (owner_id::text = auth.uid()::text);

-- Staff can update pets (including clinical info like allergies, weight)
CREATE POLICY "Staff can update pets"
ON public.pets FOR UPDATE
TO authenticated
USING (public.is_staff(auth.uid()));

-- ==============================================================================
-- 4. APPOINTMENTS TABLE RLS
-- ==============================================================================
ALTER TABLE public.appointments ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Owners can view own appointments" ON public.appointments;
DROP POLICY IF EXISTS "Staff can view all appointments" ON public.appointments;
DROP POLICY IF EXISTS "Owners can create own appointments" ON public.appointments;
DROP POLICY IF EXISTS "Staff can manage appointments" ON public.appointments;
DROP POLICY IF EXISTS "Staff can insert appointments" ON public.appointments;
DROP POLICY IF EXISTS "Staff can update appointments" ON public.appointments;

-- Owners view own appointments
CREATE POLICY "Owners can view own appointments"
ON public.appointments FOR SELECT
TO authenticated
USING (owner_id::text = auth.uid()::text);

-- Staff view all appointments
CREATE POLICY "Staff can view all appointments"
ON public.appointments FOR SELECT
TO authenticated
USING (public.is_staff(auth.uid()));

-- Owners create appointment requests
CREATE POLICY "Owners can create own appointments"
ON public.appointments FOR INSERT
TO authenticated
WITH CHECK (owner_id::text = auth.uid()::text);

-- Staff can insert, update (confirm, check-in, cancel) appointments
CREATE POLICY "Staff can insert appointments"
ON public.appointments FOR INSERT
TO authenticated
WITH CHECK (public.is_staff(auth.uid()));

CREATE POLICY "Staff can update appointments"
ON public.appointments FOR UPDATE
TO authenticated
USING (public.is_staff(auth.uid()));

-- ==============================================================================
-- 5. CASES AND CASE PROGRESS TABLE RLS
-- ==============================================================================
ALTER TABLE public.cases ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.case_progress ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Staff can view all cases" ON public.cases;
DROP POLICY IF EXISTS "Staff can manage cases" ON public.cases;
DROP POLICY IF EXISTS "Staff can insert cases" ON public.cases;
DROP POLICY IF EXISTS "Staff can update cases" ON public.cases;
DROP POLICY IF EXISTS "Owners can view cases of their pets" ON public.cases;

-- Staff can view and manage all cases
CREATE POLICY "Staff can view all cases"
ON public.cases FOR SELECT
TO authenticated
USING (public.is_staff(auth.uid()));

CREATE POLICY "Staff can insert cases"
ON public.cases FOR INSERT
TO authenticated
WITH CHECK (public.is_staff(auth.uid()));

CREATE POLICY "Staff can update cases"
ON public.cases FOR UPDATE
TO authenticated
USING (public.is_staff(auth.uid()));

-- Pet owners can view cases belonging to their pets
CREATE POLICY "Owners can view cases of their pets"
ON public.cases FOR SELECT
TO authenticated
USING (
  EXISTS (
    SELECT 1 FROM public.pets
    WHERE pets.id = cases.pet_id
      AND pets.owner_id::text = auth.uid()::text
  )
);

-- Case Progress
DROP POLICY IF EXISTS "Staff can view all case progress" ON public.case_progress;
DROP POLICY IF EXISTS "Staff can insert case progress" ON public.case_progress;
DROP POLICY IF EXISTS "Owners can view progress of their pets" ON public.case_progress;

CREATE POLICY "Staff can view all case progress"
ON public.case_progress FOR SELECT
TO authenticated
USING (public.is_staff(auth.uid()));

CREATE POLICY "Staff can insert case progress"
ON public.case_progress FOR INSERT
TO authenticated
WITH CHECK (public.is_staff(auth.uid()));

CREATE POLICY "Owners can view progress of their pets"
ON public.case_progress FOR SELECT
TO authenticated
USING (
  EXISTS (
    SELECT 1 FROM public.cases
    JOIN public.pets ON pets.id = cases.pet_id
    WHERE cases.id = case_progress.case_id
      AND pets.owner_id::text = auth.uid()::text
  )
);

COMMIT;
