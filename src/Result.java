/**
 * Attribut synthétisé commun à toutes les procédures de traduction :
 * le fragment construit et le prochain numéro d'état libre après lui.
 *
 * Cet enregistrement remplace l'ancien Fragment.Built. Il n'y a aucun
 * compteur caché : n' = nextFree est produits par la fabrique qui a alloué
 * les états, puis transmis explicitement à l'appelant.
 */
public record Result(Fragment fragment, int nextFree) {

    public Result {
        if (nextFree < 0) {
            throw new IllegalArgumentException("nextFree ne peut pas \u00EAtre n\u00E9gatif");
        }
    }
}
