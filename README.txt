================================================================
README — Du texte d'une expression régulière à l'automate
================================================================

Langage : Java 17
Dépendances : aucune (bibliothèque standard uniquement)
Paquets : aucun — toutes les classes sont dans le répertoire src/
Encodage des sources : UTF-8 (les caractères non ASCII des chaînes
sont écrits avec des échappements \uXXXX pour rester indépendants
de l'encodage par défaut de javac)

----------------------------------------------------------------
1) Structure de l'archive
----------------------------------------------------------------
EQUIPE.txt
rapport.pdf
src/                  (tous les .java)
tests/tests.txt
automates/            (fichiers .dot générés)
README.txt
verifier_archive.sh   (vérification sous Bash / Git Bash / Linux / macOS)
verifier_archive.bat  (vérification sous Windows cmd)
verifier_extensions.sh / .bat (vérification des 3 extensions, optionnelles)
out/                  (répertoire de compilation, créé par javac)

----------------------------------------------------------------
2) Compilation (depuis la racine du projet)
----------------------------------------------------------------
javac --release 17 -d out src/*.java

L'option --release 17 garantit un bytecode Java 17 (class file 61.0),
même si le JDK installé est plus récent (ici JDK 27). Les scripts utilisent
toujours cette option.

JDK disponible sur cette machine (le `java` du PATH est encore Java 8,
voir §2bis) :

  "C:\Program Files\Java\jdk-27.0.1.1\bin\javac.exe" --release 17 -d out src\*.java
  "C:\Program Files\Java\jdk-27.0.1.1\bin\java.exe" -cp out Main "(a|b)*abb" abb aabb ab

Un JDK 17 fonctionnel existe aussi dans le dossier temporaire :

  "C:\Users\1234m\AppData\Local\Temp\jdk17\jdk-17.0.21.1+2\bin\javac.exe" --release 17 -d out src\*.java

Chemin JDK annoncé pour Eclipse (à utiliser s'il existe sur votre machine) :

  "C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot\bin\javac.exe" --release 17 -d out src\*.java

REMARQUE : ce chemin Eclipse n'existe pas sur cette machine ; les scripts
essaient donc aussi JAVA_HOME, JAVAC et les emplacements courants. Si le
vôtre est différent, utilisez :

set "JAVA_HOME=C:\Program Files\Java\jdk-27.0.1.1"
verifier_archive.bat

----------------------------------------------------------------
2bis) Dépannage : UnsupportedClassVersionError
----------------------------------------------------------------
Si `java -cp out Main "(a|b)*abb" abb aabb ab` affiche :

  UnsupportedClassVersionError: Main has been compiled by a more recent
  version of the Java Runtime (class file version 61.0), this version of
  the Java Runtime only recognizes class file versions up to 52.0

c'est que le `java` du PATH est Java 8 (52.0) alors que les classes ont été
compilées pour Java 17 (61.0). Pour confirmer :

  where java
  java -version
  javac -version

Solution avec le JDK 27 installé (PowerShell) :

  $jdk = "C:\Program Files\Java\jdk-27.0.1.1"
  & "$jdk\bin\javac.exe" --release 17 -d out src\*.java
  & "$jdk\bin\java.exe" -cp out Main "(a|b)*abb" abb aabb ab

Le bytecode reste en version 61.0, exécutable par Java 17 ou plus récent.

Solution durable dans la session :

  $env:JAVA_HOME = "C:\Program Files\Java\jdk-27.0.1.1"
  $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
  java -version      # doit afficher 27.x
  java -cp out Main "(a|b)*abb" abb aabb ab

Pour que ce soit permanent :
  [Environment]::SetEnvironmentVariable("JAVA_HOME", $env:JAVA_HOME, "User")
puis redémarrer PowerShell. Les scripts verifier_archive.bat/.sh et
verifier_extensions.bat/.sh acceptent désormais Java 17 ou plus récent,
compilent avec --release 17 et ignorent un JAVA_HOME inexistant.

Il n'est pas possible de compiler ce projet avec Java 8 : le code utilise
les records, les switch expressions, Set.of(), List.copyOf(), etc.

----------------------------------------------------------------
3) Exécution du programme principal
----------------------------------------------------------------
java -cp out Main "(a|b)*abb" abb aabb ab

Sortie attendue :
abb : OUI
aabb : OUI
ab : NON

Syntaxe générale :
java -cp out Main <expression> <mot>... [--dot fichier.dot]

Règles de la ligne de commande :
  - l'option --dot peut apparaître n'importe où dans les arguments ;
  - le mot vide s'écrit soit "" (chaîne vide : aucun caractère),
    soit la chaîne littérale formée de deux guillemets ; il est
    toujours affiché sous la forme "" : OUI / NON ;
  - une expression invalide produit un message sur stderr et le code 1 ;
  - un usage incorrect produit "Usage : ..." sur stderr et le code 2 ;
  - l'option --dot répétée ou sans nom de fichier est un usage incorrect ;
  - zéro mot après l'expression est autorisé (aucune ligne affichée).

Exemples :
  java -cp out Main "a*" "" a ab
  java -cp out Main "a||b" ab                 (erreur, code 1)
  java -cp out Main --dot automates/automate_ababb.dot "(a|b)*abb" abb

----------------------------------------------------------------
4) Option --dot et images Graphviz
----------------------------------------------------------------
Les fichiers .dot sont écrits en UTF-8 avec la structure :

digraph AFN {
  rankdir=LR;
  node [shape=circle];
  __start [shape=point];
  __start -> 0;
  5 [shape=doublecircle];
  0 -> 1 [label="a"];
  1 -> 2 [label="ε"];
}

Commandes de génération des trois PNG du rapport :

dot -Tpng automates/automate_ababb.dot    -o automates/automate_ababb.png
dot -Tpng automates/automate_abplus.dot   -o automates/automate_abplus.png
dot -Tpng automates/automate_aquestionb.dot -o automates/automate_aquestionb.png

Les fichiers .dot correspondants sont déjà présents dans automates/.
Les trois PNG sont également générés dans automates/ (et images/) et sont
insérés dans le rapport, section 4. Graphviz est maintenant installé (dot 16.1.2).
L'arbre annoté arbre_annote_a_ou_bcetoile.png est inséré en section 2.3 avec
la même légende que le schéma fourni (h,n en bleu, s,n' en vert, fabriques en orange).
Le graphe de dépendances de ab|c est généré par :
  dot -Tpng images\dependances_ab_ou_c.dot -o images\dependances_ab_ou_c.png
L'image montre l'arbre syntaxique en pointillé gris, des flèches droites et
épaisses : bleues et libellées pour les attributs hérités, vertes et
libellées pour les attributs synthétisés. La légende, les valeurs détaillées
et l'ordre d'évaluation sont écrits dans le rapport, section 2.2.
Les arêtes parallèles (même source, même cible) sont regroupées en une
seule arête avec des labels séparés par des virgules ; l'ordre est ε
d'abord, puis lexicographique.

----------------------------------------------------------------
5) Jeu de tests
----------------------------------------------------------------
java -cp out TestRunner

Sortie attendue (extrait) :
Résumé : 16 expressions, 149 mots, 10 erreurs, 216 automates vérifiés
PASS : tous les tests sont corrects.

Le runner lit par défaut tests/tests.txt et tests/erreurs.txt.
Chemins personnalisés :

java -cp out TestRunner tests/tests.txt tests/erreurs.txt

Il vérifie :
  - chaque mot accepté / rejeté ;
  - chaque position d'erreur ;
  - stateCount() <= 2n pour n = SYMBOLE + @ + | + * + + + ? ;
  - initial sans transition entrante ;
  - acceptant sans transition sortante ;
  - numéros d'états exactement 0..N-1 ;
  - 200 expressions aléatoires à graine fixe (générateur maison).

----------------------------------------------------------------
6) Vérification complète de l'archive
----------------------------------------------------------------
Sous Bash / Git Bash / Linux / macOS :

bash verifier_archive.sh

Sous Windows cmd :

verifier_archive.bat

Les deux scripts :
  1. vérifient la présence des fichiers requis ;
  2. compilent avec javac -d out src/*.java ;
  3. lancent TestRunner ;
  4. régénèrent les trois .dot dans automates/ ;
  5. affichent un résumé PASS/FAIL et retournent un code non nul
     dès qu'une étape échoue.

La variable d'environnement JAVAC (Bash) / JAVA_HOME (batch) peut être
utilisée pour pointer vers un JDK 17 spécifique.

----------------------------------------------------------------
7) Extensions (fichiers séparés, rendu principal inchangé)
----------------------------------------------------------------
Point d'entrée unique :

java -cp out Extensions --dfa     <expression> [--dot fichier.dot] [mot...]
java -cp out Extensions --equiv   <expr1> <expr2> [--dot-prefix prefix]
java -cp out Extensions --recover <expression> [mot...]

(a) Déterminisation par sous-ensembles : Dfa.java, SubsetConstruction.java
    - AFD COMPLET : transition manquante -> état puits explicite ;
    - export .dot "digraph AFD" avec préfixe q et noms triés ;
    - DfaMain.accepts() gère aussi un symbole hors alphabet (puits).

    java -cp out DfaMain "(a|b)*abb" abb aabb ab
    java -cp out DfaMain "a*+?" "" a aa b
    java -cp out DfaMain --dot automates/dfa_ababb.dot "(a|b)*abb" abb

(b) Minimisation de Moore et équivalence : DfaMinimizer.java, EquivMain.java
    - partition {acceptants, non-acceptants}, raffinée jusqu'à stabilisation ;
    - renumérotation canonique par BFS avant comparaison ;
    - sortie EQUIVALENTS / DIFFERENTS, code 0 si la comparaison a réussi.

    java -cp out EquivMain "a*" "(a*)*"        -> EQUIVALENTS
    java -cp out EquivMain "a|b" "b|a"         -> EQUIVALENTS
    java -cp out EquivMain "a*" "a+"           -> DIFFERENTS
    java -cp out EquivMain "a?" "(@|a)" --dot-prefix automates/min_

(c) Reprise sur erreur : RecoveryLexer.java, RecoveryParser.java, RecoveryMain.java
    - jeton invalide -> INVALID, signalé puis sauté ;
    - ensembles de synchronisation = FOLLOW des non-terminaux ;
    - plusieurs erreurs affichées sur stderr, code 1 dès la première erreur.

    java -cp out RecoveryMain "a||b"    -> 1 erreur, position 3
    java -cp out RecoveryMain "a(+b"    -> 2 erreurs, positions 3 et 5
    java -cp out RecoveryMain "a#b" ab  -> erreur lexicale pos 2 + "ab : OUI"

Vérification des 9 tests d'extension :

bash verifier_extensions.sh
verifier_extensions.bat

Attention : le lanceur Java de Windows étend les jokers de la ligne de
commande ; avec JDK 27, "a*" peut devenir une liste de noms de fichiers
même depuis cmd. Les deux scripts (Bash et .bat) lancent donc Java depuis
out/ avec -cp . Pour reproduire à la main, faire :

  cd out
  java -cp . EquivMain "a*" "(a*)*"

ou utiliser une expression sans joker ambigu, par exemple "a+".
