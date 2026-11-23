import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Interface de l'extension (b) : équivalence de deux expressions régulières
 * par comparaison de leurs AFD minimaux complets et canoniques.
 *   java -cp out EquivMain <expr1> <expr2> [--dot-prefix prefix]
 * Avec --dot-prefix, les deux AFD minimaux sont écrits dans prefix1.dot
 * et prefix2.dot.
 */
public final class EquivMain {

    private static final String USAGE =
            "Usage : java -cp out EquivMain <expr1> <expr2> [--dot-prefix prefix]";

    private EquivMain() {
        // classe utilitaire : aucune instance
    }

    public static void main(String[] args) {
        System.exit(run(args));
    }

    public static int run(String[] args) {
        String prefix = null;
        boolean prefixSeen = false;
        List<String> remaining = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            if ("--dot-prefix".equals(args[i])) {
                if (prefixSeen || i + 1 >= args.length) {
                    System.err.println(USAGE);
                    return 2;
                }
                prefix = args[++i];
                prefixSeen = true;
            } else {
                remaining.add(args[i]);
            }
        }
        if (remaining.size() != 2) {
            System.err.println(USAGE);
            return 2;
        }

        Dfa first;
        Dfa second;
        try {
            first = minimalDfa(remaining.get(0));
            second = minimalDfa(remaining.get(1));
        } catch (SyntaxErrorException e) {
            System.err.println(e.getMessage());
            return 1;
        }

        if (prefix != null) {
            try {
                first.write(Path.of(prefix + "1.dot"));
                second.write(Path.of(prefix + "2.dot"));
            } catch (IOException | RuntimeException e) {
                System.err.println("Erreur lors de l'\u00E9criture des .dot : "
                                   + e.getMessage());
                return 1;
            }
        }

        System.out.println("AFD minimaux : " + first.transitions().size()
                           + " et " + second.transitions().size() + " \u00E9tats");
        System.out.println(first.equals(second) ? "EQUIVALENTS" : "DIFFERENTS");
        return 0;
    }

    private static Dfa minimalDfa(String expression) throws SyntaxErrorException {
        return DfaMinimizer.minimize(
                SubsetConstruction.convert(
                        new Parser(new Lexer(expression)).parse()));
    }
}
