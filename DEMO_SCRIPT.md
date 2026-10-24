# VETterment App - Demo Presentation Script

## Overview
This script demonstrates the complete user journey across all three roles: **Pet Owner → Staff → Branch Manager**.
Each section highlights working features while avoiding known issue areas.

---

## Prerequisites
```bash
# Build and install the app
./gradlew installDebug

# Or install pre-built APK
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## Demo Flow: Pet Owner → Staff → Branch Manager

### 🎬 ACT 1: PET OWNER (Happy Path)

#### Login
1. Launch app → **Login Screen**
2. Enter pet owner credentials:
   - Email: `petowner@demo.com`
   - Password: `demo123`
3. Tap **LOGIN** → Navigates to Pet Owner Dashboard

#### Dashboard (PetOwner_Dashboard)
- Shows: Upcoming appointments, pet summary, quick actions
- **Working**: Bottom nav (Appointments, My Pets, Cases, Medications, Profile)

#### My Pets → Add New Pet
1. Tap **My Pets** (bottom nav)
2. Tap **FAB (+)** → **Add Pet Dialog**
3. Fill required fields:
   - Name: "Buddy"
   - Species: Dog
   - Breed: Golden Retriever
   - Birth Date: 2022-01-15
   - Gender: Male
4. Tap **Save** → Pet appears in list ✓

#### My Pets → Edit Pet Details
1. Tap **Buddy** card → **PetOwnerPetDetailScreen**
2. Tap **Edit** (pencil icon) → **EditPetDialog**
3. Update: Weight → "28.5 kg", add note
4. Tap **Save** → Changes reflected immediately ✓

#### Book Appointment (Check-up Only)
1. Tap **Appointments** (bottom nav)
2. Tap **FAB (+)** → **BookAppointmentScreen**
3. Select pet: "Buddy"
4. Appointment Type: **Check-up** (only bookable type)
5. Select date/time (any future slot)
6. Reason: "Annual wellness check"
7. Tap **Book** → **Payment Screen** (PayPal stub - shows "Payment integration pending")
8. Tap **Skip Payment (Demo)** → Appointment created ✓
   - *Note: PayPal integration not yet implemented - demo bypasses payment*

#### View Appointment Details
1. Tap created appointment → **PetOwnerAppointmentDetailScreen**
2. Shows: Pet, type, date/time, status, reason
3. Status badge: "Pending" → Updates to "Confirmed" after staff action

#### Medical Records (Cases)
1. Tap **Cases** (bottom nav) → **PetOwnerCasesScreen**
2. Shows read-only case history for owned pets
3. Tap case → **PetOwnerCaseDetailScreen** (view only)

#### Medications
1. Tap **Medications** (bottom nav) → **PetOwnerMedicationsScreen**
2. Shows active prescriptions for owned pets

#### Profile
1. Tap **Profile** (bottom nav) → **PetOwnerProfileScreen**
2. View: Name, email, role badge "Pet Owner"
3. Logout works correctly

---

### 🎬 ACT 2: STAFF (Veterinarian/Nurse)

#### Login
1. Logout from pet owner
2. Enter staff credentials:
   - Email: `vet@demo.com` (or `nurse@demo.com`)
   - Password: `demo123`
3. Tap **LOGIN** → Navigates to **Staff Dashboard**

#### Staff Dashboard (StaffDashboardScreen)
- Bottom nav: **Patients, Cases, Appointments, Profile**
- Role badge shows: "Veterinarian" / "Assistant Vet" / "Nurse"

#### Patients → View Patient List
1. Tap **Patients** → **PatientsScreen**
2. Search by name/owner
3. List shows: Pet photo, name, breed, owner, status
4. **FAB (+)** → Register new patient (requires owner search first)

#### Patients → Pet Details (Staff View)
1. Tap any patient → **PetDetailsScreen**
2. **Working**: View all tabs (Info, Medical, Appointments, Medications)
3. **Edit**: Tap pencil icon → EditPetDialog (full edit capability)
4. Species-specific fields render correctly (dog/cat icons)

#### Cases → Create New Case
1. Tap **Cases** → **CasesScreen**
2. Tap **FAB (+)** → **SearchPetForCaseScreen**
3. Search/select pet → **SelectCaseTypeScreen**
4. Choose: Consultation / Surgery / Emergency / Follow-up
5. Fill case details → **CaseDetailsScreen**
6. Case created with status "Open" ✓

#### Cases → Manage Existing Case
1. Tap case from list → **CaseDetailsScreen**
2. Add: Diagnosis, Treatment Plan, Notes
3. Update status: Open → In Progress → Closed
4. Changes persist and reflect in list ✓

#### Appointments → Manage Schedule
1. Tap **Appointments** → **AppointmentsScreen**
2. View all appointments (all pets, all owners)
3. Filter by status: Pending/Confirmed/Checked-in/Completed/Cancelled
4. Tap appointment → **AppointmentDetailScreen**
5. **Actions**: Check-in, Complete, Cancel
6. Status updates reflect immediately ✓

#### Profile
1. Tap **Profile** → **StaffProfileScreen**
2. View: Name, email, role, clinic assignment
3. Logout works

---

### 🎬 ACT 3: BRANCH MANAGER

#### Login
1. Logout from staff
2. Enter branch manager credentials:
   - Email: `manager@demo.com`
   - Password: `demo123`
3. Tap **LOGIN** → Navigates to **Branch Manager Dashboard**

#### Branch Manager Dashboard
- Bottom nav: **Dashboard, Patients, Appointments, Staff, Profile**
- **NEW**: Consistent bottom bar across all screens ✓

#### Dashboard Overview
1. Shows: Total patients, today's appointments, staff count, revenue summary
2. Quick action cards for common tasks

#### Patients (BranchManagerPatientsScreen)
1. Tap **Patients** → **BranchManagerPatientsScreen**
2. **Working**: Search, filter, sort (active first)
3. **FAB (+)** → Register new patient (SearchOwnerScreen → RegisterPetScreen)
3. Tap patient → Navigates to staff **PetDetailsScreen** (shared)
4. List refreshes after adding new patient ✓

#### Appointments (BranchManagerAppointmentsScreen)
1. Tap **Appointments** → **BranchManagerAppointmentsScreen**
2. View ALL appointments across clinic
3. Status chips: Pending/Confirmed/Checked-in/Completed/Cancelled
4. Tap → **CaseRoutes.caseDetails** (links to case management)

#### Staff Management (StaffListScreen) ← **Previously Broken, Now Fixed**
1. Tap **Staff** → **StaffListScreen**
2. **Fixed**: List shows all staff with role badges
3. **Fixed**: FAB (+) → **CreateStaffScreen**
4. **Create Staff Account**:
   - Fill: Full name, email, phone, role (Vet/Assistant Vet/Nurse)
   - Tap **Create** → Account created in Supabase Auth + profile
5. **Fixed**: New staff appears in list immediately (route-watching refresh) ✓
6. **Fixed**: Edit staff → Dialog matches Create form (3 roles: vet, assistant_vet, nurse) ✓
7. **Fixed**: Delete staff → Confirmation dialog, prevents self-deletion ✓

#### Profile (BranchManagerProfileScreen)
1. Tap **Profile** → **BranchManagerProfileScreen**
2. Shows: Name, email, role "Branch Manager"
3. **Logout**: Tap Log Out → Confirm dialog → Returns to Login ✓

---

## 🎯 Key Talking Points for Presentation

| Feature | Status | Demo Notes |
|---------|--------|------------|
| Pet Owner: Add/Edit Pets | ✅ Working | Full CRUD in My Pets |
| Pet Owner: Book Check-up | ✅ Working | Payment stubbed (demo bypass) |
| Pet Owner: View Records | ✅ Working | Read-only cases/medications |
| Staff: Patient Management | ✅ Working | Full pet details + edit |
| Staff: Case Management | ✅ Working | Create, update, close cases |
| Staff: Appointment Control | ✅ Working | Check-in, complete, cancel |
| Branch Manager: Staff CRUD | ✅ Fixed | Create/Edit/Delete + auto-refresh |
| Branch Manager: Patients | ✅ Working | Search, register, view all |
| Branch Manager: Appointments | ✅ Working | Clinic-wide view |
| Branch Manager: Navigation | ✅ Fixed | Consistent bottom bar |

---

## 🐛 Known Issues (Do Not Demo)

| Area | Issue | Workaround |
|------|-------|------------|
| PayPal Payment | Not integrated | Use "Skip Payment (Demo)" button |
| Add Pet (Pet Owner) | RLS policy may block | Use staff account to register pets |
| Self-delete prevention | Works but test carefully | Don't demo delete on own account |

---

## 📱 Quick Navigation Commands (ADB)

```bash
# Launch specific screens directly (for quick demo transitions)
adb shell am start -n com.jcm.vetterment/.MainActivity

# Force stop between roles
adb shell am force-stop com.jcm.vetterment

# Clear app data (reset login)
adb shell pm clear com.jcm.vetterment
```

---

## 🎥 Recording Tips

1. **Screen record**: `adb shell screenrecord /sdcard/demo.mp4`
2. **Pull recording**: `adb pull /sdcard/demo.mp4`
3. **Show touches**: Settings → Developer Options → Show taps

---

## ✅ Pre-Demo Checklist

- [ ] App installed on device/emulator
- [ ] Three test accounts created in Supabase:
  - [ ] Pet Owner: `petowner@demo.com` / `demo123`
  - [ ] Staff (Vet): `vet@demo.com` / `demo123`
  - [ ] Branch Manager: `manager@demo.com` / `demo123`
- [ ] Test data exists: pets, appointments, cases, staff
- [ ] Device charged, notifications off
- [ ] Screen recording ready

---

## 📝 Post-Demo Notes

After demo, document:
- Which flows worked smoothly
- Any unexpected behavior
- Questions from audience
- Features requested for next iteration