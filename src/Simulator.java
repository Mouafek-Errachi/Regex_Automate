import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/** Simulation d'un AFN de Thompson sur un mot, avec ε-fermeture itérative. */
public final class Simulator {

    private Simulator() {
        // classe utilitaire : aucune instance
    }

    /** Renvoie true si nfa accepte exactement word. */
    public static boolean accepts(Nfa nfa, String word) {
        Set<Integer> current = epsilonClosure(nfa, Set.of(nfa.init()));

        for (int i = 0; i < word.length(); i++) {
            String label = String.valueOf(word.charAt(i));
            Set<Integer> reached = new HashSet<>();

            for (int state : current) {
                for (Transition t : nfa.transitions()) {
                    if (t.source() == state && label.equals(t.label())) {
                        reached.add(t.target());
                    }
                }
            }

            current = epsilonClosure(nfa, reached);
            if (current.isEmpty()) {
                break; // plus aucun état actif : les lectures suivantes resteront vides
            }
        }

        return current.contains(nfa.accept());
    }

    /**
     * ε-fermeture : pile + ensemble de déjà visités, donc pas de boucle infinie
     * même sur un cycle ε (par exemple (a*)*).
     */
    private static Set<Integer> epsilonClosure(Nfa nfa, Set<Integer> start) {
        Set<Integer> visited = new HashSet<>(start);
        ArrayDeque<Integer> stack = new ArrayDeque<>(start);

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
}
