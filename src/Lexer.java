/**
 * Analyseur lexical sans regex : un caractère = un jeton.
 * Les positions sont comptées à partir de 1, y compris pour EOF
 * (position = longueur(source) + 1).
 */
public class Lexer {
    private final String source;
    private int index; // index 0-based dans source

    public Lexer(String source) {
        this.source = source;
        this.index = 0;
    }

    /** Renvoie le jeton suivant, ou EOF une fois la source épuisée. */
    public Token nextToken() throws SyntaxErrorException {
        if (index >= source.length()) {
            return new Token(TokenType.EOF, "", source.length() + 1);
        }

        char c = source.charAt(index);
        int position = index + 1;

        // Lettres ASCII et chiffres ASCII uniquement (pas de Character.isLetter,
        // qui accepterait aussi les lettres accentuées).
        if (isLetter(c) || isDigit(c)) {
            index++;
            return new Token(TokenType.SYMBOLE, String.valueOf(c), position);
        }

        TokenType type = switch (c) {
            case '@' -> TokenType.EPSILON;
            case '|' -> TokenType.PIPE;
            case '*' -> TokenType.STAR;
            case '+' -> TokenType.PLUS;
            case '?' -> TokenType.QUESTION;
            case '(' -> TokenType.LPAREN;
            case ')' -> TokenType.RPAREN;
            default  -> null; // caractère hors alphabet
        };

        if (type == null) {
            throw new SyntaxErrorException(
                    "Erreur \u00E0 la position " + position + " : caract\u00E8re '"
                    + printable(c) + "' invalide");
        }

        index++;
        return new Token(type, String.valueOf(c), position);
    }

    private static boolean isLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    /** Rend les caractères invisibles lisibles dans un message. */
    private static String printable(char c) {
        return switch (c) {
            case '\n' -> "\\n";
            case '\t' -> "\\t";
            default   -> String.valueOf(c); // y compris ' ' => "caractère ' ' invalide"
        };
    }
}
