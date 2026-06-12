#!/usr/bin/env bash
set -Eeuo pipefail

COUNT="${1:-40}"
EXPECTATION="${2:-scaleout}"
TMP_RESULT="/tmp/clueroom-lb-distribution-$$.txt"

case "$EXPECTATION" in
  scaleout | local-only | any) ;;
  *) echo "ERROR: unknown expectation: $EXPECTATION. Use scaleout, local-only, or any."; exit 1 ;;
esac

echo "========================================"
echo " ClueRoom Nginx LB Distribution Check"
echo "========================================"
echo "count=$COUNT"
echo "expectation=$EXPECTATION"

for i in $(seq 1 "$COUNT"); do
  curl -sI https://api.clueroom.xyz/actuator/health \
    | awk -F': ' 'tolower($1)=="x-clueroom-upstream"{gsub(/\r/,"",$2); print $2; exit}'
done | sort | uniq -c | tee "$TMP_RESULT"

echo
echo "[validation]"

if grep -Eq '127[.]0[.]0[.]1:80([[:space:]]|$)' "$TMP_RESULT"; then
  echo "ERROR: malformed upstream detected: 127.0.0.1:80"
  rm -f "$TMP_RESULT"
  exit 1
fi

if grep -Eq '127[.]0[.]0[.]1([[:space:]]|$)' "$TMP_RESULT"; then
  echo "ERROR: malformed upstream detected: 127.0.0.1 without port"
  rm -f "$TMP_RESULT"
  exit 1
fi

HAS_LOCAL=0
HAS_SCALEOUT=0
grep -Eq '127[.]0[.]0[.]1:(8081|8082)' "$TMP_RESULT" && HAS_LOCAL=1
grep -Eq '172[.]26[.].*:8080' "$TMP_RESULT" && HAS_SCALEOUT=1

case "$EXPECTATION" in
  scaleout)
    if [ "$HAS_LOCAL" -ne 1 ]; then
      echo "ERROR: local blue/green upstream not observed"
      rm -f "$TMP_RESULT"
      exit 1
    fi
    if [ "$HAS_SCALEOUT" -ne 1 ]; then
      echo "ERROR: scaleout app node upstream not observed"
      rm -f "$TMP_RESULT"
      exit 1
    fi
    ;;
  local-only)
    if [ "$HAS_LOCAL" -ne 1 ]; then
      echo "ERROR: local blue/green upstream not observed"
      rm -f "$TMP_RESULT"
      exit 1
    fi
    if [ "$HAS_SCALEOUT" -eq 1 ]; then
      echo "ERROR: scaleout app node upstream observed during local-only verification"
      rm -f "$TMP_RESULT"
      exit 1
    fi
    ;;
  any)
    if [ "$HAS_LOCAL" -ne 1 ] && [ "$HAS_SCALEOUT" -ne 1 ]; then
      echo "ERROR: no known ClueRoom upstream observed"
      rm -f "$TMP_RESULT"
      exit 1
    fi
    ;;
esac

rm -f "$TMP_RESULT"

echo "========================================"
echo " Done"
echo "========================================"
