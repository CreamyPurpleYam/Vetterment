-- ==============================================================================
-- VETterment: Veterinarian work schedules (vet_schedules)
-- ==============================================================================
-- WHAT THIS ADDS:
--
-- 1. public.is_pet_owner(user_id) helper — same style as is_staff() in
--    20261114000000_enable_rls_policies.sql. Needed so pet owners can be
--    granted read access below without exposing anything else.
--
-- 2. A `vet_schedules` table. The head veterinarian (or admin) assigns, per
--    specific date, which vet / head vet covers which shift with an exact
--    time range:
--      MORNING / AFTERNOON / GRAVEYARD + start_time / end_time as 'HH:mm'
--      text (24-hour, matching the app's time format). A graveyard range
--      like '22:00' -> '06:00' is allowed (end < start).
--    UNIQUE (vet_id, schedule_date, shift) — one entry per vet/day/shift, so
--    re-adding the same slot updates the existing row instead of duplicating.
--
-- 3. RLS: every staff member AND every pet owner can read schedules (pet
--    owners see "Veterinarians on Duty" on the booking screen). Only head
--    vet / admin can insert, update, or delete.
--
-- DEPENDS ON: public.is_staff() and public.is_head_vet_or_admin() from
-- 20261114000000_enable_rls_policies.sql. If that script has not been run
-- yet, run it first, then this one.
--
-- HOW TO RUN: Supabase Dashboard -> SQL Editor -> paste -> Run.
-- ==============================================================================

BEGIN;

-- ==============================================================================
-- 1. Helper: is the user a pet owner?
-- ==============================================================================
CREATE OR REPLACE FUNCTION public.is_pet_owner(user_id uuid)
RETURNS boolean
LANGUAGE sql
SECURITY DEFINER
STABLE
AS $$
  SELECT EXISTS (
    SELECT 1 FROM public.profiles
    WHERE id = user_id
      AND role = 'pet_owner'
  );
$$;

-- ==============================================================================
-- 2. VET_SCHEDULES TABLE
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.vet_schedules (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  vet_id uuid NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
  schedule_date date NOT NULL,
  shift text NOT NULL CHECK (shift IN ('MORNING', 'AFTERNOON', 'GRAVEYARD')),
  start_time text NOT NULL, -- 'HH:mm' 24-hour, matches the app's time format
  end_time text NOT NULL,   -- 'HH:mm' 24-hour; may be < start_time for graveyard
  notes text,
  created_by uuid REFERENCES public.profiles(id),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (vet_id, schedule_date, shift)
);

-- Keep updated_at honest without the app having to send a timestamp.
CREATE OR REPLACE FUNCTION public.set_vet_schedules_updated_at()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_vet_schedules_updated_at ON public.vet_schedules;
CREATE TRIGGER trg_vet_schedules_updated_at
BEFORE UPDATE ON public.vet_schedules
FOR EACH ROW
EXECUTE FUNCTION public.set_vet_schedules_updated_at();

ALTER TABLE public.vet_schedules ENABLE ROW LEVEL SECURITY;

-- ==============================================================================
-- 3. VET_SCHEDULES RLS
-- ==============================================================================
DROP POLICY IF EXISTS "Staff can view vet schedules" ON public.vet_schedules;
DROP POLICY IF EXISTS "Owners can view vet schedules" ON public.vet_schedules;
DROP POLICY IF EXISTS "Head vet can manage vet schedules" ON public.vet_schedules;

-- Staff can view (roster management, appointments context)
CREATE POLICY "Staff can view vet schedules"
ON public.vet_schedules FOR SELECT
TO authenticated
USING (public.is_staff(auth.uid()));

-- Pet owners can view (on-duty list on the booking screen)
CREATE POLICY "Owners can view vet schedules"
ON public.vet_schedules FOR SELECT
TO authenticated
USING (public.is_pet_owner(auth.uid()));

-- Head vet / admin manage (insert, update, delete)
CREATE POLICY "Head vet can manage vet schedules"
ON public.vet_schedules FOR ALL
TO authenticated
USING (public.is_head_vet_or_admin(auth.uid()))
WITH CHECK (public.is_head_vet_or_admin(auth.uid()));

COMMIT;

-- Verify afterwards:
-- select column_name, data_type, is_nullable from information_schema.columns where table_name = 'vet_schedules';
-- select policyname, cmd from pg_policies where tablename = 'vet_schedules';
