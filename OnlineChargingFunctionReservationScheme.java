import java.util.Hashtable;



public class OnlineChargingFunctionReservationScheme implements ReservationScheme {
	double defaultGU;
	// charging period (hours)
	double chargingPeriods = 1;
	String reservationScheme;
	
	public OnlineChargingFunctionReservationScheme(double defaultGU, double chargingPeriods, String reservationScheme) {
		this.defaultGU = defaultGU;
		this.chargingPeriods = chargingPeriods;
		this.reservationScheme = reservationScheme;
	}

	@Override
	public double determineGU(Hashtable hashtable) {
		// TODO Auto-generated method stub
		return this.defaultGU;
	}
	
	public double getSurplusGu(double remainingDataAllowance) {
		return remainingDataAllowance;
	}
	
	public double getChargingPeriods() {
		return this.chargingPeriods;
	}
	
	public void setChargingPeriod(double chargingPeriods) {
		this.chargingPeriods = chargingPeriods;
	}
	
}
