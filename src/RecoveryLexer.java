/**
 * Lexeur utilisé uniquement par le mode de reprise sur erreur.
 * Il ne lève jamais d'exception : un caractère hors alphabet devient un jeton
 * INVALID, ce qui permet au parseur de le signaler puis de le sauter.
 * Les positions restent 1-based, EOF étant à longueur(source) + 1.
 */
public final class RecoveryLexer {

    public enum Kind {
        SYMBOLE, EPSILON, PIPE, STAR, PLUS, QUESTION,
        LPAREN, RPAREN, EOF, INVALID
    }

    public record RecToken(Kind kind, String value, int position) {

        /** Formats LE jeton trouvé, avec la même phrasing que Token#displayFound(). */
        public String displayFound() {
            return switch (kind) {
                case EOF     -> "fin de l'expression";
                case SYMBOLE -> "'" + value + "'";
                case INVALID -> "'" + printable(value.charAt(0)) + "'";
                default      -> tokenType().display();
            };
        }

        /** Traduction vers l'énumération du lexeur principal (hors INVALID). */
        public TokenType tokenType() {
            return switch (kind) {
                case SYMBOLE  -> TokenType.SYMBOLE;
                case EPSILON  -> TokenType.EPSILON;
                case PIPE     -> TokenType.PIPE;
                case STAR     -> TokenType.STAR;
                case PLUS     -> TokenType.PLUS;
                case QUESTION -> TokenType.QUESTION;
                case LPAREN   -> TokenType.LPAREN;
                case RPAREN   -> TokenType.RPAREN;
                case EOF      -> TokenType.EOF;
                case INVALID  -> null;
            };
        }
    }

    private final String source;
    private int index; // index 0-based dans source

    public RecoveryLexer(String source) {
        this.source = source;
        this.index = 0;
    }

    public RecToken nextToken() {
        if (index >= source.length()) {
            return new RecToken(Kind.EOF, "", source.length() + 1);
        }

        char c = source.charAt(index);
        int position = index + 1;

        if (isLetter(c) || isDigit(c)) {
            index++;
            return new RecToken(Kind.SYMBOLE, String.valueOf(c), position);
        }

        Kind kind = switch (c) {
            case '@' -> Kind.EPSILON;
            case '|' -> Kind.PIPE;
            case '*' -> Kind.STAR;
            case '+' -> Kind.PLUS;
            case '?' -> Kind.QUESTION;
            case '(' -> Kind.LPAREN;
            case ')' -> Kind.RPAREN;
            default  -> Kind.INVALID;
        };

        index++; // même invalide, on avance : c'est la base de la reprise
        return new RecToken(kind, String.valueOf(c), position);
    }

    private static boolean isLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    static String printable(char c) {
        return switch (c) {
            case '\n' -> "\\n";
            case '\t' -> "\\t";
            default   -> String.valueOf(c);
        };
    }
}
