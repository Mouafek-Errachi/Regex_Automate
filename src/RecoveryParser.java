import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Analyse LL(1) avec reprise sur erreur en mode panique. Ce parseur est
 * indépendant de Parser : il conserve la grammaire, la numérotation par
 * attributs et la construction de Thompson, mais au lieu de s'arrêter à la
 * première erreur, il signale l'erreur puis se synchronise sur l'ensemble
 * FOLLOW du non-terminal courant.
 *
 * Un jeton invalide devient INVALID (voir RecoveryLexer) : il est signalé
 * puis sauté. Pour éviter toute boucle infinie, la synchronisation avance
 * toujours d'au moins un jeton quand le jeton courant n'appartient pas à
 * l'ensemble de synchronisation.
 */
public final class RecoveryParser {

    /** Résultat complet : automate construit et toutes les erreurs rencontrées. */
    public record RecoveryResult(Nfa nfa, List<String> errors) {
        public RecoveryResult {
            errors = List.copyOf(errors);
        }
    }

    // Alias locaux (import static impossible depuis le paquet par défaut).
    private static final RecoveryLexer.Kind EOF = RecoveryLexer.Kind.EOF;
    private static final RecoveryLexer.Kind EPSILON = RecoveryLexer.Kind.EPSILON;
    private static final RecoveryLexer.Kind INVALID = RecoveryLexer.Kind.INVALID;
    private static final RecoveryLexer.Kind LPAREN = RecoveryLexer.Kind.LPAREN;
    private static final RecoveryLexer.Kind PIPE = RecoveryLexer.Kind.PIPE;
    private static final RecoveryLexer.Kind PLUS = RecoveryLexer.Kind.PLUS;
    private static final RecoveryLexer.Kind QUESTION = RecoveryLexer.Kind.QUESTION;
    private static final RecoveryLexer.Kind RPAREN = RecoveryLexer.Kind.RPAREN;
    private static final RecoveryLexer.Kind STAR = RecoveryLexer.Kind.STAR;
    private static final RecoveryLexer.Kind SYMBOLE = RecoveryLexer.Kind.SYMBOLE;

    // Ensembles de synchronisation = FOLLOW des non-terminaux de la grammaire.
    private static final Set<RecoveryLexer.Kind> SYNC_R = Set.of(RPAREN, EOF);
    private static final Set<RecoveryLexer.Kind> SYNC_R_PRIME = Set.of(RPAREN, EOF);
    private static final Set<RecoveryLexer.Kind> SYNC_T = Set.of(PIPE, RPAREN, EOF);
    private static final Set<RecoveryLexer.Kind> SYNC_T_PRIME = Set.of(PIPE, RPAREN, EOF);
    private static final Set<RecoveryLexer.Kind> SYNC_F =
            Set.of(LPAREN, SYMBOLE, EPSILON, PIPE, RPAREN, EOF);
    private static final Set<RecoveryLexer.Kind> SYNC_F_PRIME = SYNC_F;
    private static final Set<RecoveryLexer.Kind> SYNC_A =
            Set.of(STAR, PLUS, QUESTION, LPAREN, SYMBOLE, EPSILON, RPAREN, EOF);
    private static final Set<RecoveryLexer.Kind> SYNC_EOF = Set.of(EOF);
    private static final Set<RecoveryLexer.Kind> SYNC_PAREN = Set.of(RPAREN, EOF);

    private final RecoveryLexer lexer;
    private final List<String> errors = new ArrayList<>();
    private RecoveryLexer.RecToken current;

    public RecoveryParser(String source) {
        this.lexer = new RecoveryLexer(source);
        this.current = lexer.nextToken();
    }

    /** Analyse complète ; les erreurs sont collectées, pas lancées. */
    public RecoveryResult parse() {
        Result result = parseR(0);
        if (current.kind() != EOF) {
            report(TokenType.EOF);
            synchronize(SYNC_EOF);
        }
        return new RecoveryResult(Nfa.of(result), errors);
    }

    /** R -> T R'  (FOLLOW(R) = { ')' , $ }). */
    private Result parseR(int n) {
        if (!isAtomStart(current.kind())) {
            reportProblem(TokenType.LPAREN, TokenType.SYMBOLE, TokenType.EPSILON);
            synchronize(SYNC_R);
            return Fragment.epsilon(n);
        }
        Result t = parseT(n);
        return parseRPrime(t.nextFree(), t.fragment());
    }

    /** R' -> '|' T R'1 | ε  (FOLLOW(R') = { ')' , $ }). */
    private Result parseRPrime(int n, Fragment h) {
        if (current.kind() == PIPE) {
            advance();
            Result t = parseT(n);
            Result u = Fragment.union(h, t.fragment(), t.nextFree());
            return parseRPrime(u.nextFree(), u.fragment());
        }
        if (isFollowRPrime(current.kind())) {
            return new Result(h, n);
        }
        reportProblem(TokenType.RPAREN, TokenType.EOF);
        synchronize(SYNC_R_PRIME);
        return new Result(h, n);
    }

