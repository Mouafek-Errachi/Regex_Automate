# Partie 1 — Grammaire LL(1)

> **Convention importante.** Dans ce qui suit, `ε` désigne la **production vide de la grammaire** (dérivation sans consommer de jeton), tandis que `@` est un **terminal du langage** : le mot vide. Ce sont deux notions différentes. `$` est la fin d’entrée.

Grammaire étudiée :

```
R  -> T R'
R' -> '|' T R' | ε
T  -> F T'
T' -> F T' | ε
F  -> A F'
F' -> '*' F' | '+' F' | '?' F' | ε
A  -> '(' R ')' | SYMBOLE | '@'
```

---

## 1) Calcul manuel de FIRST et FOLLOW

### Règles utilisées

**FIRST.** Pour `X -> Y1 Y2 ... Yk` :
1. on ajoute `FIRST(Y1) \ {ε}` ;
2. si `ε ∈ FIRST(Y1)`, on passe à `Y2`, puis `Y3`, etc. ;
3. si tous les `Yi` peuvent être vides (ou si `k = 0`), on ajoute `ε` à `FIRST(X)`.

**FOLLOW.** `$ ∈ FOLLOW(R)` (axiome). Pour `X -> α Y β` avec `Y` non-terminal :
1. on ajoute `FIRST(β) \ {ε}` à `FOLLOW(Y)` ;
2. si `ε ∈ FIRST(β)` (ou si `β` est vide), on ajoute aussi `FOLLOW(X)` à `FOLLOW(Y)`.

### Étapes du calcul de FIRST

Initialisation : tous les ensembles sont vides.

**Passe 1** (les dépendances se propagent en plusieurs passes, l’ordre d’écriture n’étant pas optimal) :

- `A -> '(' R ')' | SYMBOLE | '@'` donne directement `FIRST(A) = { (, SYMBOLE, @ }`.
- `F' -> '*' F' | '+' F' | '?' F' | ε` donne `FIRST(F') = { *, +, ?, ε }` (les récursions sur `F'` n’ajoutent rien de nouveau).
- `R' -> '|' T R' | ε` donne `FIRST(R') = { |, ε }`.
- `T' -> F T' | ε` donnera `FIRST(T') = FIRST(F) ∪ { ε }`.
- `F -> A F'` : `A` n’est pas nullable, donc `FIRST(F) = FIRST(A)`.
- `T -> F T'` : `F` n’est pas nullable, donc `FIRST(T) = FIRST(F)`.
- `R -> T R'` : `T` n’est pas nullable, donc `FIRST(R) = FIRST(T)`.

**Passes suivantes** : propagation de `A` vers `F`, de `F` vers `T`, de `T` vers `R`. Stabilisation : aucune modification.

**Résultat**

