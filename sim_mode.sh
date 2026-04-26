#!/usr/bin/env bash
set -euo pipefail

MODE="${1:?usage: ./sim_mode.sh <low|medium|high>}"
BASE_URL="${SOVA_AGENTS_URL:-https://sova-agents.onrender.com}"
PATIENT_ID="${PATIENT_ID:-default}"

echo "Setting $MODE simulation for patient $PATIENT_ID"
curl -sS -X POST "$BASE_URL/v1/patients/$PATIENT_ID/simulation" \
  -H "Content-Type: application/json" \
  -d "{\"mode\":\"$MODE\"}"
echo
STATUS_PATIENT_ID="$PATIENT_ID"
if [[ "$STATUS_PATIENT_ID" == "default" || "$STATUS_PATIENT_ID" == "*" || "$STATUS_PATIENT_ID" == "all" ]]; then
  STATUS_PATIENT_ID="${SOVA_STATUS_PATIENT_ID:-CR-003}"
fi
curl -sS "$BASE_URL/v1/patients/$STATUS_PATIENT_ID/status"
echo
