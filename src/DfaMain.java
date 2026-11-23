import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Interface de l'extension (a) : déterminisation par sous-ensembles.
 *   java -cp out DfaMain <expression> [--dot fichier.dot] [mot...]
 * L'option --dot peut apparaître n'importe où. Le mot vide s'écrit "".
 */
public final class DfaMain {

    private static final String USAGE =
            "Usage : java -cp out DfaMain <expression> [--dot fichier.dot] [mot...]";

    private DfaMain() {
        // classe utilitaire : aucune instance
    }

    public static void main(String[] args) {
        System.exit(run(args));
    }

    public static int run(String[] args) {
        String dotPath = null;
        boolean dotSeen = false;
        List<String> remaining = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            if ("--dot".equals(args[i])) {
                if (dotSeen || i + 1 >= args.length) {
                    System.err.println(USAGE);
                    return 2;
                }
                dotPath = args[++i];
                dotSeen = true;
            } else {
                remaining.add(args[i]);
            }
        }
        if (remaining.isEmpty()) {
            System.err.println(USAGE);
            return 2;
        }

        String expression = remaining.remove(0);
        Dfa dfa;
        try {
            dfa = SubsetConstruction.convert(
                    new Parser(new Lexer(expression)).parse());
        } catch (SyntaxErrorException e) {
            System.err.println(e.getMessage());
            return 1;
        }

        if (dotPath != null) {
            try {
                dfa.write(Path.of(dotPath));
            } catch (IOException | RuntimeException e) {
                System.err.println("Erreur lors de l'\u00E9criture du fichier .dot : "
                                   + e.getMessage());
                return 1;
            }
        }

        for (String raw : remaining) {
            boolean empty = raw.isEmpty() || "\"\"".equals(raw);
            String word = empty ? "" : raw;
            String display = empty ? "\"\"" : raw;
            System.out.println(display + " : " + (dfa.accepts(word) ? "OUI" : "NON"));
        }
        return 0;
    }
}