| Non-terminal | FIRST |
|---|---|
| R | { (, SYMBOLE, @ } |
| R' | { \|, ε } |
| T | { (, SYMBOLE, @ } |
| T' | { (, SYMBOLE, @, ε } |
| F | { (, SYMBOLE, @ } |
| F' | { \*, +, ?, ε } |
| A | { (, SYMBOLE, @ } |

**Confrontation aux valeurs de contrôle : aucune divergence.** On retrouve bien `FIRST(R)=FIRST(T)=FIRST(F)=FIRST(A)={ '(', SYMBOLE, '@' }`, `FIRST(R')={ '|', ε }` et `FIRST(F')={ '*', '+', '?', ε }`. La valeur supplémentaire `FIRST(T') = { '(', SYMBOLE, '@', ε }` est cohérente avec la production `T' -> ε`.

### Étapes du calcul de FOLLOW

- Initialisation : `FOLLOW(R) = { $ }`.
- `A -> '(' R ')'` : `R` est suivi de `)` → `FOLLOW(R) = { $, ) }`.
- `R -> T R'` : `T` est suivi de `R'` avec `FIRST(R') = { |, ε }` → on ajoute `|` **et** `FOLLOW(R)` à `FOLLOW(T)` : `FOLLOW(T) = { |, ), $ }`. `R'` est en fin de membre droit → `FOLLOW(R') = FOLLOW(R) = { ), $ }`.
- `R' -> '|' T R'` : même ajout pour `T`, et `FOLLOW(R')` reste inchangé.
- `T -> F T'` : `T'` en fin → `FOLLOW(T') = FOLLOW(T) = { |, ), $ }`.
- `T' -> F T'` : `F` est suivi de `T'` avec `ε ∈ FIRST(T')` → on ajoute `FIRST(T') \ {ε} = { (, SYMBOLE, @ }` **et** `FOLLOW(T')` à `FOLLOW(F)` : `FOLLOW(F) = { (, SYMBOLE, @, |, ), $ }`.
- `F -> A F'` : `A` est suivi de `F'` avec `ε ∈ FIRST(F')` → on ajoute `{ *, +, ? }` et `FOLLOW(F)` à `FOLLOW(A)`. `F'` en fin → `FOLLOW(F') = FOLLOW(F)`.
- `F' -> '*' F' | ...` : `F'` est suivi de rien → `FOLLOW(F')` s’ajoute à lui-même, rien de nouveau.
- Nouvelle passe : `)` propagé par `A -> '(' R ')'` remonte dans `FOLLOW(T)`, puis `FOLLOW(T')`, puis `FOLLOW(F)`, puis `FOLLOW(A)` et `FOLLOW(F')`. Stabilisation.

**Résultat**

| Non-terminal | FOLLOW |
|---|---|
| R | { ), $ } |
| R' | { ), $ } |
| T | { \|, ), $ } |
| T' | { \|, ), $ } |
| F | { (, SYMBOLE, @, \|, ), $ } |
| F' | { (, SYMBOLE, @, \|, ), $ } |
| A | { \|, \*, +, ?, (, ), SYMBOLE, @, $ } |

`FOLLOW(A)` contient les 9 terminaux : après un atome, tout peut arriver — un postfixe, un autre facteur, une union, une fermeture de parenthèse ou la fin d’entrée.

### FIRST de chaque production

| Production | FIRST du membre droit |
|---|---|
| R → T R' | { (, SYMBOLE, @ } |
| R' → \| T R' | { \| } |
| R' → ε | { ε } |
| T → F T' | { (, SYMBOLE, @ } |
| T' → F T' | { (, SYMBOLE, @ } |
| T' → ε | { ε } |
| F → A F' | { (, SYMBOLE, @ } |
| F' → \* F' | { \* } |
| F' → + F' | { + } |
| F' → ? F' | { ? } |
| F' → ε | { ε } |
| A → ( R ) | { ( } |
| A → SYMBOLE | { SYMBOLE } |
| A → @ | { @ } |

---

## 2) Table d’analyse prédictive M

Règle de remplissage :

- pour chaque `a ∈ FIRST(α) \ {ε}`, on met `M[X, a] = X -> α` ;
- si `ε ∈ FIRST(α)`, alors pour chaque `b ∈ FOLLOW(X)`, on met `M[X, b] = X -> α`.

| X \ a | \| | \* | + | ? | ( | ) | SYMBOLE | @ | $ |
|---|---|---|---|---|---|---|---|---|---|
| R | R → T R' | . | . | . | R → T R' | . | R → T R' | R → T R' | . |
| R' | R' → \| T R' | . | . | . | . | R' → ε | . | . | R' → ε |
| T | T → F T' | . | . | . | T → F T' | . | T → F T' | T → F T' | . |
| T' | T' → ε | . | . | . | T' → F T' | T' → ε | T' → F T' | T' → F T' | T' → ε |
| F | F → A F' | . | . | . | F → A F' | . | F → A F' | F → A F' | . |
| F' | F' → ε | F' → \* F' | F' → + F' | F' → ? F' | F' → ε | F' → ε | F' → ε | F' → ε | F' → ε |
| A | A → ( R ) | . | . | . | A → ( R ) | . | A → SYMBOLE | A → @ | . |

Sur 63 cases, 30 sont remplies et 33 sont vides.

### Vérification case par case

| Ligne | Colonnes utilisées par chaque production | Intersection |
|---|---|---|
| R | `R → T R'` : { (, SYMBOLE, @ } | une seule production |
| R' | `→ \| T R'` : { \| } ; `→ ε` : FOLLOW(R') = { ), $ } | ∅ |
| T | `T → F T'` : { (, SYMBOLE, @ } | une seule production |
| T' | `→ F T'` : { (, SYMBOLE, @ } ; `→ ε` : FOLLOW(T') = { \|, ), $ } | ∅ |
| F | `F → A F'` : { (, SYMBOLE, @ } | une seule production |
| F' | `→ \* F'` : { \* } ; `→ + F'` : { + } ; `→ ? F'` : { ? } ; `→ ε` : FOLLOW(F') = { (, SYMBOLE, @, \|, ), $ } | ∅ deux à deux |
| A | `→ ( R )` : { ( } ; `→ SYMBOLE` : { SYMBOLE } ; `→ @` : { @ } | ∅ deux à deux |

Aucune case ne contient deux productions : **la grammaire est LL(1)**.

### Rôle de FOLLOW pour les productions ε

Une production `X -> ε` est choisie exactement quand le jeton courant ne peut pas commencer une autre production de `X` mais **peut légalement suivre `X`** : c’est précisément `FOLLOW(X)`.

- `R' -> ε` sur `)` ou `$` : permet de refermer `( R )` ou de terminer l’expression.
- `T' -> ε` sur `|`, `)` ou `$` : rend la main à `R'` pour l’union, ou termine le facteur courant.
- `F' -> ε` sur `(`, SYMBOLE, `@` : permet un facteur suivant (`a* b`, `(a)b`) ; sur `|`, `)`, `$` : termine la chaîne de postfixes.

Le point essentiel est la **disjonction** : `FIRST` des alternatives non-ε et `FOLLOW` de la production ε ne se recouvrent jamais ici. Par exemple `*` n’appartient pas à `FOLLOW(F')`, donc `M[F', '*']` ne contient que `F' -> '*' F'`, ce qui autorise `a*+?` sans ambiguïté.

---

## 3) Grammaire de départ et transformation

### Grammaire initiale (récursive à gauche)

```
R  -> R '|' T | T
T  -> T F | F
F  -> F '*' | F '+' | F '?' | A
A  -> '(' R ')' | SYMBOLE | '@'
```

- `R -> R '|' T | T` : union **associative à gauche** ;
- `T -> T F | F` : concaténation implicite **associative à gauche** ;
- `F -> F '*' | F '+' | F '?' | A` : postfixes **enchaînables** et associatifs à gauche.

### Transformation appliquée

Règle générale : `A -> A α | β` devient `A -> β A'`, `A' -> α A' | ε`. Avec plusieurs `αi` et plusieurs `βj` : `A -> β1 A' | ... | βk A'` et `A' -> α1 A' | ... | αn A' | ε`.

| Non-terminal | α (queue répétée) | β (premier terme) | Résultat |
|---|---|---|---|
| R | `\| T` | `T` | `R -> T R'` ; `R' -> \| T R' \| ε` |
| T | `F` | `F` | `T -> F T'` ; `T' -> F T' \| ε` |
| F | `*`, `+`, `?` | `A` | `F -> A F'` ; `F' -> \* F' \| + F' \| ? F' \| ε` |

La récursivité à gauche est éliminée : la répétition est stockée dans le nouveau non-terminal. L’**associativité à gauche est préservée sémantiquement** : l’arbre devient à branchement droit, mais la construction de l’AST se fait par repli à gauche — `a|b|c` donne `((a|b)|c)`, `abc` donne `((a b) c)`, `a*+?` donne `(((a)*)+)?`.

### Pourquoi tester FIRST(F) dans T' ?

La concaténation n’a **aucun jeton visible**. Dans `T' -> F T' | ε`, rien ne dit « un autre facteur commence ici » à part le jeton lui-même. Or un facteur ne peut commencer que par `FIRST(F) = { (, SYMBOLE, @ }`.

- regard dans `FIRST(F)` → on choisit `T' -> F T'` ;
- regard dans `FOLLOW(T') = { |, ), $ }` → on choisit `T' -> ε`.

Comme `FIRST(F) ∩ FOLLOW(T') = ∅`, la décision est déterministe avec **un seul jeton de regard**. Exemples : dans `ab`, après `a`, le `SYMBOLE` `b` force la continuation ; dans `a|b`, le `|` force `ε` et rend la main à `R'`.

---

## 4) Détection des erreurs

### Entrée `a)`

Jetons : `SYMBOLE(a, pos 1)`, `')'(pos 2)`, `$`.

Trace : `R → T R' → F T' → A F'` ; `A → SYMBOLE` consomme `a` ; puis `F' → ε` sur `)` (car `) ∈ FOLLOW(F')`), `T' → ε` sur `)`, `R' → ε` sur `)`.

- **Jeton fautif** : `)` à la position 2.
- **Procédure en cours** : plus aucune procédure de non-terminal n’est active ; c’est le **contrôle final de fin d’entrée** du pilote (`lookahead == $`) qui échoue, pile vide.
- **Attendus** : uniquement `$`.
- **Case vide ?** **Non.** `M[R', ')'] = R' -> ε` existe (ainsi que `M[F',')']` et `M[T',')']`). L’erreur n’est donc **pas** détectée par la table, mais par le test final.

### Entrée `a||b`

Jetons : `SYMBOLE(a, 1)`, `'|'(2)`, `'|'(3)`, `SYMBOLE(b, 4)`, `$`.

Trace : après `a`, `F' → ε` et `T' → ε` sur le premier `|` ; `R'` choisit `R' -> '|' T R'` et consomme le `|` de position 2 ; on appelle alors `T` avec le **second `|`** en regard.

- **Jeton fautif** : `|` à la position 3.
- **Procédure en cours** : `T` (appelé par la production `R' -> '|' T R'`).
- **Attendus** : `FIRST(T) = { (, SYMBOLE, @ }`, c’est-à-dire les seules cases non vides `M[T,'(']`, `M[T,SYMBOLE]`, `M[T,'@']`.
- **Case vide ?** **Oui.** `M[T, '|'] = ∅` : l’erreur est détectée directement par le pilote sur case vide.

**Distinction résumée**

| Entrée | Jeton fautif | Mécanisme | Attendus |
|---|---|---|---|
| `a)` | `)` pos 2 | Test final `$` (case `M[R',')']` non vide) | `$` |
| `a\|\|b` | `\|` pos 3 | Case vide `M[T,'\|']` | { (, SYMBOLE, @ } |

---

## 5) Vérification Java : `src/FirstFollow.java`

```java
import java.util.*;

/**
 * Verification independante des calculs FIRST / FOLLOW de la grammaire LL(1) du TP.
 * Compilation : javac -d out src/FirstFollow.java
 * Execution   : java -cp out FirstFollow
 *
 * Seules des constantes immuables sont "static" : aucun champ modifiable,
 * aucun compteur global. Les nombres de passes sont des variables locales.
 */
public class FirstFollow {
    private static final String EPS = "EPS";      // epsilon en interne
    private static final String END = "$";
    private static final List<String> TERMINALS =
            List.of("|", "*", "+", "?", "(", ")", "SYMBOLE", "@", END);
    private static final List<String> ORDER =
            List.of("|", "*", "+", "?", "(", ")", "SYMBOLE", "@", END, EPS);

    /** Une production : lhs -> rhs (rhs vide = epsilon). Enregistrement immuable. */
    private record Prod(String lhs, List<String> rhs) {
        String label() {
            return lhs + " -> " + (rhs.isEmpty() ? "ε" : String.join(" ", rhs));
        }
    }

    public static void main(String[] args) {
        new FirstFollow().run();
    }

    private void run() {
        List<String> nonTerminals = List.of("R", "R'", "T", "T'", "F", "F'", "A");
        Set<String> nt = new LinkedHashSet<>(nonTerminals);

        List<Prod> prods = List.of(
                new Prod("R",  List.of("T", "R'")),
                new Prod("R'", List.of("|", "T", "R'")),
                new Prod("R'", List.<String>of()),
                new Prod("T",  List.of("F", "T'")),
                new Prod("T'", List.of("F", "T'")),
                new Prod("T'", List.<String>of()),
                new Prod("F",  List.of("A", "F'")),
                new Prod("F'", List.of("*", "F'")),
                new Prod("F'", List.of("+", "F'")),
                new Prod("F'", List.of("?", "F'")),
                new Prod("F'", List.<String>of()),
                new Prod("A",  List.of("(", "R", ")")),
                new Prod("A",  List.of("SYMBOLE")),
                new Prod("A",  List.of("@"))
        );

        // ---------- FIRST : point fixe ----------
        Map<String, Set<String>> first = new LinkedHashMap<>();
        for (String n : nonTerminals) {
            first.put(n, new LinkedHashSet<>());
        }
        boolean changed = true;
        int firstPasses = 0;
        while (changed) {
            changed = false;
            firstPasses++;
            for (Prod p : prods) {
                if (first.get(p.lhs()).addAll(firstOfSequence(p.rhs(), first, nt))) {
                    changed = true;
                }
            }
        }

        // ---------- FOLLOW : point fixe ----------
        Map<String, Set<String>> follow = new LinkedHashMap<>();
        for (String n : nonTerminals) {
            follow.put(n, new LinkedHashSet<>());
        }
        follow.get("R").add(END);
        changed = true;
        int followPasses = 0;
        while (changed) {
            changed = false;
            followPasses++;
            for (Prod p : prods) {
                List<String> rhs = p.rhs();
                for (int i = 0; i < rhs.size(); i++) {
                    String y = rhs.get(i);
                    if (!nt.contains(y)) {
                        continue; // terminal : rien a ajouter
                    }
                    Set<String> after =
                            firstOfSequence(rhs.subList(i + 1, rhs.size()), first, nt);
                    Set<String> add = new LinkedHashSet<>(after);
                    add.remove(EPS);
                    if (after.contains(EPS)) {
                        add.addAll(follow.get(p.lhs()));
                    }
                    if (follow.get(y).addAll(add)) {
                        changed = true;
                    }
                }
            }
        }

        // ---------- Affichage ----------
        System.out.println("FIRST  (point fixe en " + firstPasses + " passes)");
        for (String n : nonTerminals) {
            System.out.println("  FIRST(" + n + ")  = " + show(first.get(n)));
        }
        checkControl(first);

        System.out.println("FOLLOW (point fixe en " + followPasses + " passes)");
        for (String n : nonTerminals) {
            System.out.println("  FOLLOW(" + n + ") = " + show(follow.get(n)));
        }

        System.out.println("FIRST des productions");
        for (Prod p : prods) {
            System.out.println("  " + p.label() + " : "
                    + show(firstOfSequence(p.rhs(), first, nt)));
        }

        printPredictiveTable(prods, first, follow, nonTerminals);
    }

    /** FIRST d'une suite de symboles ; EPS present si la suite peut disparaitre. */
    private Set<String> firstOfSequence(List<String> seq,
                                        Map<String, Set<String>> first,
                                        Set<String> nonTerminals) {
        Set<String> result = new LinkedHashSet<>();
        for (String sym : seq) {
            if (!nonTerminals.contains(sym)) { // terminal
                result.add(sym);
                return result;
            }
            Set<String> fs = first.get(sym);
            for (String t : fs) {
                if (!t.equals(EPS)) result.add(t);
            }
            if (!fs.contains(EPS)) {
                return result; // symbole non nullable : on s'arrete ici
            }
            // sinon ce symbole peut etre vide : on examine le suivant
        }
        result.add(EPS); // sequence vide, ou tous les symboles sont nullables
        return result;
    }

    /** Table M[X, terminal] ; un conflit est signale des qu'une case a 2 productions. */
    private void printPredictiveTable(List<Prod> prods,
                                      Map<String, Set<String>> first,
                                      Map<String, Set<String>> follow,
                                      List<String> nonTerminals) {
        Map<String, Map<String, List<String>>> m = new LinkedHashMap<>();
        for (String n : nonTerminals) {
            Map<String, List<String>> row = new LinkedHashMap<>();
            for (String t : TERMINALS) row.put(t, new ArrayList<>());
            m.put(n, row);
        }
        Set<String> nt = m.keySet();

        for (Prod p : prods) {
            Set<String> f = firstOfSequence(p.rhs(), first, nt);
            for (String t : f) {
                if (!t.equals(EPS)) add(m, p, t);
            }
            if (f.contains(EPS)) {
                for (String t : follow.get(p.lhs())) add(m, p, t);
            }
        }

        System.out.println("Table predictive M ('.' = case vide, '!!' = conflit)");
        System.out.printf("%-8s", "X \\ a");
        for (String t : TERMINALS) System.out.printf("%-14s", t);
        System.out.println();
        for (String n : nonTerminals) {
            System.out.printf("%-8s", n);
            for (String t : TERMINALS) {
                List<String> cell = m.get(n).get(t);
                String v;
                if (cell.isEmpty()) v = ".";
                else if (cell.size() > 1) v = "!! " + String.join(" / ", cell);
                else v = cell.get(0);
                if (v.length() > 14) v = v.substring(0, 13) + "~";
                System.out.printf("%-14s", v);
            }
            System.out.println();
        }
        int conflicts = 0;
        for (String n : nonTerminals) {
            for (String t : TERMINALS) {
                if (m.get(n).get(t).size() > 1) conflicts++;
            }
        }
        System.out.println("Cases multiples : " + conflicts
                + (conflicts == 0 ? " -> grammaire LL(1) confirmee."
                                  : " -> grammaire non LL(1)."));
    }

    private void add(Map<String, Map<String, List<String>>> m, Prod p, String terminal) {
        List<String> cell = m.get(p.lhs()).get(terminal);
        if (!cell.contains(p.label())) cell.add(p.label());
    }

    /** Confronte les valeurs de controle donnees dans le sujet. */
    private void checkControl(Map<String, Set<String>> first) {
        System.out.println("Controle des valeurs imposees");
        expect(first, "R",  "(", "SYMBOLE", "@");
        expect(first, "T",  "(", "SYMBOLE", "@");
        expect(first, "F",  "(", "SYMBOLE", "@");
        expect(first, "A",  "(", "SYMBOLE", "@");
        expect(first, "R'", "|", EPS);
        expect(first, "F'", "*", "+", "?", EPS);
    }

    private void expect(Map<String, Set<String>> first, String n, String... values) {
        Set<String> wanted = new LinkedHashSet<>(List.of(values));
        boolean ok = first.get(n).equals(wanted);
        System.out.println("  FIRST(" + n + ") : "
                + (ok ? "OK" : "DIVERGENCE, attendu " + show(wanted)
                              + " obtenu " + show(first.get(n))));
    }

    /** Affichage determine, avec epsilon ecrit "ε" a la place de la cle interne. */
    private String show(Set<String> s) {
        List<String> parts = new ArrayList<>();
        for (String t : ORDER) {
            if (s.contains(t)) parts.add(t.equals(EPS) ? "ε" : t);
        }
        return "{ " + String.join(", ", parts) + " }";
    }
}
```

Le programme stabilise FIRST et FOLLOW par point fixe, affiche les trois tableaux demandés, construit `M`, signale toute case multiple, et compare automatiquement les six valeurs de contrôle du sujet. Le nombre de passes affiché dépend de l’ordre de balayage des productions ; les ensembles finaux, eux, sont indépendants de cet ordre.

---

## Texte pour le rapport

### 1. FIRST, FOLLOW et FIRST des productions

`ε` = dérivation vide de la grammaire ; `@` = mot vide du langage ; `$` = fin d’entrée.

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

### 2. Table prédictive et LL(1)

| X \ a | \| | \* | + | ? | ( | ) | SYMBOLE | @ | $ |
|---|---|---|---|---|---|---|---|---|---|
| R | R → T R' | . | . | . | R → T R' | . | R → T R' | R → T R' | . |
| R' | R' → \| T R' | . | . | . | . | R' → ε | . | . | R' → ε |
| T | T → F T' | . | . | . | T → F T' | . | T → F T' | T → F T' | . |
| T' | T' → ε | . | . | . | T' → F T' | T' → ε | T' → F T' | T' → F T' | T' → ε |
| F | F → A F' | . | . | . | F → A F' | . | F → A F' | F → A F' | . |
| F' | F' → ε | F' → \* F' | F' → + F' | F' → ? F' | F' → ε | F' → ε | F' → ε | F' → ε | F' → ε |
| A | A → ( R ) | . | . | . | A → ( R ) | . | A → SYMBOLE | A → @ | . |

Vérification par ligne : `{ | } ∩ { ), $ } = ∅` pour `R'` ; `{ (, SYMBOLE, @ } ∩ { |, ), $ } = ∅` pour `T'` ; `{ * }`, `{ + }`, `{ ? }` et `{ (, SYMBOLE, @, |, ), $ }` sont disjoints pour `F'` ; `A` répartit `(`, `SYMBOLE`, `@` en trois cases distinctes. **Aucune case multiple : la grammaire est LL(1).** FOLLOW sert à placer les productions ε : on choisit `X → ε` quand le regard appartient à `FOLLOW(X)`, c’est-à-dire quand ce qui suit `X` est légal et qu’aucune autre production de `X` ne peut commencer par ce regard.

### 3. Grammaire initiale et transformation

Grammaire de départ : `R → R '|' T | T`, `T → T F | F`, `F → F '*' | F '+' | F '?' | A`, `A → '(' R ')' | SYMBOLE | '@'`.

La règle `X → X α | β ⇔ X → β X'` , `X' → α X' | ε` a été appliquée trois fois :

| Non-terminal | β | α | Grammaire obtenue |
|---|---|---|---|
| R | T | \| T | `R → T R'`, `R' → \| T R' \| ε` |
| T | F | F | `T → F T'`, `T' → F T' \| ε` |
| F | A | \*, +, ? | `F → A F'`, `F' → \* F' \| + F' \| ? F' \| ε` |

L’associativité à gauche est conservée par repli sémantique à gauche (`a|b|c` → `((a|b)|c)`). La concaténation étant implicite, aucun jeton ne signale un facteur suivant : `T'` doit donc continuer quand le regard est dans `FIRST(F) = { (, SYMBOLE, @ }` et s’arrêter quand il est dans `FOLLOW(T') = { |, ), $ }`. Leur disjonction rend la décision LL(1).

### 4. Erreurs

| Entrée | Jeton fautif | Où | Nature | Jeton(s) attendu(s) |
|---|---|---|---|---|
| `a)` | `)` pos 2 | Pile vide, après `R' → ε` | Test final `lookahead == $` (case `M[R',')']` **non vide**) | `$` |
| `a\|\|b` | `\|` pos 3 | Appel de `T` par `R' → \| T R'` | Case vide `M[T,'\|']` | { (, SYMBOLE, @ } |

La première erreur n’est donc **pas** une case vide de la table mais un échec du contrôle de fin d’entrée ; la seconde est une véritable case vide, détectée dès la consultation de `M[T, '|']`.
