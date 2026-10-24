# Claude Instructions --- VETterment Android App

> Read this file before making changes. These notes provide project
> context and guardrails; always inspect the current repository because
> code and database state may have changed.

## 1. Project overview

**Project:** VETterment --- A Mobile-Based Veterinary Case Management
System with 3D Patient Visualization for PetHub Veterinary Clinic.

**Technology:** - Android Studio - Kotlin - Jetpack Compose and Material
3 - Supabase Auth, Postgres, and potentially Supabase Edge Functions -
Android application package: `com.jcm.vetterment` - Screens package:
`com.jcm.vetterment.screens` - Supabase integration package:
`com.jcm.vetterment.supabase` - Data models have been used under
`com.jcm.vetterment.features.data_models` - Pet Owner features are under
`com.jcm.vetterment.features.pet_owner`

Use actual repository paths as the source of truth. The paths above are
context, not permission to assume a file exists.

This is a standalone Android app for PetHub Veterinary Clinic. It does
**not** replace or integrate with the clinic's existing confidential
desktop records system.

## 2. How to work in this repository

The developer is familiar with Java but is still learning Kotlin and
Jetpack Compose. Explain changes clearly and give exact file paths.

-   Start by inspecting the existing implementation before editing.
-   Make small, incremental, reviewable changes. Avoid large rewrites.
-   Prefer targeted patches over replacing entire files.
-   Follow the existing architecture and naming conventions.
-   The UI and logic may be separated into composables/wrapper
    functions; inspect how the relevant screen is structured before
    changing it.
-   Reuse existing models, helpers, repositories, components, and
    navigation where practical.
-   Do not add dependencies unless genuinely necessary; explain why
    before adding one.
-   Do not invent filenames, models, database columns, routes, or
    functions.
-   Do not use mock data as if it were real production data.
-   Do not silently change unrelated features.
-   Do not commit, push, deploy, or run destructive database changes
    unless explicitly asked.
-   After each stage, summarize the files changed and why, and tell the
    developer what to test.
-   Run a relevant Gradle build when possible, report the actual result,
    and distinguish new errors from pre-existing errors.
-   If a required file, schema, secret, or environment is inaccessible,
    say so instead of guessing.

## 3. Current priority: PayPal Sandbox for check-up booking

The current task is to integrate **PayPal Sandbox** into the existing
Kotlin + Jetpack Compose + Supabase app.

### Business rules

-   Only **Check-up** is bookable through the Pet Owner's appointment
    booking flow.
-   The doctor fee is **₱400**.
-   The owner must complete a successful payment before the appointment
    booking is submitted.
-   A failed, cancelled, abandoned, or unverified payment must not
    create a paid appointment.
-   Payment does not guarantee a consultation time or queue priority.
-   PetHub follows a first-come-first-served approach and operates 24/7,
    including emergencies.
-   Do not create emergency appointments or imply emergency care must
    wait for app booking.
-   The clinic's ordinary check-up hours were discussed as approximately
    9 AM--5 PM but are **not confirmed**. Do not hardcode these hours as
    settled policy.
-   Additional procedure, diagnostic, and surgery charges are handled at
    the clinic; do not invent or display prices for them.
-   A successful payment does **not automatically mean the appointment
    is clinic-confirmed**. Keep payment status and appointment status
    separate.

### Important currency check

The intended fee is ₱400 PHP. Before implementing the final PayPal
order, verify the current PayPal Sandbox API's support for the intended
`PHP` currency and the account's transaction configuration. Do not
silently convert PHP to another currency or mislabel an amount. If
PayPal's supported currency configuration prevents the intended
transaction, report the limitation and propose options before changing
the product requirement.

## 4. PayPal security rules --- mandatory

**Never place PayPal secrets in the Android app.**

Never include a PayPal Client Secret, Supabase service-role key,
database password, access token, or other private credential in: -
Kotlin source files - `BuildConfig` - Android resources -
`local.properties` - Git-tracked configuration - Logs, screenshots, or
final reports

Use server-side secrets for PayPal credentials, for example Supabase
Edge Function secrets. Do not print secret values while inspecting
files. It is acceptable to report the names of environment variables
referenced.

The Android app must not be trusted to determine: - The authoritative
amount or currency - The authenticated owner's identity - Whether
payment succeeded - Whether an order belongs to an appointment - Whether
an appointment is confirmed

