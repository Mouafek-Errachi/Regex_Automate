import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Jeu de tests sans JUnit.
 *
 * Lit tests/tests.txt et tests/erreurs.txt, verifie chaque mot et chaque
 * position d'erreur, puis controle la propriete "au plus 2n etats" sur
 * toutes les expressions du fichier et sur 200 expressions aleatoires
 * (graine fixe, generateur ecrit a la main).
 *
 * Aucun champ static modifiable : le generateur a son propre etat et les
 * compteurs sont instances de Stats.
 */
public final class TestRunner {

    private static final String USAGE =
            "Usage : java -cp out TestRunner [tests.txt erreurs.txt]";

    private TestRunner() {
        // classe utilitaire : aucune instance
    }

    public static void main(String[] args) {
        if (args.length > 2) {
            System.err.println(USAGE);
            System.exit(2);
            return;
        }

        Path testsPath = args.length >= 1 ? Path.of(args[0]) : Path.of("tests", "tests.txt");
        Path errorsPath = args.length >= 2 ? Path.of(args[1]) : Path.of("tests", "erreurs.txt");

        Stats stats = new Stats();
        try {
            runWordTests(testsPath, stats);
            runErrorTests(errorsPath, stats);
            runRandomTests(stats);
        } catch (IOException e) {
            System.err.println("Lecture impossible : " + e.getMessage());
            System.exit(2);
            return;
        }

        stats.report();
        System.exit(stats.failures == 0 ? 0 : 1);
    }

    // -----------------------------------------------------------------
    // 1) Mots acceptes / rejetes
    // -----------------------------------------------------------------
    private static void runWordTests(Path path, Stats stats) throws IOException {
        for (String line : usefulLines(path)) {
            String[] parts = splitFields(line, 3);
            if (parts == null) {
                stats.fail("Ligne mal form\u00E9e dans tests.txt : " + line);
                continue;
            }

            String expression = parts[0];
            stats.expressions++;

            Nfa nfa;
            try {
                nfa = new Parser(new Lexer(expression)).parse();
            } catch (SyntaxErrorException e) {
                stats.fail("Expression invalide dans tests.txt : " + expression
                           + " -> " + e.getMessage());
                continue;
            }

            checkAutomaton(expression, expression, nfa, stats);

            for (String word : words(parts[1])) {
                stats.words++;
                if (!Simulator.accepts(nfa, word)) {
                    stats.fail("Mot accept\u00E9 attendu : " + expression
                               + " / " + show(word));
                }
            }
            for (String word : words(parts[2])) {
                stats.words++;
                if (Simulator.accepts(nfa, word)) {
                    stats.fail("Mot rejet\u00E9 attendu : " + expression
                               + " / " + show(word));
                }
            }
        }
    }

    // -----------------------------------------------------------------
    // 2) Positions d'erreur
    // -----------------------------------------------------------------
    private static void runErrorTests(Path path, Stats stats) throws IOException {
        for (String line : usefulLines(path)) {
            String[] parts = splitFields(line, 2);
            if (parts == null) {
                stats.fail("Ligne mal form\u00E9e dans erreurs.txt : " + line);
                continue;
            }

            String expression = parts[0];
            int expected;
            try {
                expected = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                stats.fail("Position non num\u00E9rique : " + line);
                continue;
            }

            stats.errors++;
            try {
                new Parser(new Lexer(expression)).parse();
                stats.fail("Erreur attendue mais absente : " + expression
                           + " (position " + expected + ")");
            } catch (SyntaxErrorException e) {
                int found = extractPosition(e.getMessage());
                if (found != expected) {
                    stats.fail("Position attendue " + expected + " mais " + found
                               + " obtenue pour " + expression + " : " + e.getMessage());
                }
            } catch (RuntimeException e) {
                stats.fail("Exception inattendue pour " + expression + " : " + e);
            }
        }
    }

    // -----------------------------------------------------------------
    // 3) 200 expressions aleatoires, graine fixe
    // -----------------------------------------------------------------
    private static void runRandomTests(Stats stats) {
        Generator generator = new Generator();
        for (int i = 0; i < 200; i++) {
            String expression = generator.expression();
            try {
                Nfa nfa = new Parser(new Lexer(expression)).parse();
                checkAutomaton("al\u00E9atoire #" + (i + 1) + " : " + expression,
                               expression, nfa, stats);
            } catch (SyntaxErrorException e) {
                stats.fail("Expression al\u00E9atoire invalide #" + (i + 1)
                           + " : " + expression + " -> " + e.getMessage());
            }
        }
    }

    // -----------------------------------------------------------------
    // Proprietes structurelles verifiees sur chaque automate
    // -----------------------------------------------------------------
    private static void checkAutomaton(String label, String expression, Nfa nfa, Stats stats) {
        stats.automata++;

        int n = countedTokens(expression);
        if (n < 0) {
            stats.fail("Comptage des jetons impossible : " + label);
            return;
        }
        if (nfa.stateCount() > 2 * n) {
            stats.fail("Trop d'\u00E9tats : " + label + " -> "
                       + nfa.stateCount() + " > 2*" + n);
        }

        if (!nfa.states().contains(nfa.init())) {
            stats.fail("Initial absent des \u00E9tats : " + label);
        }
        if (!nfa.states().contains(nfa.accept())) {
            stats.fail("Acceptant absent des \u00E9tats : " + label);
        }

        int incomingInit = 0;
        int outgoingAccept = 0;
        for (Transition t : nfa.transitions()) {
            if (t.target() == nfa.init()) {
                incomingInit++;
            }
            if (t.source() == nfa.accept()) {
                outgoingAccept++;
            }
            if (!nfa.states().contains(t.source()) || !nfa.states().contains(t.target())) {
                stats.fail("Transition hors ensemble d'\u00E9tats : " + label + " / " + t);
            }
        }
        if (incomingInit != 0) {
            stats.fail("L'initial a " + incomingInit + " transition(s) entrante(s) : " + label);
        }
        if (outgoingAccept != 0) {
            stats.fail("L'acceptant a " + outgoingAccept + " transition(s) sortante(s) : " + label);
        }

        for (int i = 0; i < nfa.stateCount(); i++) {
            if (!nfa.states().contains(i)) {
                stats.fail("Num\u00E9ro manquant " + i + " : " + label);
            }
        }
        for (int state : nfa.states()) {
            if (state < 0 || state >= nfa.stateCount()) {
                stats.fail("\u00C9tat hors de 0..N-1 : " + state + " / " + label);
            }
        }
    }

