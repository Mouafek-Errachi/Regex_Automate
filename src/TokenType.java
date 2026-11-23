/** Nature des jetons reconnus par le lexeur. */
public enum TokenType {
    SYMBOLE,   // lettre minuscule/majuscule ou chiffre
    EPSILON,   // '@' : mot vide du langage
    PIPE,      // '|'
    STAR,      // '*'
    PLUS,      // '+'
    QUESTION,  // '?'
    LPAREN,    // '('
    RPAREN,    // ')'
    EOF;       // fin de l'entrée

    /**
     * Libellé utilisé dans la LISTE des jetons attendus.
     * Le jeton trouvé, lui, est formaté par Token#displayFound().
     */
    public String display() {
        return switch (this) {
            case SYMBOLE  -> "une lettre ou un chiffre";
            case EPSILON  -> "'@'";
            case PIPE     -> "'|'";
            case STAR     -> "'*'";
            case PLUS     -> "'+'";
            case QUESTION -> "'?'";
            case LPAREN   -> "'('";
            case RPAREN   -> "')'";
            case EOF      -> "fin de l'expression";
        };
    }
}
