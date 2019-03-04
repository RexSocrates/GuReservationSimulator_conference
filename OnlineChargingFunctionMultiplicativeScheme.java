import java.util.Hashtable;



/**
 *
 * @author Socrates
 */
public class OnlineChargingFunctionMultiplicativeScheme extends OnlineChargingFunctionReservationScheme {
    private double c = 1;
    
    public OnlineChargingFunctionMultiplicativeScheme(double defaultGU, double c, double chargingPeriods) {
        super(defaultGU, chargingPeriods, "MS");
        this.c = c;
    }

    public double getDefaultGU() {
        return defaultGU;
    }

    public double getC() {
        return c;
    }

    @Override
    public double determineGU(Hashtable hashtable) {
        // j is the number of reservations run by UE
        double j = 1;
        if(hashtable.containsKey("reservationCount")) {
            j = (double)hashtable.get("reservationCount");
        }
        System.out.printf("J : %5.0f\n", j);
        double reservedGU = Math.ceil(j / this.c) * this.getDefaultGU();
        
        double remainingDataAllowance = (double)hashtable.get("remainingDataAllowance");
        
        if(reservedGU >= remainingDataAllowance) {
        	reservedGU = remainingDataAllowance;
        	hashtable.put("dataAllowanceNotEnough", 1);
        }
        
        return reservedGU;
    }
}
