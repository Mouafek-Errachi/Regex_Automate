import java.util.ArrayList;
import java.util.List;

/**
 * Fragment de Thompson immutable : un état initial, un état final,
 * et la liste des transitions déjà construites.
 *
 * Chaque fabrique reçoit n (prochain numéro libre hérité) et rend un Result
 * (fragment, n' = prochain numéro libre synthétisé). Aucun compteur static,
 * aucun état partagé : n est un simple entier passé par valeur.
 */
public record Fragment(int init, int fin, List<Transition> transitions) {

    public Fragment {
        transitions = List.copyOf(transitions); // défense contre toute mutation
    }

    /** i -c-> f : alloue 2 états, n' = n + 2. */
    public static Result symbole(String c, int n) {
        int i = n;
        int f = n + 1;
        return new Result(
                new Fragment(i, f, List.of(new Transition(i, c, f))),
                n + 2);
    }

    /** i -ε-> f : alloue 2 états, n' = n + 2. */
    public static Result epsilon(int n) {
        int i = n;
        int f = n + 1;
        return new Result(
                new Fragment(i, f, List.of(new Transition(i, null, f))),
                n + 2);
    }

    /** Nouveaux i, f ; i->init(r), i->init(s), fin(r)->f, fin(s)->f. n' = n + 2. */
    public static Result union(Fragment r, Fragment s, int n) {
        int i = n;
        int f = n + 1;
        List<Transition> t = new ArrayList<>();
        t.add(new Transition(i, null, r.init()));
        t.add(new Transition(i, null, s.init()));
        t.add(new Transition(r.fin(), null, f));
        t.add(new Transition(s.fin(), null, f));
        t.addAll(r.transitions()); // réutilisation des états, aucun clonage
        t.addAll(s.transitions());
        return new Result(new Fragment(i, f, t), n + 2);
    }

    /** fin(r) -> init(s) ; aucun état alloué, n' = n. */
    public static Result concat(Fragment r, Fragment s, int n) {
        List<Transition> t = new ArrayList<>(r.transitions());
        t.add(new Transition(r.fin(), null, s.init()));
        t.addAll(s.transitions());
        return new Result(new Fragment(r.init(), s.fin(), t), n);
    }

    /** Étoile : boucle + passe vide. alloue i, f, n' = n + 2. */
    public static Result star(Fragment r, int n) {
        int i = n;
        int f = n + 1;
        List<Transition> t = new ArrayList<>();
        t.add(new Transition(i, null, r.init()));
        t.add(new Transition(r.fin(), null, f));
        t.add(new Transition(r.fin(), null, r.init())); // boucle
        t.add(new Transition(i, null, f));              // passe vide
        t.addAll(r.transitions());
        return new Result(new Fragment(i, f, t), n + 2);
    }

    /** Plus : boucle SANS passe vide. alloue i, f, n' = n + 2. */
    public static Result plus(Fragment r, int n) {
        int i = n;
        int f = n + 1;
        List<Transition> t = new ArrayList<>();
        t.add(new Transition(i, null, r.init()));
        t.add(new Transition(r.fin(), null, f));
        t.add(new Transition(r.fin(), null, r.init())); // boucle
        t.addAll(r.transitions());
        return new Result(new Fragment(i, f, t), n + 2);
    }

    /** Optionnel : passe vide SANS boucle. alloue i, f, n' = n + 2. */
    public static Result optional(Fragment r, int n) {
        int i = n;
        int f = n + 1;
        List<Transition> t = new ArrayList<>();
        t.add(new Transition(i, null, r.init()));
        t.add(new Transition(r.fin(), null, f));
        t.add(new Transition(i, null, f)); // passe vide
        t.addAll(r.transitions());
        return new Result(new Fragment(i, f, t), n + 2);
    }
}
