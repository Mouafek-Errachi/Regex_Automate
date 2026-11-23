/** Erreur lexicale ou syntaxique : le message est directement affichable. */
public class SyntaxErrorException extends Exception {

    public SyntaxErrorException(String message) {
        super(message);
    }
}
