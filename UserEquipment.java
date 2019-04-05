import java.util.ArrayList;
import java.util.Hashtable;
/**
 *
 * @author Socrates
 */
public class UserEquipment {
	// Basic variables
	// consider the UE ID is its IMEI
    private int ueID;
    private OnlineChargingSystem OCS;
    // store the remaining GU in the device
    private double currentGU = 0.0;
    // record the number of signals that the device produce
    private double producedSignals = 0;
    // record the current time
    private double currentTimePeriod = 1;
    // count the sessions include completed and failed sessions
    double numberOfSessions = 0;
    // record the times that session fails
    double sessionFailedTimes = 0;
    // record the times that UE interact with OCS
    double interaction = 0;
    
    // store a single period of allocated GUs
    ArrayList<Double> allocatedGUs;
    
    // store multiple periods' allocated GUs
    ArrayList<SinglePeriodAllocatedGUs> periodAllocatedRecords;
    
    // represent the reservation with acronym
    String reservationScheme = "";
    // if the reservation need each UE to report their current status, then this variable is true, otherwise it's false
    boolean reportUeStatus;
    
    // IRS variables
    String deviceType = "";
    // total demand per month, Unit : MB 
    double totalDemand;
    // periodical data usage
    double periodicalDataUsage;
    // charging periods, unit : hour
    double chargingPeriods;
    // data collection time, unit : hour
    double dataCollectionPeriod;
    // report interval
    double reportInterval;
    // count the times that Credit Control Request sent
    double countRequestTimes = 0;
    // count the times that Credit Control Request sent and a new granted unit has been allocated.
    double countSuccessfulRequestTimes = 0;
    // count the times that CCR sent and no granted unit has been allocated
    double countFailedRequestTimes = 0;
    
    // charging data, index is date, value is the daily usage of the cell
    DailyUsage dailyUsage;
    
    // constructor for FS or MS
    public UserEquipment(int ID, OnlineChargingSystem OCS, String reservationScheme) {
        this.ueID = ID;
        this.OCS = OCS;
        this.currentGU = 0;
        this.allocatedGUs = new ArrayList<Double>();
        this.periodAllocatedRecords = new ArrayList<SinglePeriodAllocatedGUs>();
        this.reservationScheme = reservationScheme;
        this.reportUeStatus = true;
        this.currentTimePeriod = 1;
        // charging data
        this.dailyUsage = new DailyUsage();
    }
    
    // constructor for IRS
    public UserEquipment(int ID, String deviceType, OnlineChargingSystem OCS, double chargingPeriods, double dataCollectionPeriod, double reportInterval, double totalDemand, double periodicalDataUsage, String reservationScheme) {
    	this.ueID = ID;
    	this.deviceType = deviceType;
        this.OCS = OCS;
        this.currentGU = 0;
        this.allocatedGUs = new ArrayList<Double>();
        this.periodAllocatedRecords = new ArrayList<SinglePeriodAllocatedGUs>();
        this.reservationScheme = reservationScheme;
        this.reportUeStatus = true;
        this.currentTimePeriod = 1;
        this.totalDemand = totalDemand;
        this.periodicalDataUsage = periodicalDataUsage;
        
    	// change days to hours
    	this.chargingPeriods = chargingPeriods;
    	this.dataCollectionPeriod = dataCollectionPeriod;
    	this.reportInterval = reportInterval;
    	
    	// charging data
    	this.dailyUsage = new DailyUsage();
    }

    // getter and setter
    public int getUeID() {
    	return this.ueID;
    }
    
    public double getCurrentGU() {
        return this.currentGU;
    }

    public void setCurrentGU(double currentGU) {
        this.currentGU = currentGU;
    }

    public double getProducedSignals() {
        return producedSignals;
    }

    public void setProducedSignals(double producedSignals) {
        this.producedSignals = producedSignals;
    }
    
