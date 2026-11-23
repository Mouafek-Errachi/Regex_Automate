import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * CLI finale :
 *   java -cp out Main <expression> <mot>... [--dot fichier.dot]
 * L'option --dot peut apparaître n'importe où. Le mot vide s'écrit "".
 */
public class Main {

    public static void main(String[] args) {
        String usage = "Usage : java -cp out Main <expression> <mot>... [--dot fichier.dot]";

        String dotPath = null;
        boolean dotSeen = false;
        List<String> remaining = new ArrayList<>();

        // 1) Retrait de l'option --dot, où qu'elle se trouve.
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--dot".equals(arg)) {
                if (dotSeen || i + 1 >= args.length) {
                    System.err.println(usage);
                    System.exit(2);
                    return;
                }
                dotPath = args[++i];
                dotSeen = true;
            } else {
                remaining.add(arg);
            }
        }

        // 2) Le premier argument restant est l'expression, les suivants sont les mots.
        if (remaining.isEmpty()) {
            System.err.println(usage);
            System.exit(2);
            return;
        }
        String expression = remaining.remove(0);

        // 3) Analyse lexicale + syntaxique.
        Nfa nfa;
        try {
            nfa = new Parser(new Lexer(expression)).parse();
        } catch (SyntaxErrorException e) {
            System.err.println(e.getMessage());
            System.exit(1);
            return;
        }

        // 4) Export .dot facultatif.
        if (dotPath != null) {
            try {
                DotExporter.write(nfa, Path.of(dotPath));
            } catch (IOException | RuntimeException e) {
                System.err.println("Erreur lors de l'\u00E9criture du fichier .dot : " + e.getMessage());
                System.exit(1);
                return;
            }
        }

        // 5) Simulation, une ligne par mot.
        for (String raw : remaining) {
            boolean emptyWord = raw.isEmpty() || "\"\"".equals(raw);
            String word = emptyWord ? "" : raw;
            String display = emptyWord ? "\"\"" : raw;
            System.out.println(display + " : " + (Simulator.accepts(nfa, word) ? "OUI" : "NON"));
        }
    }
}
