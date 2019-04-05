import java.util.Hashtable;
import java.util.Set;

public class OnlineChargingFunctionInventoryBasedReservationScheme extends OnlineChargingFunctionReservationScheme {
	// Variables of Q model for regular IoT devices
	
	// the fixed number of signals per periodical report needs
	double R;
	// the fixed number of signals of each order
	double S;
	// charging periods (hours)
//	double chargingPeriods;
	
	
	// UE status report variables
	double sumOfEGUs;
	// total demand per month D 
	Hashtable estimatedTotalDemandHashtable;
	// declare a hash table to store the average data usage for each user equipment W
	Hashtable dataUsageHashtable;
	// the granted unit that SBCF always reserves Q 
	Hashtable optimalGUsHashtable;
	
	// remaining GU in each device
	Hashtable remainingGUsHashtable;
	
	// EGU variables
	Hashtable lastReservationTime;
	// latest reporting time
	Hashtable reportingTime;
	// valid time or cycle time
	
	// a hash table to store EGUs
	Hashtable EGUsHashtable;
	// a hash table to store the reserved GU last time
	Hashtable lastReservedGU;
	
	int countSurplusGU_times = 0;
	
	
	// constructor
	public OnlineChargingFunctionInventoryBasedReservationScheme(double defaultGu, double chargingPeriods, double signalsPerReport, double signalsPerOrder) {
		super(defaultGu, chargingPeriods, "IRS");
		this.R = signalsPerReport;
		this.S = signalsPerOrder;
		// change the number of days to the number of hours
//		this.chargingPeriods = chargingPeriods * 24;
		this.defaultGU = defaultGu;
		
		// initialize the hash table, the key is UE ID
		// estimated total demand = data usage * charging period
		this.estimatedTotalDemandHashtable = new Hashtable<Integer, Double>();
		// data usage
		this.dataUsageHashtable = new Hashtable<Integer, Double>();
		// optimal size of GU
		this.optimalGUsHashtable = new Hashtable<Integer, Double>();
		// remaining GU
		this.remainingGUsHashtable = new Hashtable<Integer, Double>();
		// last reservation time
		this.lastReservationTime = new Hashtable<Integer, Double>();
		// latest reporting time
		this.reportingTime = new Hashtable<Integer, Double>();
		// EGU
		this.EGUsHashtable = new Hashtable<Integer, Double>();
		this.lastReservedGU = new Hashtable<Integer, Double>();
	}
	
	// compute the optimal size of granted unit for each user equipment
	public double getOptimalGU(double estiTotalDemand, double periodicalDataUsage) {
		return Math.floor(Math.sqrt(estiTotalDemand * this.S * periodicalDataUsage / this.R));
	}
	
	// update optimal GU(Q) for each user equipment
	public void receiveStatusReport(int ueID, double avgDataUsage, double remainingGU, double totalDemand, double currentTimePeriod) {
		// update the list of average data usage and estimated total demand
		double optimalGU = this.getOptimalGU(totalDemand, avgDataUsage);
		
		// store estimated total demand
		this.estimatedTotalDemandHashtable.put(ueID, totalDemand);
		// store average data usage
		this.dataUsageHashtable.put(ueID, avgDataUsage);
		// store remaining GU
		this.remainingGUsHashtable.put(ueID, remainingGU);
		// store optimal GU
		this.optimalGUsHashtable.put(ueID, optimalGU);
		// update latest reporting time
		this.reportingTime.put(ueID, currentTimePeriod);
		
		/*
		System.out.println("Put Total demand : " + totalDemand);
		System.out.println("Put Periodical data usage : " + avgDataUsage);
		System.out.println("Put Optimal GU : " + optimalGU);
		System.out.println("Put Remaining GU : " + remainingGU);
		System.out.println("Put Reporting time : " + currentTimePeriod);
		System.out.println("Charging periods : " + this.chargingPeriods);
		*/
	}	
	
	// get latest reporting time
	public double getLatestReportingTime(int ueID) {
		// return 0 if there is no record of latest reporting time
		double latestReportingTime = 1;
		if(this.reportingTime.containsKey(ueID)) {
			latestReportingTime = (double)this.reportingTime.get(ueID);
		}
		
		return latestReportingTime;
	}
	
