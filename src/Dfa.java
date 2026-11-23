import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Automate fini déterministe complet : toute transition est définie pour
 * chaque symbole de l'alphabet. Les collections sont immuables et triées
 * à l'export, afin que deux AFD isomorphes produisent le même .dot.
 *
 * sink = numéro de l'état puits (transition boucle sur tous les symboles),
 * ou -1 si l'alphabet est vide. Aucun champ static modifiable.
 */
public record Dfa(int start,
                  Set<Integer> accepting,
                  Map<Integer, Map<String, Integer>> transitions,
                  List<String> alphabet,
                  int sink) {

    public Dfa {
        accepting = Set.copyOf(accepting);
        alphabet = List.copyOf(alphabet);
        Map<Integer, Map<String, Integer>> copy = new TreeMap<>();
        for (Map.Entry<Integer, Map<String, Integer>> entry : transitions.entrySet()) {
            copy.put(entry.getKey(), new TreeMap<>(entry.getValue()));
        }
        transitions = Map.copyOf(copy);
    }

    /** Transition obligatoirement présente pour un symbole de l'alphabet. */
    public int next(int state, String symbol) {
        Map<String, Integer> row = transitions.get(state);
        Integer target = row == null ? null : row.get(symbol);
        if (target == null) {
            throw new IllegalStateException("Transition absente : " + state + " / " + symbol);
        }
        return target;
    }

    /**
     * Transition tolérant un symbole hors alphabet : on va dans le puits si
     * l'AFD en possède un, sinon on renvoie -1 (mot rejeté).
     */
    public int step(int state, String symbol) {
        Map<String, Integer> row = transitions.get(state);
        Integer target = row == null ? null : row.get(symbol);
        if (target != null) {
            return target;
        }
        return sink >= 0 ? sink : -1;
    }

    /** Simulation sur un mot, sans epsilon (l'AFD est déterministe et complet). */
    public boolean accepts(String word) {
        int state = start;
        for (int i = 0; i < word.length(); i++) {
            state = step(state, String.valueOf(word.charAt(i)));
            if (state < 0) {
                return false;
            }
        }
        return accepting.contains(state);
    }

    /**
     * Renumérotation canonique par parcours en largeur depuis l'état initial.
     * Deux AFD isomorphes obtiennent ainsi exactement les mêmes numéros.
     */
    public Dfa canonical() {
        Map<Integer, Integer> ids = new LinkedHashMap<>();
        Deque<Integer> queue = new ArrayDeque<>();
        ids.put(start, 0);
        queue.add(start);

        while (!queue.isEmpty()) {
            int state = queue.removeFirst();
            for (String symbol : alphabet) {
                int target = next(state, symbol);
                if (!ids.containsKey(target)) {
                    ids.put(target, ids.size());
                    queue.addLast(target);
                }
            }
        }

        Map<Integer, Map<String, Integer>> renamed = new TreeMap<>();
        for (Map.Entry<Integer, Integer> entry : ids.entrySet()) {
            Map<String, Integer> row = new TreeMap<>();
            for (String symbol : alphabet) {
                row.put(symbol, ids.get(next(entry.getKey(), symbol)));
            }
            renamed.put(entry.getValue(), row);
        }

        Set<Integer> renamedAccepting = new TreeSet<>();
        for (int state : accepting) {
            renamedAccepting.add(ids.get(state));
        }
        int renamedSink = sink < 0 ? -1 : ids.getOrDefault(sink, -1);
        return new Dfa(0, renamedAccepting, renamed, alphabet, renamedSink);
    }

    /** Lignes de transitions triées, utilisées par toDot(). */
    private Map<Integer, Map<String, Integer>> sortedRows() {
        Map<Integer, Map<String, Integer>> rows = new TreeMap<>();
        for (Map.Entry<Integer, Map<String, Integer>> entry : transitions.entrySet()) {
            rows.put(entry.getKey(), new TreeMap<>(entry.getValue()));
        }
        return rows;
    }

    /** Export Graphviz : même esprit que DotExporter, avec préfixe q. */
    public String toDot() {
        Map<Integer, Map<String, Integer>> rows = sortedRows();
        // Regroupement par (source, cible), comme dans DotExporter pour l'AFN.
        Map<String, List<String>> edges = new TreeMap<>();
        for (Map.Entry<Integer, Map<String, Integer>> row : rows.entrySet()) {
            for (Map.Entry<String, Integer> edge : row.getValue().entrySet()) {
                String key = row.getKey() + "->" + edge.getValue();
                edges.computeIfAbsent(key, k -> new ArrayList<>()).add(edge.getKey());
            }
        }

        List<String> lines = new ArrayList<>();
        lines.add("digraph AFD {");
        lines.add("  rankdir=LR;");
        lines.add("  node [shape=circle];");
        lines.add("  __start [shape=point];");
        lines.add("  __start -> q" + start + ";");
        for (int state : new TreeSet<>(accepting)) {
            lines.add("  q" + state + " [shape=doublecircle];");
        }
        if (sink >= 0) {
            lines.add("  q" + sink + " [label=\"puits\", shape=circle];");
        }
        for (Map.Entry<String, List<String>> entry : edges.entrySet()) {
            String[] parts = entry.getKey().split("->", -1);
            List<String> labels = new ArrayList<>(entry.getValue());
            labels.sort(String::compareTo);
            lines.add("  q" + parts[0] + " -> q" + parts[1]
                      + " [label=\"" + String.join(",", labels) + "\"];");
        }
        lines.add("}");
        return String.join("\n", lines) + "\n";
    }

    /** Écrit le .dot en UTF-8. */
    public void write(Path path) throws IOException {
        Files.writeString(path, toDot(), StandardCharsets.UTF_8);
    }
}
