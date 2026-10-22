#!/usr/bin/env bash
# Builds the Aiven CA truststore used for VERIFY_IDENTITY TLS to Aiven MySQL.
#
#   ./scripts/setup-aiven-tls.sh [store-password]
#
# Prints the DB_TRUST_STORE_URL value to add to backend/.env.
set -euo pipefail

STORE_PASSWORD="${1:-changeit}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_ROOT="$(dirname "$SCRIPT_DIR")"
CA_PEM="$BACKEND_ROOT/src/main/resources/tls/aiven-ca.pem"
CERT_DIR="$BACKEND_ROOT/certs"
TRUSTSTORE="$CERT_DIR/aiven-truststore.p12"

if [ ! -f "$CA_PEM" ]; then
  echo "CA certificate not found: $CA_PEM" >&2
  exit 1
fi

mkdir -p "$CERT_DIR"
rm -f "$TRUSTSTORE"

keytool -importcert -noprompt -alias aiven-ca -file "$CA_PEM" \
  -keystore "$TRUSTSTORE" -storetype PKCS12 -storepass "$STORE_PASSWORD"

TRUSTSTORE_URL="file://$(cd "$CERT_DIR" && pwd)/$(basename "$TRUSTSTORE")"

cat <<EOF

Truststore created.
  path : $TRUSTSTORE
  add the following lines to backend/.env to enable verified TLS:

DB_TRUST_STORE_URL=$TRUSTSTORE_URL
DB_TRUST_STORE_PASSWORD=$STORE_PASSWORD
DB_TRUST_STORE_TYPE=PKCS12

  and switch the JDBC url to sslMode=VERIFY_IDENTITY.
EOF
