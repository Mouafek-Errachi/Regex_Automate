import java.util.List;

/**
 * Interface de l'extension (c) : mode diagnostic avec reprise sur erreur.
 *   java -cp out RecoveryMain <expression> [mot...]
 * Toutes les erreurs sont affichées sur stderr ; le code de retour vaut 1
 * dès qu'au moins une erreur a été trouvée, 2 pour un usage incorrect.
 */
public final class RecoveryMain {

    private static final String USAGE =
            "Usage : java -cp out RecoveryMain <expression> [mot...]";

    private RecoveryMain() {
        // classe utilitaire : aucune instance
    }

    public static void main(String[] args) {
        System.exit(run(args));
    }

    public static int run(String[] args) {
        if (args.length == 0) {
            System.err.println(USAGE);
            return 2;
        }

        RecoveryParser.RecoveryResult result = new RecoveryParser(args[0]).parse();
        List<String> errors = result.errors();
        for (String error : errors) {
            System.err.println(error);
        }

        for (int i = 1; i < args.length; i++) {
            String raw = args[i];
            boolean empty = raw.isEmpty() || "\"\"".equals(raw);
            String word = empty ? "" : raw;
            String display = empty ? "\"\"" : raw;
            System.out.println(display + " : "
                               + (Simulator.accepts(result.nfa(), word) ? "OUI" : "NON"));
        }
        return errors.isEmpty() ? 0 : 1;
    }
}
