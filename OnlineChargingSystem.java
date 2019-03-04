import java.util.Hashtable;
import java.util.ArrayList;

/**
 *
 * @author Socrates
 */
public class OnlineChargingSystem {
	ArrayList<UserEquipment> UeArr;
    private OnlineChargingFunctionReservationScheme OCF;
    private AccountBalanceManagementFunction ABMF;
    String reservationSchemeName = "";
    boolean callBack = true;
    
    public OnlineChargingSystem(ArrayList<UserEquipment> ueArr, OnlineChargingFunctionReservationScheme OCF, AccountBalanceManagementFunction ABMF, String reservationSchemeName) {
        this.UeArr = ueArr;
    	this.OCF = OCF;
        this.ABMF = ABMF;
        this.reservationSchemeName = reservationSchemeName;
    }

    public OnlineChargingFunctionReservationScheme getOCF() {
        return OCF;
    }

    public AccountBalanceManagementFunction getABMF() {
        return ABMF;
    }

    public double determineGU(Hashtable hashtable) {
        return this.OCF.determineGU(hashtable);
    }
    
    public double getRemainingDataAllowance() {
        return this.ABMF.getRemainingDataAllowance();
    }
    
    // Functions
    
    // reserve GU
    public double reserveGrantedUnit(Hashtable hashtable) {
        // the hashtable should at least containa number of signals
    	double numOfSignals = (double)hashtable.get("numOfSignals");
        
        // Debit unit request, signals + 1
        numOfSignals += 1;
//        System.out.println("Send Debit unit request");
        
        // Debit unit response, signals + 1
        numOfSignals += 1;
//        System.out.println("Send Debit unit response");
        
        // update the number of signals
        hashtable.put("numOfSignals", numOfSignals);
        
        // compute the reserved granted unit
        double remainingBalance = this.ABMF.getRemainingDataAllowance();
        hashtable.put("remainingDataAllowance", remainingBalance);
        
        
        double reservedGU = 0;
        if(this.reservationSchemeName.equals("IRS")) {
        	OnlineChargingFunctionInventoryBasedReservationScheme IRSOCF = (OnlineChargingFunctionInventoryBasedReservationScheme)this.getOCF();
        	if(IRSOCF.getSurplusGuCondition(hashtable)) {
        		// before calculate the GU, the remaining GU of devices should be called back.
        		this.callBack();
        		
        		remainingBalance = this.ABMF.getRemainingDataAllowance();
        		hashtable.put("remainingDataAllowance", remainingBalance);
        		
        		// re-allocate GU, an array of allocated GUs should be return
        		Hashtable reservedGUs = IRSOCF.getSurplusGu(hashtable);
        		
        		// get the reserved GU for this UE
        		int currentUID = ((Double)hashtable.get("UEID")).intValue();
        		reservedGU = (double)reservedGUs.get(currentUID);
        		System.out.println("Get surplus GU : " + reservedGU);
        		
        		// assign GU
        		this.assignGU(currentUID, reservedGUs);
        		
        	}else {
        		reservedGU = IRSOCF.determineGU(hashtable);
        		System.out.println("Determine GU : " + reservedGU);
        	}
        }else {
        	reservedGU = this.determineGU(hashtable);
        }
        
        this.getABMF().setRemainingDataAllowance(this.getRemainingDataAllowance() - reservedGU);
        
        /*
        if(reservedGU <= this.getABMF().getRemainingDataAllowance()) {
        	// subtract the reserved granted unit
        	System.out.println("Data allowance enough");
            this.getABMF().setRemainingDataAllowance(this.getRemainingDataAllowance() - reservedGU);
        }else {
        	System.out.println("Data allowance not enough");
        	hashtable.put("dataAllowanceNotEnough", 1);
        }
        */
        
        
        
        // send online charging response to tell the UE how much granted unit it can use
        return reservedGU;
        
    }
    
    // Account control operations
    
    // receive online charging request, session start, check balance
    public Hashtable receiveOnlineChargingRequestSessionStart(int ueID, double numOfSignals, double timePeriod) {
        // Debit unit request, signals + 1
        numOfSignals += 1;
//        System.out.println("Send Debit unit request");
        
        // Debit unit response, signals + 1
        numOfSignals += 1;
//        System.out.println("Send Debit unit response");
        
        
        // prepare the hashtable to return the value
        Hashtable<String, Double> hashtable = new Hashtable<String, Double>();
        hashtable.put("UEID", (double)ueID);
        hashtable.put("numOfSignals", numOfSignals);
        hashtable.put("balance", this.ABMF.getRemainingDataAllowance());
        hashtable.put("timePeriod", timePeriod);
        
        // reserve GU
        double reservedGU = this.reserveGrantedUnit(hashtable);
        hashtable.put("reservedGU", reservedGU);
        
        // online charging response, signals + 1
        numOfSignals = (double)hashtable.get("numOfSignals") + 1;
        hashtable.put("numOfSignals", numOfSignals);
        System.out.printf("Num of signals : %5.0f\n", numOfSignals);
        
        return hashtable;
    }
    
