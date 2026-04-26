#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${SOVA_AGENTS_URL:-https://sova-agents.onrender.com}"
PATIENT_ID="${PATIENT_ID:-CR-003}"

curl -sS -X POST "$BASE_URL/v1/patients/$PATIENT_ID/simulation" \
  -H "Content-Type: application/json" \
  -d '{"mode":"medium"}'
echo
curl -sS "$BASE_URL/v1/patients/$PATIENT_ID/status"
echo
