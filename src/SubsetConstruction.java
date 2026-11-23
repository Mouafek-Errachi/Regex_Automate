import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Déterminisation par sous-ensembles (construction de Rabin-Scott) d'un AFN
 * de Thompson. L'AFD produit est COMPLET : les transitions manquantes vont
 * vers un état puits explicite. Les états sont numérotés par ordre de
 * découverte en largeur, donc de façon reproductible.
 *
 * Les ensembles de NFA sont représentés par des List<Integer> triées : cela
 * fournit une clé de map stable et une représentation canonique du sous-ensemble.
 */
public final class SubsetConstruction {

    private SubsetConstruction() {
        // classe utilitaire : aucune instance
    }

    public static Dfa convert(Nfa nfa) {
        return new Builder(nfa).build();
    }

    /** État interne de la construction : aucun champ static, aucune variable globale. */
    private static final class Builder {
        private final Nfa nfa;
        private final List<String> alphabet;
        private final List<List<Integer>> subsets = new ArrayList<>();
        private final Map<List<Integer>, Integer> ids = new HashMap<>();
        private final Deque<Integer> queue = new ArrayDeque<>();
        private final Map<Integer, Map<String, Integer>> transitions = new TreeMap<>();

        private Builder(Nfa nfa) {
            this.nfa = nfa;
            TreeSet<String> sorted = new TreeSet<>();
            for (Transition t : nfa.transitions()) {
                if (!t.isEpsilon()) {
                    sorted.add(t.label());
                }
            }
            this.alphabet = List.copyOf(sorted);
        }

        private Dfa build() {
            int start = intern(closure(Set.of(nfa.init())));

            while (!queue.isEmpty()) {
                int state = queue.removeFirst();
                List<Integer> subset = subsets.get(state);
                Map<String, Integer> row = new TreeMap<>();
                for (String symbol : alphabet) {
                    row.put(symbol, intern(closure(move(subset, symbol))));
                }
                transitions.put(state, row);
            }

            Set<Integer> accepting = new TreeSet<>();
            for (int state = 0; state < subsets.size(); state++) {
                if (subsets.get(state).contains(nfa.accept())) {
                    accepting.add(state);
                }
            }
            Integer sink = ids.get(List.of()); // List.of() = sous-ensemble vide
            return new Dfa(start, accepting, transitions, alphabet,
                           sink == null ? -1 : sink);
        }

        /** Insère un sous-ensemble s'il est nouveau et renvoie son numéro. */
        private int intern(Set<Integer> raw) {
            List<Integer> key = List.copyOf(new TreeSet<>(raw));
            Integer known = ids.get(key);
            if (known != null) {
                return known;
            }
            int id = subsets.size();
            ids.put(key, id);
            subsets.add(key);
            queue.addLast(id);
            return id;
        }

        /** ε-fermeture itérative (pile + visités), identique à Simulator. */
        private Set<Integer> closure(Set<Integer> start) {
            Set<Integer> visited = new HashSet<>(start);
            Deque<Integer> stack = new ArrayDeque<>(start);
            while (!stack.isEmpty()) {
                int state = stack.pop();
                for (Transition t : nfa.transitions()) {
                    if (t.source() == state && t.isEpsilon() && !visited.contains(t.target())) {
                        visited.add(t.target());
                        stack.push(t.target());
                    }
                }
            }
            return visited;
        }

        /** États atteignables par une transition portant exactement symbol. */
        private Set<Integer> move(List<Integer> subset, String symbol) {
            Set<Integer> reached = new HashSet<>();
            for (int state : subset) {
                for (Transition t : nfa.transitions()) {
                    if (t.source() == state && symbol.equals(t.label())) {
                        reached.add(t.target());
                    }
                }
            }
            return reached;
        }
    }
}
