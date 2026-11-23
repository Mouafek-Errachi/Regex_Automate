/**
 * Jeton immutable : nature, valeur source et position 1-based.
 * L'enregistrement (record) génère automatiquement les accesseurs type(),
 * value() et position(), ainsi que equals/hashCode/toString.
 */
public record Token(TokenType type, String value, int position) {

    /** Formats LE JETON TROUVÉ dans un message d'erreur. */
    public String displayFound() {
        return switch (type) {
            case EOF     -> "fin de l'expression";
            case SYMBOLE -> "'" + value + "'";
            default      -> type.display(); // '@', '|', '*', ... déjà entre guillemets
        };
    }
}
