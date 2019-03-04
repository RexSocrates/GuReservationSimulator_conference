import java.util.Hashtable;

/**
 *
 * @author Socrates
 */
public interface ReservationScheme {
    // every data that the reservation scheme need should be put in the hash table
    public double determineGU(Hashtable hashtable);
}
