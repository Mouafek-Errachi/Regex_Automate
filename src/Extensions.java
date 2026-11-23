import java.util.Arrays;

/**
 * Point d'entrée unique des extensions, sans modifier Main.java.
 *
 *   --dfa      déterminisation par sous-ensembles + export AFD
 *   --equiv    équivalence par AFD minimaux complets
 *   --recover  reprise sur erreur en mode panique (FOLLOW)
 */
public final class Extensions {

    private static final String USAGE =
            "Usage : java -cp out Extensions <--dfa|--equiv|--recover> [arguments...]\n"
            + "  --dfa      <expression> [--dot fichier.dot] [mot...]\n"
            + "  --equiv    <expr1> <expr2> [--dot-prefix prefix]\n"
            + "  --recover  <expression> [mot...]";

    private Extensions() {
        // classe utilitaire : aucune instance
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println(USAGE);
            System.exit(2);
            return;
        }

        String[] rest = Arrays.copyOfRange(args, 1, args.length);
        int code = switch (args[0]) {
            case "--dfa"     -> DfaMain.run(rest);
            case "--equiv"   -> EquivMain.run(rest);
            case "--recover" -> RecoveryMain.run(rest);
            default -> {
                System.err.println(USAGE);
                yield 2;
            }
        };
        System.exit(code);
    }
}
