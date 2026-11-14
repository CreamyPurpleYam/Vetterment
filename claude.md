# VETterment — Project Guide

## 1. Project Overview

**VETterment** is a BSIT capstone project titled:

**"VETterment: Mobile-Based Veterinary Management System With 3D Pet Anatomy Visualization for PetHub Veterinary Clinic."**

It is an Android application designed to help PetHub Veterinary Clinic manage pet information, appointments, medical records, prescriptions, treatment progress, and administrative activities. It also aims to provide simplified 3D visualizations to help pet owners understand their pets' medical conditions.

This is a standalone mobile application. It does not replace or integrate with PetHub's existing desktop record system.

The priority is to build a functional, presentable application for the capstone demonstration. Prefer simple and practical solutions over unnecessary complexity.

## 2. Technology Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose and Material 3
- **IDE:** Android Studio
- **Backend:** Supabase
- **Database:** PostgreSQL
- **Authentication and Storage:** Supabase Auth and Storage
- **Payments:** PayPal Sandbox
- **3D Generation:** AI-based 3D generation API (Tripo AI, subject to the existing implementation)

Continue using the existing stack. Do not introduce additional frameworks or dependencies unless they are genuinely necessary.

## 3. Main System Modules

VETterment consists of the following major modules:

1. Account Registration, Management, Login, and Role-Based Access
2. Appointment Scheduling
3. Pet Management, Medical Records, Prescription, and Treatment Progress
4. 3D Patient Visualization
5. Administrative Management and Reporting

The system has five intended user roles:

- Admin
- Head Veterinarian
- Veterinarian
- Clinical Staff
- Pet Owner

Inspect the existing role implementation and permissions before changing them. Do not assume that role names or responsibilities should be redesigned without a clear reason.

## 4. Important System Requirements

### Pet Management

- The system supports cats and dogs only.
- Pet owners can register and edit their own basic pet information.
- Clinic-managed medical information, such as recorded allergies and pre-existing conditions, must remain protected from pet-owner edits.
- Pet registration avatars should only offer cat and dog options.
- Actual pet photos should use image storage and URLs, not emoji values stored as photo URLs.

### Appointments and Treatment Progress

There are exactly **three appointment options** available to pet owners:

- Consultation
- Confinement
- Surgery

Keep appointment status separate from treatment progress.

**Consultation**
- Does not have a treatment timeline or status updates.
- The veterinarian records the check-up summary in the pet's Medical Records.

**Confinement**

The treatment progression has exactly three stages:

1. Confined
2. Recovery
3. Discharge (Ready for Pick-up)

**Surgery**

The treatment progression has exactly three stages:

1. Surgery
2. Recovery
3. Discharge (Ready for Pick-up)

Treatment progress begins only when the pet is physically brought to and accepted by the clinic. Booking or confirming an appointment must not automatically start treatment or create an active treatment case.

PetHub operates on a first-come-first-served, walk-in basis and accepts emergencies. Do not introduce guaranteed appointment slots, queue priority, or assumptions that conflict with this workflow.

**Follow-up Checkups**
- Follow-up checkups are not a fourth appointment option.
- Veterinarians initiate them from an existing pet profile.
- They should be associated with the pet's medical history and should not create a confinement or surgery timeline.

### Payments

- Use PayPal Sandbox for testing.
- The initial doctor's fee is ₱400 for Consultation and ₱700 for Confinement or Surgery.
- These fees are not the final clinic bill or a downpayment.
- Additional charges based on the veterinarian's findings are handled at the clinic.
- Payment must be verified through the backend. Never simulate successful payments or expose PayPal secrets in the Android application.

### 3D Patient Visualization

- The system focuses on the supported cat and dog breeds and medical conditions established for the project.
- The visualization is intended to help explain a veterinarian's findings.
- It is a simplified representation, not an exact anatomical replica, diagnostic tool, or treatment recommendation.
- The visualization should reflect the veterinarian's recorded findings rather than independently determine a diagnosis.

## 5. Existing Code Structure and Conventions

Preserve the existing project structure and coding style.

In particular, screen files generally follow three main sections:

1. **Logic** — screen state, data retrieval, and business logic.
2. **UI Layer** — Compose layouts and reusable UI components.
3. **Functions** — supporting functions placed at the bottom of the file.

Follow this organization when modifying existing screens.

Use the existing components, naming conventions, navigation patterns, and Supabase data functions where practical. Avoid unnecessary rewrites or architectural migrations.

Inspect related files before changing shared functions or database interactions.

## 6. Development Guidelines

- Understand the existing implementation before modifying it.
- Make focused changes that solve the requested task.
- Preserve working features and avoid unrelated modifications.
- Do not duplicate existing functionality when it can be extended.
- Keep code readable and consistent with the project.
- Check for affected screens, navigation, database operations, and role permissions when making changes.
- Report important incomplete functionality or errors instead of hiding them.
- Test changes where possible and clearly state anything that could not be tested.

Do not modify live Supabase configuration, deploy Edge Functions, run migrations, or change database policies without explicit approval.

Never commit secrets, service-role keys, PayPal credentials, or other private configuration values.

## 7. How to Work on This Project

When starting a new task, first inspect the relevant implementation and understand how it connects to the rest of the application.

For larger tasks, explain the proposed approach before making changes. Ask for clarification only when a decision genuinely affects system behavior and cannot reasonably be determined from the existing requirements or implementation.

Work incrementally, prioritizing essential functionality and a working application over unnecessary refinements.

Do not assume that every issue discovered during a review needs immediate fixing. Distinguish between:
- Required changes
- Existing incomplete features
- Potential improvements

Wait for approval before making changes outside the requested scope.

**Overall goal:** Help complete VETterment as a functional, consistent, and demonstrable capstone application while preserving its established requirements and existing implementation.