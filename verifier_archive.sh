#!/usr/bin/env bash
# Vérification complète de l'archive (Bash / Git Bash / Linux / macOS).
# Usage : bash verifier_archive.sh
# Surcharge possible : JAVAC=/chemin/javac JAVA=/chemin/java bash verifier_archive.sh

set -u

ERRORS=0

fail() {
    echo "ERREUR : $*" >&2
    ERRORS=$((ERRORS + 1))
}

ok() {
    echo "OK : $*"
}

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

echo "=== 1) Structure de l'archive ==="
for required in EQUIPE.txt rapport.pdf README.txt src tests/tests.txt automates; do
    if [ -e "$required" ]; then
        ok "présent : $required"
    else
        fail "élément absent : $required"
    fi
done

echo
echo "=== 2) Version de Java ==="
JVER=$("$JAVAC" -version 2>&1 | head -n 1 | sed -E 's/^[^0-9]*([0-9]+).*/\1/')
RVER=$("$JAVA" -version 2>&1 | head -n 1 | sed -E 's/.*"([0-9]+).*/\1/')
if [ -n "$JVER" ] && [ "$JVER" -ge 17 ]; then
    ok "javac $JVER (cible --release 17)"
else
    fail "javac doit etre Java 17 ou plus recent (utiliser JAVAC=...)"
fi
if [ -n "$RVER" ] && [ "$RVER" -ge 17 ]; then
    ok "java $RVER"
else
    fail "java doit etre Java 17 ou plus recent (utiliser JAVA=...)"
fi

echo
echo "=== 3) Compilation ==="
rm -rf out
mkdir -p out
if "$JAVAC" --release 17 -d out src/*.java; then
    ok "javac --release 17 -d out src/*.java"
else
    fail "compilation impossible"
fi

echo
echo "=== 4) Jeu de tests ==="
if [ -f out/TestRunner.class ]; then
    if "$JAVA" -cp out TestRunner; then
        ok "TestRunner"
    else
        fail "TestRunner a signalé un échec"
    fi
else
    fail "out/TestRunner.class absent (compilation incomplète)"
fi

echo
echo "=== 5) Génération des trois fichiers .dot ==="
mkdir -p automates

generate_dot() {
    file="$1"
    expression="$2"
    word="$3"

    if "$JAVA" -cp out Main --dot "$file" "$expression" "$word" >/dev/null; then
        if [ -s "$file" ]; then
            if grep -q '^digraph AFN {' "$file" && grep -q '__start ->' "$file"; then
                ok "dot généré : $file"
            else
                fail "contenu .dot invalide : $file"
            fi
        else
            fail "fichier .dot vide : $file"
        fi
    else
        fail "échec de la génération de $file pour « $expression »"
    fi
}

generate_dot "automates/automate_ababb.dot"      "(a|b)*abb" "abb"
generate_dot "automates/automate_abplus.dot"     "(ab)+"     "ab"
generate_dot "automates/automate_aquestionb.dot" "a?b"       "ab"

echo
echo "=== Résumé ==="
if [ "$ERRORS" -eq 0 ]; then
    echo "PASS : archive complète, compilation, tests et .dot corrects."
    exit 0
else
    echo "FAIL : $ERRORS erreur(s) détectée(s)."
    exit 1
fi
