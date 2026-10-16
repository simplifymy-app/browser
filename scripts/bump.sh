#!/bin/sh
set -e
cd "$(dirname "$0")/.."

GRADLE=app/build.gradle.kts
CURRENT=$(sed -n 's/.*versionName = "\(.*\)".*/\1/p' $GRADLE | head -1)
CODE=$(sed -n 's/.*versionCode = \([0-9]*\).*/\1/p' $GRADLE | head -1)
PART=$1

case "$PART" in
  major|minor|patch)
    NEXT=$(CURRENT="$CURRENT" PART="$PART" awk 'BEGIN {
      split(ENVIRON["CURRENT"], v, ".")
      major = v[1] + 0; minor = v[2] + 0; patch = v[3] + 0
      if (ENVIRON["PART"] == "major") { major++; minor = 0; patch = 0 }
      else if (ENVIRON["PART"] == "minor") { minor++; patch = 0 }
      else { patch++ }
      printf "%d.%d.%d", major, minor, patch
    }')
    ;;
  [0-9]*)
    NEXT=$PART
    ;;
  *)
    echo "usage: sh scripts/bump.sh major|minor|patch|<version>" >&2
    echo "current version is $CURRENT ($CODE)" >&2
    exit 1
    ;;
esac

NEXT_CODE=$((CODE + 1))
sed -e "s/versionName = \"$CURRENT\"/versionName = \"$NEXT\"/" \
    -e "s/versionCode = $CODE\$/versionCode = $NEXT_CODE/" $GRADLE > $GRADLE.tmp
mv $GRADLE.tmp $GRADLE
echo "$CURRENT ($CODE) -> $NEXT ($NEXT_CODE)"
