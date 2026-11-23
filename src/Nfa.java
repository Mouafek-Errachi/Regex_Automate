import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Automate complet : état initial, état acceptant, ensemble des états
 * et transitions. Les deux collections sont immuables.
 * Aucun compteur n'est stocké ici : stateCount() est déduit des états.
 */
public record Nfa(int init, int accept, Set<Integer> states, List<Transition> transitions) {

    public Nfa {
        states = Set.copyOf(states);
        transitions = List.copyOf(transitions);
    }

    /** Fabrique pratique à partir du fragment racine. */
    public Nfa(int init, int accept, Fragment fragment) {
        this(init, accept, collectStates(fragment), fragment.transitions());
    }

    /** Fabrique vérifiant que la numérotation est exactement 0..nextFree-1. */
    public static Nfa of(Result result) {
        Fragment fragment = result.fragment();
        Nfa nfa = new Nfa(fragment.init(), fragment.fin(), fragment);
        if (nfa.stateCount() != result.nextFree()) {
            throw new IllegalStateException(
                    "Num\u00E9rotation incoh\u00E9rente : nextFree = " + result.nextFree()
                    + " mais " + nfa.stateCount() + " \u00E9tats pr\u00E9sents");
        }
        for (int i = 0; i < result.nextFree(); i++) {
            if (!nfa.states().contains(i)) {
                throw new IllegalStateException("\u00C9tat " + i + " manquant (num\u00E9ro perdu)");
            }
        }
        return nfa;
    }

    private static Set<Integer> collectStates(Fragment fragment) {
        TreeSet<Integer> all = new TreeSet<>();
        all.add(fragment.init());
        all.add(fragment.fin());
        for (Transition t : fragment.transitions()) {
            all.add(t.source());
            all.add(t.target());
        }
        return all;
    }

    public int stateCount() {
        return states.size();
    }
}