    public double getSessionFailedTimes() {
    	return this.sessionFailedTimes;
    }
    
    public void setSessionFailedTimes(double sessionFailedTimes) {
    	this.sessionFailedTimes = sessionFailedTimes;
    }
    
    public double getTotalDemand() {
    	return this.totalDemand;
    }
    
    public void setTotalDemand(double totalDemand) {
    	this.totalDemand = totalDemand;
    }
    
    public void setPeriodicalDataUsage(double periodicalDataUsage) {
    	this.periodicalDataUsage = periodicalDataUsage;
    }
    
    public double getPeriodicalDataUsage() {
    	// sum previously allocated GU
    	double allocatedGU = 0;
    	for(int index = 0; index < periodAllocatedRecords.size(); index++) {
    		SinglePeriodAllocatedGUs singlePeriodRecord = periodAllocatedRecords.get(index);
    		allocatedGU += singlePeriodRecord.getSumOfGUs();
    	}
    	
    	// subtract remaining GU
    	double consumedGU = allocatedGU - this.getCurrentGU();
    	
    	double dataRate = 0;
    	if(periodAllocatedRecords.size() > 0) {
    		dataRate = consumedGU / periodAllocatedRecords.size();    		
    	}else {
    		// average data usage per hour in dataset
    		dataRate = -1;
    	}
    	this.setPeriodicalDataUsage(dataRate);
    	
    	return dataRate;
    }
    
    public double getSuccessfulRate() {
    	double successfulTimes = this.numberOfSessions - this.sessionFailedTimes;
    	return successfulTimes / this.numberOfSessions;
    	
//    	double successRate = (this.countRequestTimes - this.countFailedRequestTimes) / this.countRequestTimes;
//    	double successRate = this.countSuccessfulRequestTimes / this.countRequestTimes;
//    	return successRate;
    }
    
    public DailyUsage getDailyUsage() {
    	return this.dailyUsage;
    }
    
    public void setDailyUsage(DailyUsage dailyUsage) {
    	this.dailyUsage = dailyUsage;
    }
    
    
    // compute IRS variables
    // compute periodical data rate in previous hours
    public double computePeriodicalDataRate(double currentTime, double dataCollectionPeriods) {
    	double startTime = 1;
    	if(currentTime > dataCollectionPeriods) {
    		startTime = currentTime - dataCollectionPeriods;
    	}
    	
    	double totalUsageInDataCollectionPeriods = this.computeTotalGuConsumption(startTime, currentTime);
    	
    	double dataRate = totalUsageInDataCollectionPeriods / (currentTime - startTime);
    	
    	return dataRate;
    }
    
    // compute total GU consumption
    public double computeTotalGuConsumption(double startTime, double currentTime) {
    	double totalUsageInDataCollectionPeriods = 0;
    	for(int time = (int)startTime; time < (int)currentTime; time++) {
    		totalUsageInDataCollectionPeriods += dailyUsage.getHourlyUsage(time);
    	}
    	
    	return totalUsageInDataCollectionPeriods;
    }
    
    // Functions
    
    // return current status, including remaining GU of UE and the average data rate
    public Hashtable reportCurrentStatus(double currentTime) {
    	Hashtable<String, Double> hashtable = new Hashtable<String, Double>();
    	if(needToReport(currentTime)) {
    		interaction += 1;
        	
//        	System.out.printf("UE ID : %d\n", this.ueID);
//        	System.out.printf("Periodical data usage : %f\n", periodicalDataUsage);
//        	System.out.printf("Remaining GU : %f\n", this.currentGU);
        	
        	// add the content of current status report
        	hashtable.put("ueID", (double)this.ueID);
        	hashtable.put("avgDataRate", this.getPeriodicalDataUsage());
        	hashtable.put("totalDemand", this.totalDemand);
        	hashtable.put("remainingGU", this.currentGU);
        	
//        	System.out.println("Periodical data usage : " + periodicalDataUsage);
//        	System.out.println("Remaining GU : " + this.getCurrentGU());
//        	System.out.println("==============================");
        	
        	// add the time period to tell the OCS that the current time
        	hashtable.put("timePeriod", currentTime);
        	
        	// add the number of signals used by one report
//        	System.out.printf("Report current status, UE ID : %d\n", this.ueID);
        	this.producedSignals += 1;
        	
        	this.OCS.receiveCurrentStatusReport(hashtable);
    	}
    	
    	return hashtable;
    }
    
