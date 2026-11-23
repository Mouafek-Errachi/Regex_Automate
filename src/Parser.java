/**
 * Analyse syntaxique LL(1) par descente récursive + schéma L-attribué.
 *
 * Chaque procédure reçoit n (prochain numéro d'état libre, attribut hérité)
 * et éventuellement h (fragment accumulé), et retourne un Result :
 *   Result(fragment = attribut synthétisé s, nextFree = attribut synthétisé n').
 * Le compteur n'est donc ni un champ de Parser, ni un champ de Fragment,
 * ni un champ de Nfa : c'est un entier passé par valeur le long de l'arbre.
 *
 * Une méthode par non-terminal, un seul jeton de prévision (current),
 * aucun retour arrière, aucune regex Java.
 */
public class Parser {
    private final Lexer lexer;
    private Token current; // l'unique jeton de prévision

    public Parser(Lexer lexer) throws SyntaxErrorException {
        this.lexer = lexer;
        this.current = lexer.nextToken();
    }

    /** Départ obligatoire à n = 0 ; vérifie ensuite l'épuisement de l'entrée. */
    public Nfa parse() throws SyntaxErrorException {
        Result result = parseR(0);
        if (current.type() != TokenType.EOF) {
            throw error(TokenType.EOF);
        }
        return Nfa.of(result); // contrôle : stateCount() == nextFree
    }

    /**
     * R -> T R'
     * FIRST(R) = { '(' , SYMBOLE , '@' }.
     * n(T) = n(R) ; h(R') = s(T), n(R') = n'(T) ; s(R) = s(R'), n'(R) = n'(R').
     */
    private Result parseR(int n) throws SyntaxErrorException {
        if (!isAtomStart(current.type())) {
            throw error(TokenType.LPAREN, TokenType.SYMBOLE, TokenType.EPSILON);
        }
        Result t = parseT(n);                              // s(T), n'(T)
        return parseRPrime(t.nextFree(), t.fragment());    // h(R'), n(R')
    }

    /**
     * R' -> '|' T R'1 | ε
     * FIRST(R') = { '|', ε } ; FOLLOW(R') = { ')', $ }.
     * n(T) = n(R') ; h(R'1) = union(h(R'), s(T), n'(T)) ; n(R'1) = n'(T) + 2 ;
     * s(R') = s(R'1), n'(R') = n'(R'1).
     */
    private Result parseRPrime(int n, Fragment h) throws SyntaxErrorException {
        if (current.type() == TokenType.PIPE) {
            advance();                                      // '|'
            Result t = parseT(n);                           // s(T), n'(T)
            Result u = Fragment.union(h, t.fragment(), t.nextFree());
            return parseRPrime(u.nextFree(), u.fragment()); // n(R'1) = n'(T) + 2
        }
        if (isFollowRPrime(current.type())) {
            return new Result(h, n);                        // ε : s = h, n' = n
        }
        throw error(TokenType.RPAREN, TokenType.EOF);
    }

    /**
     * T -> F T'
     * FIRST(T) = { '(' , SYMBOLE , '@' }.
     * n(F) = n(T) ; h(T') = s(F), n(T') = n'(F) ; s(T) = s(T'), n'(T) = n'(T').
     */
    private Result parseT(int n) throws SyntaxErrorException {
        if (!isAtomStart(current.type())) {
            throw error(TokenType.LPAREN, TokenType.SYMBOLE, TokenType.EPSILON);
        }
        Result f = parseF(n);
        return parseTPrime(f.nextFree(), f.fragment());
    }

    /**
     * T' -> F T'1 | ε
     * FIRST(T') = { '(' , SYMBOLE , '@', ε } ; FOLLOW(T') = { '|', ')', $ }.
     * n(F) = n(T') ; h(T'1) = concat(h(T'), s(F), n'(F)) ; n(T'1) = n'(F) ;
     * s(T') = s(T'1), n'(T') = n'(T'1). concat n' = n (aucun état créé).
     */
    private Result parseTPrime(int n, Fragment h) throws SyntaxErrorException {
        if (isAtomStart(current.type())) {                  // FIRST(F), concat implicite
            Result f = parseF(n);
            Result c = Fragment.concat(h, f.fragment(), f.nextFree());
            return parseTPrime(c.nextFree(), c.fragment());
        }
        if (isFollowTPrime(current.type())) {
            return new Result(h, n);                        // ε
        }
        throw error(TokenType.PIPE, TokenType.RPAREN, TokenType.EOF);
    }