    /** n = SYMBOLE + @ + | + * + + + ? ; les parenth\u00E8ses ne comptent pas. */
    private static int countedTokens(String expression) {
        int count = 0;
        try {
            Lexer lexer = new Lexer(expression);
            Token token = lexer.nextToken();
            while (token.type() != TokenType.EOF) {
                switch (token.type()) {
                    case SYMBOLE, EPSILON, PIPE, STAR, PLUS, QUESTION -> count++;
                    default -> { /* '(' et ')' ne comptent pas */ }
                }
                token = lexer.nextToken();
            }
        } catch (SyntaxErrorException e) {
            return -1;
        }
        return count;
    }

    // -----------------------------------------------------------------
    // Utilitaires
    // -----------------------------------------------------------------
    private static List<String> usefulLines(Path path) throws IOException {
        List<String> result = new ArrayList<>();
        for (String raw : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            String line = raw.trim();
            if (!line.isEmpty() && !line.startsWith("#")) {
                result.add(line);
            }
        }
        return result;
    }

    /** D\u00E9coupe exactement `count` champs s\u00E9par\u00E9s par ';', ou null. */
    private static String[] splitFields(String line, int count) {
        String[] result = new String[count];
        int from = 0;
        for (int i = 0; i < count - 1; i++) {
            int semi = line.indexOf(';', from);
            if (semi < 0) {
                return null;
            }
            result[i] = line.substring(from, semi).trim();
            from = semi + 1;
        }
        result[count - 1] = line.substring(from).trim();
        if (line.indexOf(';', from) >= 0) {
            return null; // champ final inattendu
        }
        return result;
    }

    private static List<String> words(String field) {
        List<String> result = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(field);
        while (tokenizer.hasMoreTokens()) {
            String word = tokenizer.nextToken();
            result.add("@".equals(word) ? "" : word);
        }
        return result;
    }

    private static String show(String word) {
        return word.isEmpty() ? "@" : word;
    }

    private static int extractPosition(String message) {
        int index = message.indexOf("position");
        if (index < 0) {
            return -1;
        }
        int i = index + "position".length();
        while (i < message.length() && !Character.isDigit(message.charAt(i))) {
            i++;
        }
        int start = i;
        while (i < message.length() && Character.isDigit(message.charAt(i))) {
            i++;
        }
        return start == i ? -1 : Integer.parseInt(message.substring(start, i));
    }

    /** LCG 48 bits, graine fixe, sans java.util.Random. */
    private static final class Generator {
        private long state = 0x5DEECE66DL;

        String expression() {
            return regex(2);
        }

        private int nextInt(int bound) {
            state = (state * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
            return (int) ((state >>> 16) % bound);
        }

        private String regex(int depth) {
            StringBuilder sb = new StringBuilder(term(depth));
            int extra = nextInt(3);
            for (int i = 0; i < extra; i++) {
                sb.append('|').append(term(depth));
            }
            return sb.toString();
        }

        private String term(int depth) {
            StringBuilder sb = new StringBuilder(factor(depth));
            int extra = nextInt(3);
            for (int i = 0; i < extra; i++) {
                sb.append(factor(depth));
            }
            return sb.toString();
        }

        private String factor(int depth) {
            StringBuilder sb = new StringBuilder(atom(depth));
            int ops = nextInt(3);
            for (int i = 0; i < ops; i++) {
                sb.append(postfix());
            }
            return sb.toString();
        }

        private String atom(int depth) {
            if (depth > 0 && nextInt(4) == 0) {
                return "(" + regex(depth - 1) + ")";
            }
            return nextInt(5) == 0 ? "@" : String.valueOf(symbol());
        }

        private char symbol() {
            return switch (nextInt(3)) {
                case 0 -> 'a';
                case 1 -> 'b';
                default -> 'c';
            };
        }

        private char postfix() {
            return switch (nextInt(3)) {
                case 0 -> '*';
                case 1 -> '+';
                default -> '?';
            };
        }
    }

    /** Compteurs d'ex\u00E9cution : simples champs d'instance, jamais static. */
    private static final class Stats {
        private int expressions;
        private int words;
        private int errors;
        private int automata;
        private int failures;

        private void fail(String message) {
            failures++;
            System.out.println("FAIL : " + message);
        }

        private void report() {
            System.out.println("R\u00E9sum\u00E9 : " + expressions + " expressions, "
                               + words + " mots, " + errors + " erreurs, "
                               + automata + " automates v\u00E9rifi\u00E9s");
            if (failures == 0) {
                System.out.println("PASS : tous les tests sont corrects.");
            } else {
                System.out.println("FAIL : " + failures + " \u00E9chec(s).");
            }
        }
    }
}