    // to determine whether to report current status
    public boolean needToReport(double currentTime) {
    	boolean report = false;
    	
    	if(this.getCurrentGU() > 0 || currentTime == 1 || this.reportUeStatus) {
    		report = true;
    	}
    	return report;
    }
    
    // to complete a session, giving a granted unit that a session needs and the time that the session created
    public void completeSession(double sessionTotalGU, double timePeriod) {
    	// session counter += 1
        this.numberOfSessions += 1;
    	
    	// update the current time of the device
    	this.currentTimePeriod = timePeriod;
    	
    	// session service
    	if(sessionTotalGU <= this.getCurrentGU()) {
    		// if the GU in this device is enough then consume the remaining GU
    		this.setCurrentGU(this.getCurrentGU() - sessionTotalGU);
//    		System.out.println("Consumed GU : " + sessionTotalGU);
    	}else {
    		// if it is not enough then ask for new GU from the OCS
    		this.askNewGU(sessionTotalGU, timePeriod);
    	}
    }
    
    public void askNewGU(double sessionTotalGU, double timePeriod) {
    	// send online charging request to ask new GU
    	boolean dataAllowanceNotEnough = this.sendOnlineChargingRequestSessionStart(timePeriod);
//    	System.out.println("Ask new GU : data allowance not enough : " + dataAllowanceNotEnough);
        
        if(dataAllowanceNotEnough) {
        	// when the remaining data allowance is not enough, session ends
        	this.sendOnlineChargingRequestSessionEnd();
        	
        	if(sessionTotalGU <= this.getCurrentGU()) {
        		// the allocated surplus GU is enough for this session
        		this.setCurrentGU(this.getCurrentGU() - sessionTotalGU);
        	}else {
        		// the allocated surplus GU is not enough for this session
        		this.setCurrentGU(0);
        		this.sessionFailedTimes += 1;
        		this.countFailedRequestTimes += 1;
        	}
        }
        else {
        	// when the remaining data allowance is enough, session continues
        	this.consumeGU(sessionTotalGU);
            this.sendOnlineChargingRequestSessionEnd();
        }
        
        
        
        // add those allocated GUs into a single record
        this.periodAllocatedRecords.add(new SinglePeriodAllocatedGUs(timePeriod, this.allocatedGUs));
        
        // initialize the allocated GUs after the record of previous period is stored
        this.allocatedGUs = new ArrayList<Double>();
    }
    
    
    // session start, requesting GU
    public boolean sendOnlineChargingRequestSessionStart(double timePeriod) {
    	this.countRequestTimes += 1;
    	this.interaction += 1;
//        System.out.println("sendOnlineChargingRequestSessionStart");
        
        // call next function, the parameter is a signals counter, it will return the number of signals
        Hashtable hashtable = this.OCS.receiveOnlineChargingRequestSessionStart(this.ueID, 1, timePeriod);
        
        boolean dataAllowanceNotEnough = false;
        if(hashtable.containsKey("dataAllowanceNotEnough")) {
        	// if the key is contained in the hash table, then the remaining data allowance is not enough
        	dataAllowanceNotEnough = true;
        }
        
        // keys : numOfSignals, balance, reservedGU
        double numOfSignals = (double)hashtable.get("numOfSignals");
        
        // add the number of signals
        this.setProducedSignals(this.getProducedSignals() + numOfSignals);
//        System.out.printf("Num of signals : %5.0f\n", numOfSignals);
        
        // update granted unit
        double allocatedGU = (double) hashtable.get("reservedGU");
        if(allocatedGU > 0) {
        	this.countSuccessfulRequestTimes += 1;
        }
        this.setCurrentGU(this.getCurrentGU() + allocatedGU);
        // add the allocated GU to the list
        this.allocatedGUs.add(allocatedGU);
//        System.out.printf("Session start, reserved GU : %f\n", allocatedGU);
        
        return dataAllowanceNotEnough;
    }
    
