# Extensions — compléments pour le rapport

> Ces trois extensions sont livrées dans des fichiers séparés : `Main.java`,
> `Parser.java`, `TestRunner.java` et `rapport.pdf` restent inchangés. Les tests
> automatisés sont dans `verifier_extensions.sh` et `verifier_extensions.bat`.

## Extension (a) — Déterminisation par sous-ensembles

**Option CLI :** `java -cp out Extensions --dfa <expression> [--dot fichier.dot] [mot...]`
(alias direct : `java -cp out DfaMain ...`).

**Trois tests :**

1. `DfaMain "(a|b)*abb" abb aabb ab` → `abb : OUI`, `aabb : OUI`, `ab : NON`.
2. `DfaMain "a*+?" "" a aa b` → `"" : OUI`, `a : OUI`, `aa : OUI`, `b : NON`.
3. `DfaMain "(a?)*" "" a aa b` → `"" : OUI`, `a : OUI`, `aa : OUI`, `b : NON` ; l'export `--dot automates/dfa_ababb.dot` contient `digraph AFD`.

**Paragraphe pour le rapport :** La déterminisation de Rabin-Scott part de la ε-fermeture de l'état initial de l'AFN de Thompson, puis calcule pour chaque sous-ensemble et chaque symbole de l'alphabet l'ε-fermeture des cibles. Les sous-ensembles sont représentés par des listes triées d'états, ce qui fournit une clé de hachage canonique et une numérotation par ordre de découverte en largeur. L'AFD obtenu est **complet** : toute transition absente mène à un état puits explicite, représenté dans l'export par `q… [label="puits"]`. Le fichier `.dot` utilise le même regroupement des arêtes que l'AFN (labels triés), mais avec le préfixe `q` pour distinguer les états déterministes. Les trois exemples vérifient successivement une alternance avec boucle, un enchaînement de postfixes équivalent à `a*`, et une boucle d'ε : tous conduisent à des AFD dont la simulation accepte exactement les mêmes mots que l'AFN d'origine.

## Extension (b) — Minimisation de Moore et équivalence

**Option CLI :** `java -cp out Extensions --equiv <expr1> <expr2> [--dot-prefix prefix]`
(alias direct : `java -cp out EquivMain ...`).

**Trois tests :**

1. `EquivMain "a*" "(a*)*"` → `AFD minimaux : 2 et 2 états`, `EQUIVALENTS`.
2. `EquivMain "a|b" "b|a"` → `AFD minimaux : 4 et 4 états`, `EQUIVALENTS`.
3. `EquivMain "a*" "a+"` → `AFD minimaux : 2 et 2 états`, `DIFFERENTS` (le mot vide sépare les deux langages).

**Paragraphe pour le rapport :** L'algorithme de Moore part de la partition en deux classes (états acceptants et non acceptants), puis raffine itérativement chaque classe selon la signature `(classe courante, classes des cibles pour chaque symbole de l'alphabet)`. Le raffinement s'arrête quand la partition ne change plus ; le quotient est ensuite renuméroté par parcours en largeur depuis l'état initial, ce qui rend la représentation **canonique** : deux automates isomorphes reçoivent exactement les mêmes numéros. L'équivalence de deux expressions est alors décidée par l'égalité structurelle de leurs AFD minimaux complets, et non par un ensemble fini de mots tests. Les exemples montrent que les boucles d'ε redondantes de `(a*)*` et la commutativité de `a|b` sont reconnues, tandis que la différence sur le mot vide entre `a*` et `a+` est détectée malgré un nombre d'états identique.

## Extension (c) — Reprise sur erreur en mode panique

**Option CLI :** `java -cp out Extensions --recover <expression> [mot...]`
(alias direct : `java -cp out RecoveryMain ...`).

**Trois tests :**

1. `RecoveryMain "a||b"` → une erreur en position 3, code de retour 1.
2. `RecoveryMain "a(+b"` → deux erreurs (positions 3 et 5), code de retour 1.
3. `RecoveryMain "a#b" ab` → erreur lexicale en position 2 puis `ab : OUI`, code de retour 1.

**Paragraphe pour le rapport :** Le parseur de reprise est indépendant du parseur principal : il conserve la grammaire LL(1), les attributs `n → n'` et la construction de fragments, mais ne s'arrête pas à la première erreur. Un lexeur dédié transforme tout caractère hors alphabet en jeton `INVALID` au lieu de lever une exception, ce qui permet de continuer l'analyse. À chaque erreur, le parseur émet le message habituel puis se synchronise sur l'ensemble `FOLLOW` du non-terminal courant : `{), $}` pour `R` et `R'`, `{|, ), $}` pour `T` et `T'`, l'ensemble élargi pour `F`/`F'`, et les neuf terminaux de `FOLLOW(A)`. Quand le jeton courant n'appartient pas à cet ensemble, les jetons sont sautés jusqu'à y entrer ou atteindre EOF ; la progression est donc garantie et aucune boucle infinie n'est possible. L'AFN est tout de même construit en insérant un fragment ε pour l'opérande manquant, ce qui permet de simuler les mots fournis même après une erreur.