    /** T -> F T'  (FOLLOW(T) = { '|' , ')' , $ }). */
    private Result parseT(int n) {
        if (!isAtomStart(current.kind())) {
            reportProblem(TokenType.LPAREN, TokenType.SYMBOLE, TokenType.EPSILON);
            synchronize(SYNC_T);
            return Fragment.epsilon(n);
        }
        Result f = parseF(n);
        return parseTPrime(f.nextFree(), f.fragment());
    }

    /** T' -> F T'1 | ε  (FOLLOW(T') = { '|' , ')' , $ }). */
    private Result parseTPrime(int n, Fragment h) {
        if (isAtomStart(current.kind())) {
            Result f = parseF(n);
            Result c = Fragment.concat(h, f.fragment(), f.nextFree());
            return parseTPrime(c.nextFree(), c.fragment());
        }
        if (isFollowTPrime(current.kind())) {
            return new Result(h, n);
        }
        reportProblem(TokenType.PIPE, TokenType.RPAREN, TokenType.EOF);
        synchronize(SYNC_T_PRIME);
        return new Result(h, n);
    }

    /** F -> A F'  (FOLLOW(F) contient FIRST(A) à cause des postfixes). */
    private Result parseF(int n) {
        Result a = parseA(n);
        return parseFPrime(a.nextFree(), a.fragment());
    }

    /** F' -> '*' F'1 | '+' F'1 | '?' F'1 | ε. */
    private Result parseFPrime(int n, Fragment h) {
        RecoveryLexer.Kind op = current.kind();
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
            return new Result(h, n);
        }
        reportProblem(TokenType.LPAREN, TokenType.SYMBOLE, TokenType.EPSILON,
                      TokenType.PIPE, TokenType.RPAREN, TokenType.EOF);
        synchronize(SYNC_F_PRIME);
        return new Result(h, n);
    }

    /** A -> '(' R ')' | SYMBOLE | '@'  (FOLLOW(A) = 9 terminaux). */
    private Result parseA(int n) {
        switch (current.kind()) {
            case LPAREN -> {
                advance();
                Result inner = parseR(n);
                expectClosingParen();
                return inner;
            }
            case SYMBOLE -> {
                String symbol = current.value();
                advance();
                return Fragment.symbole(symbol, n);
            }
            case EPSILON -> {
                advance();
                return Fragment.epsilon(n);
            }
            default -> {
                reportProblem(TokenType.LPAREN, TokenType.SYMBOLE, TokenType.EPSILON);
                synchronize(SYNC_A);
                return Fragment.epsilon(n);
            }
        }
    }

    /** ')' attendu : on le consomme s'il est là, sinon on saute jusqu'à lui ou EOF. */
    private void expectClosingParen() {
        if (current.kind() == RPAREN) {
            advance();
            return;
        }
        report(TokenType.RPAREN);
        synchronize(SYNC_PAREN);
        if (current.kind() == RPAREN) {
            advance();
        }
    }

    // ------------------------------------------------------------------
    // Diagnostic et reprise
    // ------------------------------------------------------------------
    private void reportProblem(TokenType... expected) {
        if (current.kind() == INVALID) {
            reportInvalid();
        } else {
            report(expected);
        }
    }

    private void report(TokenType... expected) {
        StringBuilder message = new StringBuilder();
        message.append("Erreur \u00E0 la position ").append(current.position()).append(" : ");
        for (int i = 0; i < expected.length; i++) {
            if (i > 0) {
                message.append(" ou ");
            }
            message.append(expected[i].display());
        }
        message.append(" attendu, ").append(current.displayFound()).append(" trouv\u00E9");
        errors.add(message.toString());
    }

    /** Message lexical strictement identique à celui du Lexer principal. */
    private void reportInvalid() {
        errors.add("Erreur \u00E0 la position " + current.position()
                   + " : caract\u00E8re '" + RecoveryLexer.printable(current.value().charAt(0))
                   + "' invalide");
    }

    /** Saute les jetons hors de l'ensemble FOLLOW fourni. */
    private void synchronize(Set<RecoveryLexer.Kind> follow) {
        while (current.kind() != EOF && !follow.contains(current.kind())) {
            advance();
        }
    }

    private void advance() {
        current = lexer.nextToken();
    }

    private boolean isAtomStart(RecoveryLexer.Kind kind) {
        return kind == LPAREN || kind == SYMBOLE || kind == EPSILON;
    }

    private boolean isPostfix(RecoveryLexer.Kind kind) {
        return kind == STAR || kind == PLUS || kind == QUESTION;
    }

    private boolean isFollowRPrime(RecoveryLexer.Kind kind) {
        return kind == RPAREN || kind == EOF;
    }

    private boolean isFollowTPrime(RecoveryLexer.Kind kind) {
        return kind == PIPE || kind == RPAREN || kind == EOF;
    }

    private boolean isFollowFPrime(RecoveryLexer.Kind kind) {
        return SYNC_F.contains(kind);
    }
}