    // receive online charging request, session continue
    public Hashtable receiveOnlineChargingRequestSessionContinue(int ueID, double numOfSignals, double reservationCount) {
        Hashtable<String, Double> hashtable = new Hashtable<String, Double>();
        hashtable.put("UEID", (double)ueID);
        hashtable.put("numOfSignals", numOfSignals);
        hashtable.put("reservationCount", reservationCount);
        
        // reserve GU
        double reservedGU = reserveGrantedUnit(hashtable);
        hashtable.put("reservedGU", reservedGU);
        
        // online charging response, signals + 1
        numOfSignals = (double)hashtable.get("numOfSignals");
        numOfSignals = numOfSignals + 1;
        hashtable.put("numOfSignals", numOfSignals);
        
        return hashtable;
    }
    
    // receive online charging request, session end
    public Hashtable receiveOnlineChargingRequestSessionEnd(int ueID, double numOfSignals) {
        Hashtable<String, Double> hashtable = new Hashtable<String, Double>();
        hashtable.put("UEID", (double)ueID);
        
        // Debit unit request, signals + 1
        numOfSignals += 1;
//        System.out.println("Send Debit unit request");
        
        // Debit unit response, signals + 1
        numOfSignals += 1;
//        System.out.println("Send Debit unit response");
        
        // online charging response, signals + 1
        numOfSignals += 1;
//        System.out.println("Send Online charging response");
        
        hashtable.put("numOfSignals", numOfSignals);
        
        return hashtable;
    }
    
    // receiving the current status of user equipments, the report includes UE ID, remaining GU and average data rate
    public void receiveCurrentStatusReport(Hashtable hashtable) {
    	// update the optimal GU when receiving the current status report of each user equipment
    	
    	// get information from the current status report
    	int ueID = 0;
    	double avgDataRate = 0;
    	double remainingGU = 0;
    	double totalDemand = 0;
    	double currentTimePeriod = 1;
    	
    	if( hashtable.containsKey("ueID") && 
    		hashtable.containsKey("avgDataRate") && 
    		hashtable.containsKey("remainingGU") &&
    		hashtable.containsKey("totalDemand") &&
    		hashtable.containsKey("timePeriod")
    		) {
    		ueID = ((Double)hashtable.get("ueID")).intValue();
    		avgDataRate = (double)hashtable.get("avgDataRate");
    		remainingGU = (double)hashtable.get("remainingGU");
    		totalDemand = (double)hashtable.get("totalDemand");
    		currentTimePeriod = (double)hashtable.get("timePeriod");
    	}
    	
    	// record these data in online charging function
    	if(this.reservationSchemeName.equals("IRS")) {
    		OnlineChargingFunctionInventoryBasedReservationScheme IRSOCF = (OnlineChargingFunctionInventoryBasedReservationScheme)this.getOCF();
    		
    		IRSOCF.receiveStatusReport(ueID, avgDataRate, remainingGU, totalDemand, currentTimePeriod);
    	}
    }
    
    // receive total demand and periodical data usage
    public void setTotalDemandAndDataRate(int ueID, double totalDemand, double dataRate) {
    	if(this.reservationSchemeName.equals("IRS")) {
    		OnlineChargingFunctionInventoryBasedReservationScheme IRSOCF = (OnlineChargingFunctionInventoryBasedReservationScheme)this.getOCF();
    		IRSOCF.storeDemandInfo(ueID, totalDemand, dataRate);
    	}
    }
    
    // call back the reserved GU of devices
    public void callBack() {
    	if(callBack) {
    		double calledBackGU = 0;
        	for(int i = 0; i < UeArr.size(); i++) {
        		UserEquipment ue = UeArr.get(i);
        		double withdrewGU = ue.callBack();
        		
        		calledBackGU += withdrewGU;
        	}
        	
        	// add the called back GU to remaining data allowance
        	double remainingDataAllowance = this.getRemainingDataAllowance();
        	remainingDataAllowance += calledBackGU;
        	
        	this.getABMF().setRemainingDataAllowance(remainingDataAllowance);
        	
        	if(remainingDataAllowance < UeArr.size()) {
        		callBack = false;
        	}
        	
        	System.out.println("Called back RD : " + remainingDataAllowance);
    	}
    }
    
    // assign GU
    public void assignGU(int UEID, Hashtable reservedGUs) {
    	System.out.println("Assign GU");
    	double totalReservedGU = 0;
    	for(int i = 0; i < UeArr.size(); i++) {
    		UserEquipment ue = UeArr.get(i);
    		int ID = ue.getUeID();
    		
    		if (ID != UEID) {
    			double reservedGU = (double)reservedGUs.get(ID);
    			totalReservedGU += reservedGU;
        		ue.setCurrentGU(reservedGU);
    		}
    	}
    	
    	// subtract the reserved GUs
    	double remainingDataAllowance = this.getRemainingDataAllowance();
    	this.getABMF().setRemainingDataAllowance(remainingDataAllowance - totalReservedGU);
    }
    
}