    /**
     * F -> A F'
     * FIRST(F) = FIRST(A) = { '(' , SYMBOLE , '@' }.
     * n(A) = n(F) ; h(F') = s(A), n(F') = n'(A) ; s(F) = s(F'), n'(F) = n'(F').
     */
    private Result parseF(int n) throws SyntaxErrorException {
        Result a = parseA(n);
        return parseFPrime(a.nextFree(), a.fragment());
    }

    /**
     * F' -> '*' F'1 | '+' F'1 | '?' F'1 | ε
     * FIRST(F') = { '*', '+', '?', ε } ;
     * FOLLOW(F') = { '(', SYMBOLE, '@', '|', ')', $ }.
     * Pour '*' : h(F'1) = star(h(F'), n(F')) ; n(F'1) = n(F') + 2 ;
     * s(F') = s(F'1), n'(F') = n'(F'1). Idem '+' (plus) et '?' (optional).
     */
    private Result parseFPrime(int n, Fragment h) throws SyntaxErrorException {
        TokenType op = current.type();
        if (isPostfix(op)) {
            advance();
            Result built = switch (op) {
                case STAR     -> Fragment.star(h, n);
                case PLUS     -> Fragment.plus(h, n);
                case QUESTION -> Fragment.optional(h, n);
                default       -> throw new IllegalStateException("postfixe inattendu");
            };
            return parseFPrime(built.nextFree(), built.fragment());
        }
        if (isFollowFPrime(op)) {
            return new Result(h, n);                        // ε
        }
        throw error(TokenType.LPAREN, TokenType.SYMBOLE, TokenType.EPSILON,
                    TokenType.PIPE, TokenType.RPAREN, TokenType.EOF);
    }

    /**
     * A -> '(' R ')' | SYMBOLE | '@'
     * FIRST(A) = { '(' , SYMBOLE , '@' } (FIRST disjoints, aucun ε).
     * '(' : n(R) = n(A), s(A) = s(R), n'(A) = n'(R).
     * SYMBOLE : (s(A), n'(A)) = symbole(val, n(A))  -> n' = n + 2.
     * '@'     : (s(A), n'(A)) = epsilon(n(A))       -> n' = n + 2.
     */
    private Result parseA(int n) throws SyntaxErrorException {
        switch (current.type()) {
            case LPAREN -> {
                advance();                          // '('
                Result inner = parseR(n);
                expect(TokenType.RPAREN);           // ')' ∈ FOLLOW(R)
                return inner;                       // s(A) = s(R), n'(A) = n'(R)
            }
            case SYMBOLE -> {
                Token symbol = current;
                advance();
                return Fragment.symbole(symbol.value(), n);
            }
            case EPSILON -> {
                advance();
                return Fragment.epsilon(n);
            }
            default -> throw error(TokenType.LPAREN, TokenType.SYMBOLE, TokenType.EPSILON);
        }
    }

    // ------------------------------------------------------------------
    // Utilitaires (aucun état global : tout dérive de current et de n).
    // ------------------------------------------------------------------
    private void advance() throws SyntaxErrorException {
        current = lexer.nextToken();
    }

    private void expect(TokenType expected) throws SyntaxErrorException {
        if (current.type() != expected) {
            throw error(expected);
        }
        advance();
    }

    /** "Erreur à la position p : A ou B attendu, C trouvé". */
    private SyntaxErrorException error(TokenType... expected) {
        StringBuilder message = new StringBuilder();
        message.append("Erreur \u00E0 la position ").append(current.position()).append(" : ");
        for (int i = 0; i < expected.length; i++) {
            if (i > 0) {
                message.append(" ou ");
            }
            message.append(expected[i].display());
        }
        message.append(" attendu, ").append(current.displayFound()).append(" trouv\u00E9");
        return new SyntaxErrorException(message.toString());
    }

    private boolean isAtomStart(TokenType t) {
        return t == TokenType.LPAREN || t == TokenType.SYMBOLE || t == TokenType.EPSILON;
    }

    private boolean isPostfix(TokenType t) {
        return t == TokenType.STAR || t == TokenType.PLUS || t == TokenType.QUESTION;
    }

    private boolean isFollowRPrime(TokenType t) {
        return t == TokenType.RPAREN || t == TokenType.EOF;
    }

    private boolean isFollowTPrime(TokenType t) {
        return t == TokenType.PIPE || t == TokenType.RPAREN || t == TokenType.EOF;
    }

    private boolean isFollowFPrime(TokenType t) {
        return switch (t) {
            case LPAREN, SYMBOLE, EPSILON, PIPE, RPAREN, EOF -> true;
            default -> false;
        };
    }
}
