-- VETterment role restructure (2026-11-13)
-- Maps code roles to the documented 5-role system:
--   branch_manager  -> head_vet        (Head Veterinarian: clinic operations + highest staff role)
--   nurse           -> clinical_staff  (Clinical Staff)
--   assistant_vet   -> clinical_staff  (Clinical Staff)
--   vet             -> vet             (unchanged — Veterinarian)
--   pet_owner       -> pet_owner       (unchanged)
-- Admin is seeded manually later (not created here).
--
-- HOW TO RUN: Supabase Dashboard -> SQL Editor -> paste -> Run.

begin;

update public.profiles set role = 'head_vet'       where role = 'branch_manager';
update public.profiles set role = 'clinical_staff' where role in ('nurse', 'assistant_vet');

commit;

-- Verify afterwards:
-- select role, count(*) from public.profiles group by role order by role;
