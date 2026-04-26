#!/usr/bin/env bash
set -euo pipefail

MODE="${1:?usage: ./sim_mode.sh <low|medium|high>}"
BASE_URL="${SOVA_AGENTS_URL:-https://sova-agents.onrender.com}"

detect_desktop_patient_id() {
  if ! command -v jshell >/dev/null 2>&1; then
    return 0
  fi
  jshell <<'EOF' 2>/dev/null | grep -o 'SOVA_PATIENT_ID=[^[:space:]]*' | sed 's/SOVA_PATIENT_ID=//' | tail -n 1
import java.util.prefs.*;
System.out.println("SOVA_PATIENT_ID=" + Preferences.userRoot().node("org/sova/patient").get("patient_id", ""));
/exit
EOF
}

PATIENT_ID="${PATIENT_ID:-$(detect_desktop_patient_id)}"
PATIENT_ID="${PATIENT_ID:-CR-003}"

echo "Setting $MODE simulation for patient $PATIENT_ID"
curl -sS -X POST "$BASE_URL/v1/patients/$PATIENT_ID/simulation" \
  -H "Content-Type: application/json" \
  -d "{\"mode\":\"$MODE\"}"
echo
curl -sS "$BASE_URL/v1/patients/$PATIENT_ID/status"
echo