The backend must authenticate the user and validate ownership. Never
accept a client-provided amount as authoritative.

## 5. Expected payment architecture

Inspect the existing implementation before deciding exact files,
functions, or schema. A likely secure flow is:

1.  Owner selects their registered pet and starts a Check-up booking.
2.  App asks a Supabase Edge Function to start payment.
3.  The Edge Function authenticates the user and validates the
    pet/booking data.
4.  The server determines the required amount and currency and creates a
    PayPal Sandbox order using server-side credentials.
5.  The server returns only the information the app needs, such as the
    order ID and approval URL.
6.  Android opens the PayPal approval URL.
7.  After approval, PayPal returns the user directly to the Android app
    using a correctly configured Android App Link or deep link.
8.  The app treats the return as a signal to continue---not as proof of
    payment.
9.  The app calls a server endpoint to capture the order.
10. The backend verifies the PayPal response, including order
    association, capture status, amount, and currency.
11. Only after verified successful capture should the backend record the
    payment as paid and submit/create the appointment according to the
    agreed workflow.
12. The app refreshes state from the backend.

Use the PayPal Orders API's supported create-order, buyer-approval, and
server-side capture flow. Consider webhook reconciliation for cases
where the user closes the app or loses connectivity. Do not add a
webhook merely for appearance; implement and verify it correctly if
required.

### Reliability and duplicate prevention

-   Handle repeated button taps and network retries.
-   Prevent duplicate captures and duplicate appointment submissions.
-   Use stable PayPal order IDs and server-side idempotency/duplicate
    checks where supported.
-   Treat cancelled checkout, failed capture, timeout, and unknown
    payment status safely.
-   Do not mark payment `PAID` just because the app receives a deep
    link.
-   Do not leave the app displaying success when the backend has not
    confirmed payment.
-   Design the payment/booking state transition so a successful capture
    followed by a network failure can be recovered without charging or
    booking twice.

## 6. Inspect before implementation

At the start of the PayPal task, inspect and report the actual files and
behavior for:

### Android

-   Appointment model and status values
-   Pet Owner Appointments screen and booking form
-   Appointment repository/helper and Supabase queries
-   Supabase client/auth setup
-   Existing payment-related code
-   Existing browser/Custom Tab handling
-   Android manifest, intent filters, deep links, or App Links
-   Relevant Gradle dependencies

### Supabase

-   Existing Edge Functions and their configuration
-   SQL migrations and schema files in the repository
-   Appointment table columns, constraints, triggers, and RLS policies
-   Existing payment table/columns and payment-related policies
-   Existing webhook or server-side payment verification code
-   Any existing Maya or other payment integration

Do not assume local migration files exactly match the deployed Supabase
database. Clearly distinguish what was verified from repository files
and what still needs checking in the Supabase dashboard.

Do not read or print secret values during inspection.

### Inspection-first rule

If the repository or Supabase dashboard is not available, explain that
limitation and ask for the relevant files or safe schema output. Do not
claim to have inspected code that was not available.

## 7. Database and authorization principles

-   Reuse the current appointment/payment schema if it can safely
    support the workflow.
-   Do not create a new payment table or add columns until the current
    schema is inspected and the need is justified.
-   Do not modify RLS policies or run migrations automatically without
    explaining the changes first.
-   Payment records and appointment records must be associated with the
    correct authenticated owner and selected pet.
-   Enforce authorization on the backend/database, not only by hiding UI
    elements.
-   Never trust an owner ID supplied by the Android client when the
    backend can derive it from the authenticated user.
-   Avoid storing unnecessary payment credentials or sensitive payment
    data.
-   Store only the transaction identifiers and status information needed
    by the application.
-   Keep payment status separate from appointment/clinical status.

## 8. Android checkout return

The requirement is to return **directly to the VETterment Android app**
after PayPal approval.

Before implementing: - Inspect the existing `AndroidManifest.xml` and
navigation setup. - Choose a suitable verified Android App Link or a
carefully configured deep link based on the existing app. - Make sure
the callback can identify the relevant order without treating URL
parameters as trusted proof of payment. - Validate incoming link data
and prevent unrelated links from triggering payment actions. - Resume by
asking the backend to verify/capture the order. - Handle cancellation
and return paths as well as successful approval. - Do not embed PayPal
secrets in the return URL. - Do not assume the app will remain alive
while the owner is in the browser.

