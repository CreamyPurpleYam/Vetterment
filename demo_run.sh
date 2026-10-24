#!/bin/bash
# VETterment Demo Automation Script
# Runs the app and navigates through key screens for each role
# Requires: Android device/emulator connected via ADB

set -e

PACKAGE="com.jcm.vetterment"
ACTIVITY="com.jcm.vetterment.MainActivity"

echo "=========================================="
echo "  VETterment Demo Automation Script"
echo "=========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

function step() {
    echo -e "${GREEN}[STEP]${NC} $1"
}

function warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

function error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

function pause() {
    echo -e "${YELLOW}Press Enter to continue...${NC}"
    read -r
}

# Check ADB connection
step "Checking ADB connection..."
if ! adb devices | grep -q "device$"; then
    error "No device connected. Please connect device/emulator first."
    exit 1
fi
echo "Device found: $(adb devices | grep device$ | head -1)"

# Build and install
step "Building and installing debug APK..."
./gradlew installDebug -q
echo "Install complete."

# Clear app data for clean start
step "Clearing app data for clean demo..."
adb shell pm clear $PACKAGE

# Launch app
step "Launching app..."
adb shell am start -n $PACKAGE/$ACTIVITY
sleep 3

echo ""
echo "=========================================="
echo "  DEMO: PET OWNER FLOW"
echo "=========================================="
echo ""
echo "Manual steps for Pet Owner:"
echo "1. Login with: petowner@demo.com / demo123"
echo "2. Dashboard → My Pets → Add Pet (Buddy, Dog)"
echo "3. Tap Buddy → Edit → Update weight"
echo "4. Appointments → Book Check-up → Skip Payment"
echo "5. Cases → View medical records"
echo "6. Medications → View prescriptions"
echo "7. Profile → Logout"
echo ""
pause

echo ""
echo "=========================================="
echo "  DEMO: STAFF FLOW (Veterinarian)"
echo "=========================================="
echo ""
echo "Manual steps for Staff:"
echo "1. Login with: vet@demo.com / demo123"
echo "2. Patients → View list → Tap pet → Edit details"
echo "3. Cases → Create new case (Consultation)"
echo "4. Appointments → Check-in, Complete appointment"
echo "5. Profile → Logout"
echo ""
pause

echo ""
echo "=========================================="
echo "  DEMO: BRANCH MANAGER FLOW"
echo "=========================================="
echo ""
echo "Manual steps for Branch Manager:"
echo "1. Login with: manager@demo.com / demo123"
echo "2. Dashboard → View summary stats"
echo "3. Patients → Search, Add new patient"
echo "4. Appointments → View clinic-wide schedule"
echo "5. Staff → Add new staff (nurse@demo.com, Nurse)"
echo "6. Staff → Edit role, Delete (not self)"
echo "7. Profile → Logout"
echo ""
pause

# Optional: Screen recording
echo ""
echo "=========================================="
echo "  OPTIONAL: SCREEN RECORDING"
echo "=========================================="
echo ""
echo "To record the demo:"
echo "  adb shell screenrecord /sdcard/vetterment_demo.mp4 --time-limit 180"
echo ""
echo "To pull recording after demo:"
echo "  adb pull /sdcard/vetterment_demo.mp4 ./vetterment_demo.mp4"
echo ""

step "Demo script complete. App is running on device."
echo "Use the manual steps above for each role demonstration."