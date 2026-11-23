#!/usr/bin/env bash
# Vérification des trois extensions (Bash / Git Bash / Linux / macOS).
# Usage : JAVAC=/chemin/javac JAVA=/chemin/java bash verifier_extensions.sh
# Les expressions contenant '*' sont lancées depuis out/ : sous WSL, le
# lanceur java.exe de Windows étend sinon les jokers de la ligne de commande.
# Les comparaisons portent sur des sous-chaînes ASCII : la console Windows
# peut recoder les accents et ajouter des CRLF.

set -u
BASE="$(cd "$(dirname "$0")" && pwd)"
JAVAC="${JAVAC:-javac}"
JAVA="${JAVA:-java}"
if [ -n "${JAVA_HOME:-}" ] && [ "$JAVAC" = "javac" ] && [ -x "$JAVA_HOME/bin/javac" ]; then
    JAVAC="$JAVA_HOME/bin/javac"
    JAVA="$JAVA_HOME/bin/java"
elif [ -n "${JAVA_HOME:-}" ] && [ "$JAVAC" = "javac" ]; then
    echo "REMARQUE : JAVA_HOME inexistant ignoré : $JAVA_HOME"
fi
# Repli sous WSL : JDK installé sous C:\Program Files\Java ou dans le temporaire.
if [ "$JAVAC" = "javac" ] && ! command -v javac >/dev/null 2>&1; then
    for candidate in "/mnt/c/Program Files/Java"/jdk-*/bin/javac.exe \
                     /mnt/c/Users/*/AppData/Local/Temp/jdk17/*/bin/javac.exe; do
        if [ -x "$candidate" ]; then
            JAVAC="$candidate"
            JAVA="${candidate%javac.exe}java.exe"
            break
        fi
    done
fi

ERRORS=0
fail() { echo "ERREUR : $*" >&2; ERRORS=$((ERRORS + 1)); }
ok() { echo "OK : $*"; }

LAST_OUTPUT=""
LAST_CODE=0
run_capture() {
    LAST_OUTPUT=$("$JAVA" -cp . "$@" 2>&1)
    LAST_CODE=$?
    LAST_OUTPUT=$(printf '%s' "$LAST_OUTPUT" | tr -d '\r')
}
check_code() {
    if [ "$LAST_CODE" -ne "$1" ]; then
        fail "$2 : code $LAST_CODE au lieu de $1"
    fi
}
has() {
    if printf '%s\n' "$LAST_OUTPUT" | grep -Fq -- "$1"; then
        ok "$2 [$1]"
    else
        fail "$2 : ligne absente [$1]"
    fi
}
hasnt() {
    if printf '%s\n' "$LAST_OUTPUT" | grep -Fq -- "$1"; then
        fail "$2 : ligne inattendue [$1]"
    else
        ok "$2 (absence de [$1])"
    fi
}

echo "=== 0) Compilation et non-regression ==="
JVER=$("$JAVAC" -version 2>&1 | head -n 1 | sed -E 's/^[^0-9]*([0-9]+).*/\1/')
RVER=$("$JAVA" -version 2>&1 | head -n 1 | sed -E 's/.*"([0-9]+).*/\1/')
if [ -n "$JVER" ] && [ "$JVER" -ge 17 ]; then
    ok "javac $JVER (cible --release 17)"
else
    fail "javac doit etre Java 17 ou plus recent"
fi
if [ -n "$RVER" ] && [ "$RVER" -ge 17 ]; then
    ok "java $RVER"
else
    fail "java doit etre Java 17 ou plus recent"
fi

cd "$BASE" || exit 1
rm -rf out
mkdir -p out
if "$JAVAC" --release 17 -d out src/*.java; then
    ok "javac --release 17 -d out src/*.java"
else
    fail "compilation impossible"
    exit 1
fi
if "$JAVA" -cp out TestRunner >/dev/null; then
    ok "TestRunner (rendu principal inchange)"
else
    fail "TestRunner en echec"
fi

cd "$BASE/out" || exit 1
mkdir -p "$BASE/automates"

echo
echo "=== (a) Determinisation : --dfa ==="
run_capture DfaMain "(a|b)*abb" abb aabb ab
check_code 0 "dfa-1"
has "abb : OUI" "dfa-1"
has "aabb : OUI" "dfa-1"
has "ab : NON" "dfa-1"

run_capture DfaMain "a*+?" "" a aa b
check_code 0 "dfa-2"
has '"" : OUI' "dfa-2"
has "a : OUI" "dfa-2"
has "aa : OUI" "dfa-2"
has "b : NON" "dfa-2"

run_capture DfaMain "(a?)*" "" a aa b
check_code 0 "dfa-3"
has '"" : OUI' "dfa-3"
has "aa : OUI" "dfa-3"
has "b : NON" "dfa-3"
run_capture DfaMain --dot ../automates/dfa_ababb.dot "(a|b)*abb" abb
if [ -s ../automates/dfa_ababb.dot ] && grep -q '^digraph AFD {' ../automates/dfa_ababb.dot; then
    ok "export AFD : automates/dfa_ababb.dot"
else
    fail "export AFD absent ou invalide"
fi

echo
echo "=== (b) Equivalence : --equiv ==="
run_capture EquivMain "a*" "(a*)*"
check_code 0 "equiv-1"
has "AFD minimaux : 2 et 2" "equiv-1"
has "EQUIVALENTS" "equiv-1"

run_capture EquivMain "a|b" "b|a"
check_code 0 "equiv-2"
has "AFD minimaux : 4 et 4" "equiv-2"
has "EQUIVALENTS" "equiv-2"

run_capture EquivMain "a*" "a+"
check_code 0 "equiv-3"
has "DIFFERENTS" "equiv-3"
hasnt "EQUIVALENTS" "equiv-3"

run_capture EquivMain "a?" "(@|a)" --dot-prefix ../automates/min_
if [ -s ../automates/min_1.dot ] && [ -s ../automates/min_2.dot ]; then
    ok "export AFD minimaux : automates/min_1.dot, min_2.dot"
else
    fail "export des AFD minimaux absent"
fi

echo
echo "=== (c) Reprise sur erreur : --recover ==="
run_capture RecoveryMain "a||b"
check_code 1 "recover-1"
has "position 3" "recover-1"

run_capture RecoveryMain "a(+b"
check_code 1 "recover-2"
has "position 3" "recover-2 erreur 1"
has "position 5" "recover-2 erreur 2"

run_capture RecoveryMain "a#b" ab
check_code 1 "recover-3"
has "position 2" "recover-3 erreur lexicale"
has "ab : OUI" "recover-3 mot simule"

echo
echo "=== Resume ==="
if [ "$ERRORS" -eq 0 ]; then
    echo "PASS : 9 tests d'extension corrects."
    exit 0
else
    echo "FAIL : $ERRORS erreur(s)."
    exit 1
fi