Use a Custom Tab or browser flow if appropriate for the current
architecture. Do not add an unrelated native PayPal SDK without first
checking the current PayPal integration guidance and the simplest secure
approach.

## 9. Existing Pet Profile Details screen

**The Pet Details screen is acceptable for now. Do not redesign or
refactor it.**

Do not make changes to the Pet Details screen, its layout, validation,
or pet-information display as part of the PayPal integration unless a
specific dependency is demonstrated and the developer approves the scope
change.

Do not revisit the recent Pet Profile Overview refinements during the
payment task. Keep the work focused on payment and appointment booking.

## 10. UI wording and clinic workflow

For Pet Owners: - Prefer clear labels such as **Appointments**,
**Medical Records**, and **Treatment Progress**. - Avoid using **Cases**
as the main Pet Owner-facing label for medical history; it is internal
clinical terminology. - Keep internal database table names and Kotlin
model names if they are otherwise appropriate; do not rename them solely
to change UI wording. - Do not confuse appointment booking with an
actual completed visit or medical record. - Do not create fake medical
records at payment time. - Do not equate payment success with staff
confirmation, arrival, check-in, or queue priority.

## 11. Roles and privacy context

The project has these roles: - Admin: technical maintenance of the
codebase, database, and system. - Head Veterinarian: clinical oversight
and main case handler. - Veterinarian. - Clinical Staff. - Pet Owner.

Use existing role and authorization implementation as the source of
truth. Do not invent new roles or broaden access. Enforce sensitive
access in Supabase policies/backend functions as well as the UI.

Pet Owners should not directly edit authoritative clinical information
such as known allergies or pre-existing conditions. This is context
only; do not change the Pet Details screen as part of the PayPal task.

## 12. Recommended implementation stages

Do not implement everything in one large change. Use the following
sequence and pause after each stage for review.

### Stage 1 --- Inspection only

Report existing files, schemas, booking sequence, payment support,
security gaps, and exact proposed changes. Do not edit files.

### Stage 2 --- Backend configuration and order creation

Configure Sandbox credentials securely and implement only the
server-side create-order endpoint. Do not expose secrets. Test order
creation before changing the UI.

### Stage 3 --- Capture and verified payment state

Implement secure server-side capture, ownership/order validation,
duplicate protection, and persistence of verified payment state. Decide
how to handle successful capture followed by appointment submission
failure.

### Stage 4 --- Android checkout and return

Add the smallest necessary Android changes to open the approval URL and
return to the app. Handle approval, cancellation, and interrupted flows.
Do not mark payment paid based on the return link.

### Stage 5 --- Appointment submission after payment

Make sure the appointment is created/submitted only after verified
successful payment. Keep the amount fixed/authoritative on the server
and keep payment status distinct from appointment confirmation.

### Stage 6 --- Testing and review

Test successful payment, cancellation, failed capture, retry, repeated
requests, app closure, deep-link validation, wrong-owner access, and
duplicate booking prevention. Build the Android app and report actual
results.

For each stage: - List exact files changed. - Explain the purpose of
each change. - Provide any required Supabase CLI commands or dashboard
steps. - Explain how to test before continuing. - Stop and wait for
review when a major decision or schema change is needed.

## 13. Final checklist

Before considering PayPal Sandbox integration complete, confirm:

-   [ ] PayPal credentials exist only in server-side secret storage.
-   [ ] Android never decides that payment succeeded by itself.
-   [ ] The server controls the amount and currency.
-   [ ] PHP currency support and the intended ₱400 amount have been
    verified.
-   [ ] Only the authenticated owner can pay for their own eligible
    booking.
-   [ ] PayPal approval returns to VETterment.
-   [ ] Return/deep-link parameters are not trusted as proof of payment.
-   [ ] Server-side capture and verification work.
-   [ ] Duplicate requests cannot create duplicate payment/appointment
    records.
-   [ ] Payment status and appointment confirmation are distinct.
-   [ ] Failed or cancelled payments are not marked paid.
-   [ ] Successful payments can be reconciled if the app closes or the
    network fails.
-   [ ] Existing booking behavior and unrelated features remain intact.
-   [ ] Pet Details screen remains unchanged.
-   [ ] Build and test outcomes are reported accurately.
-   [ ] No secrets, unrelated rewrites, or unapproved
    deployments/commits occurred.

**Default instruction: inspect first, explain the plan, make the
smallest safe change, test it, report what happened, and only then
proceed to the next stage.**