	// compute valid time, the time that the GU in the device is exhausted
	public double computeValidTime(int ueID) {
		// formula : the time duration between each reservation = optimal size of GU / periodical data usage
		double optimalGUsize = 0;
		if(this.optimalGUsHashtable.containsKey(ueID)) {
			optimalGUsize = (double)this.optimalGUsHashtable.get(ueID);
		}
		double periodicalDataUsage = 1;
		if(this.dataUsageHashtable.containsKey(ueID)) {
			periodicalDataUsage = (double)this.dataUsageHashtable.get(ueID);
		}
		
		double expectedTimeDuration = Math.floor(Math.floor(optimalGUsize) / Math.ceil(periodicalDataUsage));
		
		// formula : valid time = last reservation time of UE + the time duration between each reservation
		double ueLastReservationTime = 1;
		if(this.lastReservationTime.containsKey(ueID)) {
			ueLastReservationTime = (double)this.lastReservationTime.get(ueID);
		}
		
		double validTime = ueLastReservationTime + expectedTimeDuration;
		
		
		return validTime;
	}
	
	// compute the expected GU for UE to complete its cycle. Formula 7
	public double getCompleteCycleExpectedGU(int ueID) {
		// formula : (valid time or cycle time - latest reporting time) * average data rate
		double validTime = this.computeValidTime(ueID);
		double latestReportingTime = this.getLatestReportingTime(ueID);
//		System.out.println("Valid time : " + validTime);
//		System.out.println("Last report time : " + latestReportingTime);
		double avgDataRate = 0;
		
		// check if the average data rate is in the hash table
		if(this.dataUsageHashtable.containsKey(ueID)) {
			avgDataRate = (double)this.dataUsageHashtable.get(ueID);
		}
		
		return Math.ceil((validTime - latestReportingTime) * avgDataRate);
	}
	
	// calculating EGU
	public double getEgu(int ueID) {
		// formula : getCompleteCycleExpectedGU() - remaining GU -> if the result is positive
		
		double egu = 0;
		// get the remaining GU of the device
		double remainingGU = 0;
		// check if the remaining GU is in the hash table
		if(this.remainingGUsHashtable.containsKey(ueID)) {
			remainingGU = (double)this.remainingGUsHashtable.get(ueID);
//			System.out.printf("Remaining GU : %f in device UE ID : %d\n", remainingGU, ueID);
			
			// compute the EGU
			
			double completeCycleExpectedGU = this.getCompleteCycleExpectedGU(ueID);
//			System.out.println("Complete cycle expected GU : " + completeCycleExpectedGU);
//			System.out.println("Remaining GU : " + remainingGU);
			if(completeCycleExpectedGU - remainingGU >= 0) {
				egu = completeCycleExpectedGU - remainingGU;
			}
		}else {
			// the remaining GU hash table does not contain the ue ID so it's a regular device
			if(this.optimalGUsHashtable.contains(ueID)) {
				egu = (double)this.optimalGUsHashtable.get(ueID);
			}
		}
		
		egu = Math.ceil(egu);
		
		// put the value in hash table
		this.EGUsHashtable.put(ueID, egu);
		
		return egu;
	}
	
	// get all the keys(UE IDs) in the hash table, to calculate the sum of EGU
	public int[] getKeys() {
		// get the set of the keys in hash table
		Object[] keys = this.optimalGUsHashtable.keySet().toArray();
		
		// declare an array to store those keys
		int[] IDs = new int[keys.length];
		
		// change type to integer
		for(int i = 0; i < keys.length; i++) {
			IDs[i] = (int)keys[i];
		}
		
		return IDs;
	}
	
	// get GU for the device when the remaining data allowance is not enough
	public Hashtable getSurplusGu(Hashtable hashtable) {
		// 取得 UE ID 以及整個 FSP 剩餘的 GU
		int ueID = ((Double)hashtable.get("UEID")).intValue();
		double remainingDataAllowance = (double)hashtable.get("remainingDataAllowance");
		
		// 取出 GUi
		double optimalGuForUe = this.defaultGU;
		if(this.optimalGUsHashtable.containsKey(ueID)) {
			optimalGuForUe = (double)this.optimalGUsHashtable.get(ueID);
		}
		
		// 計算其他裝置未來的 GU 需求
		double sumOfEGUs = this.getSumOfEguAndGuOf(ueID);
//		System.out.println("SUM EGU : " + sumOfEGUs);
		
		// 計算實際應該分配給裝置 i 的 GU
//		double insufficientGU = Math.floor(optimalGuForUe / sumOfEGUs * remainingDataAllowance);
//		if(insufficientGU == 0 && remainingDataAllowance  >= 1) {
//			insufficientGU = 1;
//		}
		
		// update the time that the last reservation made
		double timePeriod = 1;
		if(hashtable.containsKey("timePeriod")) {
			timePeriod = (double)hashtable.get("timePeriod");
		}
		
		int[] ueIDs = this.getKeys();
		
		Hashtable<Integer, Double> insufficientGUs = new Hashtable<Integer, Double>();
		// compute the surplus GU for each device
		boolean movedEven = true;
		countSurplusGU_times++;
		for(int i = 0; i < ueIDs.length; i++) {
			int ID = ueIDs[i];
			double currentOptimalGU = this.defaultGU;
			if(this.optimalGUsHashtable.containsKey(ID)) {
				currentOptimalGU = (double)this.optimalGUsHashtable.get(ID);
			}
			
			double reservedGU = Math.floor(currentOptimalGU / sumOfEGUs * remainingDataAllowance);
			if(reservedGU == 0 && remainingDataAllowance >= ueIDs.length) {
				reservedGU = 1;
			}
			
			insufficientGUs.put(ID, reservedGU);
//			System.out.println("Optimal GU : " + currentOptimalGU);
//			System.out.printf("Surplus GU for ue ID : %d, GU : %f\n", ID, reservedGU);
			
			this.lastReservationTime.put(ID, timePeriod);
			this.lastReservedGU.put(ID, reservedGU);
		}
		
		return insufficientGUs;
	}
	
