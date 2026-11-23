import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Export Graphviz .dot d'un AFN, avec sortie triée et arêtes parallèles regroupées. */
public final class DotExporter {

    private DotExporter() {
        // classe utilitaire : aucune instance
    }

    /** Clé de regroupement : une arête est identifiée par (source, cible). */
    private record EdgeKey(int source, int target) {
    }

    /** Construit le texte .dot complet (terminé par un retour à la ligne). */
    public static String toDot(Nfa nfa) {
        Map<EdgeKey, List<String>> grouped = new TreeMap<>(
                Comparator.comparingInt(EdgeKey::source)
                          .thenComparingInt(EdgeKey::target));

        for (Transition t : nfa.transitions()) {
            grouped.computeIfAbsent(new EdgeKey(t.source(), t.target()),
                                    k -> new ArrayList<>()).add(t.label());
        }

        // ε d'abord, puis ordre lexicographique ; les doublons sont conservés.
        Comparator<String> labelOrder = (a, b) -> {
            if (a == b) return 0;
            if (a == null) return -1;
            if (b == null) return 1;
            return a.compareTo(b);
        };

        List<String> lines = new ArrayList<>();
        lines.add("digraph AFN {");
        lines.add("  rankdir=LR;");
        lines.add("  node [shape=circle];");
        lines.add("  __start [shape=point];");
        lines.add("  __start -> " + nfa.init() + ";");
        lines.add("  " + nfa.accept() + " [shape=doublecircle];");

        for (Map.Entry<EdgeKey, List<String>> entry : grouped.entrySet()) {
            List<String> labels = entry.getValue();
            labels.sort(labelOrder);
            StringBuilder joined = new StringBuilder();
            for (int i = 0; i < labels.size(); i++) {
                if (i > 0) joined.append(',');
                String label = labels.get(i);
                joined.append(label == null ? "\u03B5" : label);
            }
            EdgeKey key = entry.getKey();
            lines.add("  " + key.source() + " -> " + key.target()
                      + " [label=\"" + joined + "\"];");
        }

        lines.add("}");
        return String.join("\n", lines) + "\n";
    }

    /** Écrit le .dot en UTF-8 (pour le caractère ε). */
    public static void write(Nfa nfa, Path path) throws IOException {
        Files.writeString(path, toDot(nfa), StandardCharsets.UTF_8);
    }
}
