/** Transition d'un automate : source --label--> cible, label null = epsilon. */
public record Transition(int source, String label, int target) {

    public boolean isEpsilon() {
        return label == null;
    }

    @Override
    public String toString() {
        return source + " -" + (label == null ? "\u03B5" : label) + "-> " + target;
    }
}
