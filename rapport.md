# Rapport — Du texte d’une expression régulière à l’automate

**Équipe :** Solo  
**Date :** 24/11/2026  
**Effectif :** 1 membres — répartition détaillée en §7.  
**Livrables :** `src/*.java`, `tests/tests.txt`, `tests/erreurs.txt`, `src/TestRunner.java`, `rapport.pdf`.

> Convention utilisée partout : `ε` désigne la production vide de la grammaire, `@` est le terminal « mot vide » du langage, `$` est la fin d’entrée. Les deux notions ε et `@` ne doivent pas être confondues.

---

## 1. Partie 1 — Grammaire LL(1)

### 1.1 FIRST, FOLLOW et FIRST des productions

| X | FIRST(X) | FOLLOW(X) |
|---|---|---|
| R | { (, SYMBOLE, @ } | { ), $ } |
| R' | { \|, ε } | { ), $ } |
| T | { (, SYMBOLE, @ } | { \|, ), $ } |
| T' | { (, SYMBOLE, @, ε } | { \|, ), $ } |
| F | { (, SYMBOLE, @ } | { (, SYMBOLE, @, \|, ), $ } |
| F' | { \*, +, ?, ε } | { (, SYMBOLE, @, \|, ), $ } |
| A | { (, SYMBOLE, @ } | { \|, \*, +, ?, (, ), SYMBOLE, @, $ } |

Calcul itératif : FIRST propage `A → F → T → R` et ajoute `ε` à `R'`, `T'`, `F'` ; FOLLOW part de `$ ∈ FOLLOW(R)`, ajoute `)` via `A → ( R )`, puis propage `|`, `)`, `$` de `R'` vers `T`, `T'`, `F`, `F'` et `A`. **Aucune divergence** avec les valeurs de contrôle fournies.

| Production | FIRST du membre droit |
|---|---|
| R → T R' ; T → F T' ; T' → F T' ; F → A F' | { (, SYMBOLE, @ } |
| R' → \| T R' | { \| } |
| R' → ε ; T' → ε ; F' → ε | { ε } |
| F' → \* F' / + F' / ? F' | { \* } / { + } / { ? } |
| A → ( R ) / SYMBOLE / @ | { ( } / { SYMBOLE } / { @ } |

`FOLLOW(A)` contient les 9 terminaux : après un atome, tout peut arriver — postfixe, facteur suivant, union, fermeture de parenthèse ou fin d’entrée.

### 1.2 Table prédictive M et LL(1)

| X \ a | \| | \* | + | ? | ( | ) | SYMBOLE | @ | $ |
|---|---|---|---|---|---|---|---|---|---|
| R | R → T R' | . | . | . | R → T R' | . | R → T R' | R → T R' | . |
| R' | R' → \| T R' | . | . | . | . | R' → ε | . | . | R' → ε |
| T | T → F T' | . | . | . | T → F T' | . | T → F T' | T → F T' | . |
| T' | T' → ε | . | . | . | T' → F T' | T' → ε | T' → F T' | T' → F T' | T' → ε |
| F | F → A F' | . | . | . | F → A F' | . | F → A F' | F → A F' | . |
| F' | F' → ε | F' → \* F' | F' → + F' | F' → ? F' | F' → ε | F' → ε | F' → ε | F' → ε | F' → ε |
| A | A → ( R ) | . | . | . | A → ( R ) | . | A → SYMBOLE | A → @ | . |

Vérification par ligne : `{ | } ∩ { ), $ } = ∅` pour `R'` ; `{ (, SYMBOLE, @ } ∩ { |, ), $ } = ∅` pour `T'` ; `{ * }`, `{ + }`, `{ ? }` et `{ (, SYMBOLE, @, |, ), $ }` sont disjoints pour `F'` ; `A` répartit `(`, `SYMBOLE`, `@` en trois cases distinctes. **Aucune case multiple : la grammaire est LL(1).** FOLLOW sert à placer les productions ε : on choisit `X → ε` quand le regard appartient à `FOLLOW(X)`, c’est-à-dire quand ce qui suit `X` est légal et qu’aucune autre production de `X` ne peut commencer par ce regard. Par exemple `*` n’appartient pas à `FOLLOW(F')`, donc `M[F', '*']` ne contient que `F' → '*' F'`, ce qui autorise `a*+?` sans ambiguïté.

### 1.3 Grammaire de départ et transformation

Grammaire de départ : `R → R '|' T | T`, `T → T F | F`, `F → F '*' | F '+' | F '?' | A`, `A → '(' R ')' | SYMBOLE | '@'`.

La règle `X → X α | β ⇔ X → β X'`, `X' → α X' | ε` a été appliquée trois fois :

| Non-terminal | β | α | Grammaire obtenue |
|---|---|---|---|
| R | T | \| T | `R → T R'`, `R' → \| T R' \| ε` |
| T | F | F | `T → F T'`, `T' → F T' \| ε` |
| F | A | \*, +, ? | `F → A F'`, `F' → \* F' \| + F' \| ? F' \| ε` |

L’associativité à gauche est conservée par repli sémantique à gauche (`a|b|c` → `((a|b)|c)`). La concaténation étant implicite, aucun jeton ne signale un facteur suivant : `T'` doit continuer quand le regard est dans `FIRST(F) = { (, SYMBOLE, @ }` et s’arrêter quand il est dans `FOLLOW(T') = { |, ), $ }`. Leur disjonction rend la décision LL(1).

### 1.4 Réponses aux 4 questions — détection des erreurs

| Entrée | Jeton fautif | Où | Nature | Jeton(s) attendu(s) |
|---|---|---|---|---|
| `a)` | `)` pos 2 | Pile vide, après `R' → ε` | Test final `lookahead == $` (case `M[R',')']` **non vide**) | `$` |
| `a\|\|b` | `\|` pos 3 | Appel de `T` par `R' → \| T R'` | Case vide `M[T,'\|']` | { (, SYMBOLE, @ } |

La première erreur n’est donc **pas** une case vide de la table mais un échec du contrôle de fin d’entrée ; la seconde est une véritable case vide, détectée dès la consultation de `M[T, '|']`. Le contrôle Java `FirstFollow.java` stabilise FIRST et FOLLOW par point fixe, construit `M`, signale toute case multiple et compare les six valeurs de contrôle : **0 conflit**.

---

## 2. Partie 3 — Traduction vers l’automate de Thompson

### 2.1 Schéma de traduction complet

`h` = fragment hérité (déjà construit à gauche), `n` = prochain état libre hérité ; `s` = fragment synthétisé, `n'` = prochain libre synthétisé.

| Production | Actions sémantiques |
|---|---|
| R → T R' | n(T)=n(R) ; h(R')=s(T) ; n(R')=n'(T) ; s(R)=s(R') ; n'(R)=n'(R') |
| R' → '\|' T R'₁ | n(T)=n(R') ; h(R'₁)=union(h(R'), s(T), n'(T)) ; n(R'₁)=n'(T)+2 ; s(R')=s(R'₁) ; n'(R')=n'(R'₁) |
| R' → ε | s(R')=h(R') ; n'(R')=n(R') |
| T → F T' | n(F)=n(T) ; h(T')=s(F) ; n(T')=n'(F) ; s(T)=s(T') ; n'(T)=n'(T') |
| T' → F T'₁ | n(F)=n(T') ; h(T'₁)=concat(h(T'), s(F), n'(F)) ; n(T'₁)=n'(F) ; s(T')=s(T'₁) ; n'(T')=n'(T'₁) |
| T' → ε | s(T')=h(T') ; n'(T')=n(T') |
| F → A F' | n(A)=n(F) ; h(F')=s(A) ; n(F')=n'(A) ; s(F)=s(F') ; n'(F)=n'(F') |
| F' → '\*' F'₁ | h(F'₁)=star(h(F'), n(F')) ; n(F'₁)=n(F')+2 ; s(F')=s(F'₁) ; n'(F')=n'(F'₁) |
| F' → '+' F'₁ | h(F'₁)=plus(h(F'), n(F')) ; n(F'₁)=n(F')+2 ; s(F')=s(F'₁) ; n'(F')=n'(F'₁) |
| F' → '?' F'₁ | h(F'₁)=optional(h(F'), n(F')) ; n(F'₁)=n(F')+2 ; s(F')=s(F'₁) ; n'(F')=n'(F'₁) |
| F' → ε | s(F')=h(F') ; n'(F')=n(F') |
| A → '(' R ')' | n(R)=n(A) ; s(A)=s(R) ; n'(A)=n'(R) |
| A → SYMBOLE | (s(A), n'(A)) = symbole(val, n(A)) |
| A → '@' | (s(A), n'(A)) = epsilon(n(A)) |

`union`, `star`, `plus`, `optional` allouent 2 états ; `symbole` et `@` allouent 2 états ; `concat` et les productions ε n’en allouent aucun.

### 2.2 Graphe de dépendances de `ab|c`

```text
R.n = 0
 └─ T.n = R.n = 0
     ├─ F(a).n = 0 ──> A(a).n = 0 ──> s(A)=(0,1), n'(A)=2
     │                 └─> F'(ε).h=(0,1), n=2 ──> s(F')=(0,1), n'=2
     ├─ T'.h = s(F)=(0,1), n = 2
     │   └─ F(b).n = 2 ──> s=(2,3), n'=4
     │       └─> concat((0,1),(2,3),4) = (0,3), n'=4
     └─ s(T)=(0,3), n'(T)=4
 └─ R'.h = s(T)=(0,3), n = 4
     ├─ T(c).n = 4 ──> s=(4,5), n'=6
     ├─ union((0,3),(4,5),6) alloue 6,7 ──> h(R'₁)=(6,7), n=8
     └─ R'(ε).h=(6,7), n=8 ──> s(R')=(6,7), n'=8
R.s = (6,7), R.n' = 8   (états exactement 0..7)
```

### 2.3 Arbre annoté de `a|bc*`

Notation `[h, n | s, n']`.

```text
R [⊥, 0 | (8,9), 10]
├─ T(a) [⊥, 0 | (0,1), 2]
│  ├─ A(a)      [⊥, 0     | (0,1), 2]
│  └─ F'(ε)     [(0,1), 2 | (0,1), 2]
├─ '|'
└─ R' [(0,1), 2 | (8,9), 10]
   ├─ T(bc*) [⊥, 2 | (2,7), 8]
   │  ├─ F(b)        [⊥, 2     | (2,3), 4]
   │  ├─ T'          [(2,3), 4 | (2,7), 8]
   │  │  ├─ F(c*)    [⊥, 4     | (6,7), 8]
   │  │  │  ├─ A(c)  [⊥, 4     | (4,5), 6]
   │  │  │  └─ F'(*) [(4,5), 6 | (6,7), 8]
   │  │  └─ T'(ε)    [(6,7), 8 | (6,7), 8]
   │  └─ concat((2,3),(6,7),8) = (2,7), n'=8
   ├─ union((0,1),(2,7),8) alloue 8,9
   └─ R'(ε) [(8,9), 10 | (8,9), 10]
```

### 2.4 Associativité et opérateur à précautions

Associativité préservée par repli à gauche : `a|b|c` → `((a|b)|c)`, `abc` → `((a b) c)`, `a*+?` → `(((a)*)+)?`. La récursion à droite des non-terminaux primés construit l’arbre, la sémantique replie à gauche.

**Opérateur à précautions : `?`** (et, plus généralement, `*`/`@` qui introduisent aussi une passe vide). `optional` ajoute une arête ε directe `i → f` **sans boucle**, alors que `plus` ajoute la boucle **sans passe vide**. Cette arête ε peut devenir parallèle à une autre arête ε (`a*+?`) et alimente des cycles dans `(a?)*`. Deux précautions : ne pas confondre cette ε de Thompson avec le terminal `@`, et utiliser une ε-fermeture itérative avec ensemble de visités.

---

## 3. Partie 4 — Numérotation des états sans variable globale

### 3.1 Attributs et schéma

`n` = prochain libre **hérité**, `n'` = prochain libre **synthétisé** (`Result.fragment`, `Result.nextFree`). Chaque procédure reçoit `n`, rend `Result`. `parse()` démarre à `0`.

```text
R  -> T R'      { T.n=R.n ; R'.h=T.s ; R'.n=T.n' ; R.s=R'.s ; R.n'=R'.n' }
R' -> '|' T R'₁ { T.n=R'.n ; R'₁.h=union(R'.h,T.s,T.n') ; R'₁.n=T.n'+2 ;
                  R'.s=R'₁.s ; R'.n'=R'₁.n' }
R' -> ε         { R'.s=R'.h ; R'.n'=R'.n }
T  -> F T'      { F.n=T.n ; T'.h=F.s ; T'.n=F.n' ; T.s=T'.s ; T.n'=T'.n' }
T' -> F T'₁     { F.n=T'.n ; T'₁.h=concat(T'.h,F.s,F.n') ; T'₁.n=F.n' ;
                  T'.s=T'₁.s ; T'.n'=T'₁.n' }
T' -> ε         { T'.s=T'.h ; T'.n'=T'.n }
F  -> A F'      { A.n=F.n ; F'.h=A.s ; F'.n=A.n' ; F.s=F'.s ; F.n'=F'.n' }
F' -> '*' F'₁   { F'₁.h=star(F'.h,F'.n) ; F'₁.n=F'.n+2 ; F'.s=F'₁.s ; F'.n'=F'₁.n' }
F' -> '+' F'₁   { F'₁.h=plus(F'.h,F'.n) ; F'₁.n=F'.n+2 ; F'.s=F'₁.s ; F'.n'=F'₁.n' }
F' -> '?' F'₁   { F'₁.h=optional(F'.h,F'.n) ; F'₁.n=F'.n+2 ; F'.s=F'₁.s ; F'.n'=F'₁.n' }
F' -> ε         { F'.s=F'.h ; F'.n'=F'.n }
A  -> '(' R ')' { R.n=A.n ; A.s=R.s ; A.n'=R.n' }
A  -> SYMBOLE   { (A.s,A.n')=symbole(val,A.n) }
A  -> '@'       { (A.s,A.n')=epsilon(A.n) }
```

### 3.2 Exemple chiffré `a|b*`

Circulation : `0 → 2` (a) `→ 4` (b) `→ 6` (étoile) `→ 8` (union). `nextFree = 8`, donc les états sont exactement `0..7`, soit `2n` avec `n = 4` (`a`, `|`, `b`, `*`). `Nfa.of()` vérifie `stateCount() == nextFree` et la présence de chaque `i` dans `0..nextFree-1`.

### 3.3 L-attribué et compteur global

Un attribut hérité d’un enfant ne dépend que du parent et/ou du frère gauche : `T.n = R.n`, `F.n = T.n`, `R.n = A.n` (cas `'(' R ')'`) ; `R'₁.n = T.n'`, `R'₁.h = union(R'.h, T.s, T.n')` — parent + frère gauche. Aucun `n` ne dépend d’un frère droit ni d’un descendant : le schéma est bien **L-attribué**, pas S-attribué.

| Risque du compteur global | Conséquence |
|---|---|
| Réentrance | Un `parse()` imbriqué reprendrait le compteur déjà avancé : sous-automate décalé et numéros réutilisés. |
| Tests parallèles | Data race sur un `int` partagé : numéros non déterministes, assertions flaky. |
| Deux automates dans un programme | Le second hérite du `nextFree` du premier : plus de `0..N-1`, comparaisons DOT fausses. |
| Ordre d’évaluation | Résultat dépendant de l’ordre des appels aux fabriques ; le passage explicite de `n → n'` impose un ordre reproductible. |

Deux erreurs classiques évitées : **numéros en double** (une fabrique n’alloue que `n` et `n+1`, et `n` circule linéairement, jamais forké) et **numéro sauté** (`n' = n + nombre réel d’états créés`, avec `+2` pour les fabriques qui allouent et `+0` pour `concat`/ε).

---

## 4. Automates générés (images Graphviz insérées)

Commande de génération : `dot -Tpng automates/automate_ababb.dot -o automates/automate_ababb.png` (idem pour les deux autres). Les fichiers `.dot` correspondants sont produits dans `automates/` et les PNG obtenus sont insérés ci-dessous.

### 4.1 `(a|b)*abb` — 14 états, initial 6, acceptant 13

IMG|automates/automate_ababb.png|Automate de `(a|b)*abb` (14 états).

Construction : l’union `a|b` alloue `6,7` ; l’étoile alloue `4,5` et ajoute boucle `5→4` et passe vide `4→5` ; la concaténation ajoute seulement des ε (`7→8`, `9→10`, `11→12`) ; le suffixe `a`, `b`, `b` alloue `8..13`. L’état acceptant `13` n’a aucune sortie ; l’état initial `6` n’a aucune entrée.

### 4.2 `(ab)+` — 6 états, initial 4, acceptant 5

IMG|automates/automate_abplus.png|Automate de `(ab)+` (6 états).

Construction : `a` (0,1) et `b` (2,3) sont concaténés par `1→2` ; le `+` alloue `4,5`, ajoute l’entrée `4→0`, la boucle `3→0` et la sortie `3→5`. **Aucune passe vide `4→5`** : le mot vide est rejeté, ce qui distingue `+` de `*`.

### 4.3 `a?b` — 6 états, initial 2, acceptant 5

IMG|automates/automate_aquestionb.png|Automate de `a?b` (6 états).

Construction : le `?` alloue `2,3` avec la passe vide `2→3` et l’entrée `2→0` ; `b` alloue `4,5` ; la concaténation ajoute `3→4`. On voit ici l’arête ε directe caractéristique de l’optionnel, sans boucle — l’opérateur à précautions du §2.4.

---

## 5. Jeu de tests

### 5.1 Les trois expressions inventées

| Expression | Cas limite | Justification |
|---|---|---|
| `(((a*)*)*)` | Triple étoile | Stabilité de la fermeture ε ; accepte `a*`, rejette `b`. |
| `a\|b\|c` | Associativité de `\|` | Trois branches par `R'` récursif ; accepte `a`, `b`, `c`, rejette `@` et `ab`. |
| `a*+?` | Enchaînement de postfixes | `(a*)+? = a*` ; valide car `F'` peut enchaîner `*`, `+`, `?`. |

Jeu complété par `((a|b)*c)+d?` (imbrication), `(a|b|c|d)+` (alternances multiples), `(A1|b2)*C` (majuscules et chiffres) et `(a?)*` (boucle d’ε). Total : **16 expressions, 149 mots** simulés un par un.

### 5.2 Tests d’erreur

| Expression | Position | Expression | Position |
|---|---|---|---|
| `(ab` | 4 | `ab)` | 3 |
| `*a` | 1 | `a\|\|b` | 3 |
| `()` | 2 | `\|a` | 1 |
| `a(` | 3 | `a\|*` | 3 |
| `(a\|)` | 4 | `a#b` | 2 |

`EOF` est à `longueur + 1` : `(ab` → 4. `a#b` est une erreur lexicale ; le runner compare uniquement la position.

### 5.3 Propriété « au plus 2n états »

`n` compte `SYMBOLE`, `@`, `|`, `*`, `+`, `?` (pas les parenthèses). `TestRunner` applique le contrôle aux 16 expressions du fichier **et à 200 expressions aléatoires** générées avec une LCG 48 bits à graine fixe. Pour chaque automate : `stateCount() ≤ 2n` (égalité observée dans tous les cas), initial sans transition entrante, acceptant sans transition sortante, et numéros exactement `0..N-1`.

Résultat : `Résumé : 16 expressions, 149 mots, 10 erreurs, 216 automates vérifiés` — `PASS`, code de retour `0`.

---

## 6. Difficultés rencontrées et choix de conception

1. La distinction entre `ε` (production vide de la grammaire) et `@` (mot vide du langage) a-t-elle été une source de confusion lors de la lecture de l’énoncé ?
2. Le choix de faire circuler `n` comme attribut plutôt que d’utiliser un compteur `static` a-t-il été discuté, notamment à cause des tests parallèles et de la réentrance ?
3. L’enchaînement des postfixes (`a*+?`) a-t-il nécessité une clarification de la grammaire (`F'` récursif) avant l’implémentation ?
4. L’ordre d’affichage des labels sur les arêtes parallèles du DOT (ε d’abord, puis ordre lexicographique, doublons conservés) a-t-il demandé un arbitrage ?
5. La génération du PDF et l’insertion des images Graphviz ont-elles posé problème (Graphviz absent sur certaines machines, encodage UTF-8 de `ε`) ?

---

## 7. Fiche de répartition

| Membre | Parties écrites | Parties relues |
|---|---|---|
| [Mouafek Errachi] | Toutes les Parties | Toutes les Parties|


---

## 8. Ressources externes, y compris outils d’assistance

| Ressource | Ce qui a été utilisé | Modification ou Correction|
|---|---|---|
| Cours / support de TP | Énoncé des parties 1 à 6, grammaire imposée, propriété `2n`, format DOT ||
| Documentation Graphviz | `digraph`, `rankdir=LR`, `shape=point`, `doublecircle`, commande `dot -Tpng` ||
| Documentation Java 17 | `record`, `switch` expression, `Files.writeString`, `StandardCharsets.UTF_8` ||
| Assistant IA — **Big Pickle** (`opencode/big-pickle`) | Aide à la rédaction/refactorisation du code, au jeu de tests et à la génération du PDF |La squelette pour générer le PDF est retenu et des tests ont été faites pour vérifier le code|
| Outils locaux | `javac -d out src/*.java`, `java -cp out TestRunner`, Edge headless pour l’export PDF |

