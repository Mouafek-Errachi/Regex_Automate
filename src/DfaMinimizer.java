import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Minimisation d'un AFD complet par l'algorithme de Moore : on part de la
 * partition {acceptants, non-acceptants}, puis on raffine en comparant le
 * couple (classe courante, classes atteintes par chaque symbole) jusqu'à
 * stabilisation. Le quotient est ensuite renuméroté canoniquement.
 */
public final class DfaMinimizer {

    private DfaMinimizer() {
        // classe utilitaire : aucune instance
    }

    public static Dfa minimize(Dfa dfa) {
        Dfa input = dfa.canonical();
        List<String> alphabet = input.alphabet();
        List<Integer> states = new ArrayList<>(input.transitions().keySet());
        states.sort(Integer::compareTo);

        // Partition initiale : 0 = non-acceptant, 1 = acceptant (si les deux existent).
        Map<Integer, Integer> current = new HashMap<>();
        int nextId = 0;
        for (int state : states) {
            if (!input.accepting().contains(state)) {
                current.put(state, nextId++);
            }
        }
        for (int state : states) {
            if (input.accepting().contains(state)) {
                current.put(state, nextId++);
            }
        }
        if (states.isEmpty()) {
            return input;
        }

        while (true) {
            Map<Signature, Integer> signatures = new TreeMap<>();
            Map<Integer, Integer> refined = new HashMap<>();
            for (int state : states) {
                List<Integer> targets = new ArrayList<>();
                for (String symbol : alphabet) {
                    targets.add(current.get(input.next(state, symbol)));
                }
                Signature key = new Signature(current.get(state), targets);
                Integer id = signatures.get(key);
                if (id == null) {
                    id = signatures.size();
                    signatures.put(key, id);
                }
                refined.put(state, id);
            }
            if (refined.equals(current)) {
                break;
            }
            current = refined;
        }

        // Classes de l'état initial à la fin du raffinement.
        int classes = 0;
        for (int id : current.values()) {
            classes = Math.max(classes, id + 1);
        }

        Map<Integer, List<Integer>> members = new TreeMap<>();
        for (int state : states) {
            members.computeIfAbsent(current.get(state), k -> new ArrayList<>()).add(state);
        }

        Set<Integer> acceptingClasses = new TreeSet<>();
        for (int state : input.accepting()) {
            acceptingClasses.add(current.get(state));
        }

        Map<Integer, Map<String, Integer>> transitions = new TreeMap<>();
        for (Map.Entry<Integer, List<Integer>> entry : members.entrySet()) {
            int representative = entry.getValue().get(0); // plus petit état = choix déterministe
            Map<String, Integer> row = new TreeMap<>();
            for (String symbol : alphabet) {
                row.put(symbol, current.get(input.next(representative, symbol)));
            }
            transitions.put(entry.getKey(), row);
        }

        int sinkClass = input.sink() < 0 ? -1 : current.get(input.sink());
        return new Dfa(current.get(input.start()), acceptingClasses,
                       transitions, alphabet, sinkClass).canonical();
    }

    /** Signature comparable : classe courante puis classes des cibles. */
    private record Signature(int klass, List<Integer> targets)
            implements Comparable<Signature> {

        @Override
        public int compareTo(Signature other) {
            int first = Integer.compare(klass, other.klass);
            if (first != 0) {
                return first;
            }
            int size = Math.min(targets.size(), other.targets.size());
            for (int i = 0; i < size; i++) {
                int cmp = Integer.compare(targets.get(i), other.targets.get(i));
                if (cmp != 0) {
                    return cmp;
                }
            }
            return Integer.compare(targets.size(), other.targets.size());
        }
    }
}