    // consuming granted unit
    public void consumeGU(double consumedGU) {
    	
    	boolean dataAllowanceNotEnough = false;
    	int reservationCount = 1;
    	while(this.getCurrentGU() < consumedGU) {
    		// the remaining GU in the device is not enough so keep asking new GU until the allocated GU is enough
    		
    		dataAllowanceNotEnough = this.sendOnlineChargingRequestSessionContinue(reservationCount++);
//    		System.out.println("consume GU data allowance not enough : " + dataAllowanceNotEnough);
    		if(dataAllowanceNotEnough) {
    			// if the remaining GU is not enough then break the loop
    			break;
    		}
    	}
    	
    	if(dataAllowanceNotEnough) {
    		this.sessionFailedTimes += 1;
    	}else {
    		// if the remaining GU is enough then consume GU
    		this.setCurrentGU(this.getCurrentGU() - consumedGU);
    	}
    }
    
    // session continue, requesting GU
    public boolean sendOnlineChargingRequestSessionContinue(double reservationCount) {
    	this.countRequestTimes += 1;
    	this.interaction += 1;
//    	System.out.println("sendOnlineChargingRequestSessionContinue");
        
        // send the online charging request, so the initial number of signals is 1
        Hashtable<String, Double> hashtable = this.OCS.receiveOnlineChargingRequestSessionContinue(this.ueID, 1, reservationCount);
        
        boolean dataAllowanceNotEnough = false;
        if(hashtable.containsKey("dataAllowanceNotEnough")) {
        	// the remaining data allowance is not enough
        	dataAllowanceNotEnough = true;
        }
        
        double reservedGU = (double) hashtable.get("reservedGU");
//        System.out.printf("Session continue, reserved GU : %f\n", reservedGU);
        
        if(reservedGU > 0) {
        	this.countSuccessfulRequestTimes += 1;
        }
        this.setCurrentGU(this.getCurrentGU() + reservedGU);
        this.allocatedGUs.add(reservedGU);
        
        double numOfSignals = hashtable.get("numOfSignals");
//        System.out.printf("Number of signals : %3.0f\n", numOfSignals);
        
        // add the number of signals to the variable produced signals
        this.setProducedSignals(this.getProducedSignals() + numOfSignals);
        
        return dataAllowanceNotEnough;
    }
    
    // session end
    public void sendOnlineChargingRequestSessionEnd() {
//    	this.countRequestTimes += 1;
    	this.interaction += 1;
//    	System.out.println("sendOnlineChargingRequestSessionEnd");
    	
        // send the online charging request, so the initial number of signals is 1
        Hashtable<String, Double> hashtable = this.OCS.receiveOnlineChargingRequestSessionEnd(this.ueID, 1);
        
        // add number of signals
        double numOfSignals = hashtable.get("numOfSignals");
        this.setProducedSignals(this.getProducedSignals() + numOfSignals);
    }
    
    // call back remaining GU
    public double callBack() {
    	this.interaction += 1;
    	this.setProducedSignals(this.getProducedSignals() + 1);
    	// when the remaining data allowance is not enough, the OCS will take the back the remaining GU of devices 
    	double withdrewGU = this.getCurrentGU();
    	this.setCurrentGU(this.getCurrentGU() - withdrewGU);
    	
    	return withdrewGU;
    }
}