	// 取得指定 UE 的 GU 以及其他裝置的 EGU 總和
	public double getSumOfEguAndGuOf(int ueID) {
		double optimalGuForUe = 0;
		if(this.optimalGUsHashtable.containsKey(ueID)) {
			optimalGuForUe = (double)this.optimalGUsHashtable.get(ueID);
		}
		
		int[] ueIDs = this.getKeys();
		// compute sum of EGU of regular devices
		double sumOfOtherEGU = 0;
		for(int i = 0; i < ueIDs.length; i++) {
			int currentUeID = ueIDs[i];
			if(currentUeID != ueID) {
				// 取出 optimal GU
				if(this.optimalGUsHashtable.containsKey(currentUeID)) {
//					double EGU = this.getEgu(currentUeID);
					double EGU = (double)this.optimalGUsHashtable.get(currentUeID);
					sumOfOtherEGU += EGU;
				}
			}
		}
		
		return optimalGuForUe + sumOfOtherEGU;
	}

	
	@Override
	public double determineGU(Hashtable hashtable) {
		int ueID = ((Double)hashtable.get("UEID")).intValue();
		
//		double sumOfEGUs = this.getSumOfEguAndGuOf(ueID);
//		double remainingDataAllowance = (double)hashtable.get("remainingDataAllowance");
		
		double reservedGU = this.defaultGU;
		System.out.println("IRS reserved GU : " + reservedGU);
		// if the sum of EGUs <= remaining data allowance, then allocate the optimal granted unit for the device
		
		if(this.optimalGUsHashtable.containsKey(ueID)) {
			reservedGU = (double)this.optimalGUsHashtable.get(ueID);
		}
		
		// update last reservation time
		double timePeriod = 1;
		if(hashtable.containsKey("timePeriod")) {
			timePeriod = (double)hashtable.get("timePeriod");
		}
		this.lastReservationTime.put(ueID, timePeriod);
		this.lastReservedGU.put(ueID, reservedGU);
		
		/*
		System.out.println("===================================");
		System.out.println("UE ID : " + ueID);
		System.out.println("Optimal GU : " + this.optimalGUsHashtable.get(ueID));
		System.out.println("Sum of EGUs : " + sumOfEGUs);
		System.out.println("RD : " + remainingDataAllowance);
		System.out.println("Reserved GU : " + reservedGU);
		*/
		
		return reservedGU;
	}
	
	// get surplus GU condition
	public boolean getSurplusGuCondition(Hashtable hashtable) {
		int ueID = ((Double)hashtable.get("UEID")).intValue();
		
		double sumOfEgu = getSumOfEguAndGuOf(ueID);
		double remainingDataAllowance = (double)hashtable.get("remainingDataAllowance");
		
		boolean getSurplusGU = false;
		if(sumOfEgu > remainingDataAllowance) {
			hashtable.put("dataAllowanceNotEnough", 1);
			getSurplusGU = true;
		}
		return getSurplusGU;
	}
	
	// store total demand and data rate of each device in hash table
	public void storeDemandInfo(int ueID, double totalDemand, double dataRate) {
		this.estimatedTotalDemandHashtable.put(ueID, totalDemand);
		this.dataUsageHashtable.put(ueID, dataRate);
		
		double optimalGU = 0;
		if(dataRate == -1) {
			optimalGU = 700;
		}else {
			optimalGU = this.getOptimalGU(totalDemand, dataRate);			
		}
		this.optimalGUsHashtable.put(ueID, optimalGU);
	}

}
