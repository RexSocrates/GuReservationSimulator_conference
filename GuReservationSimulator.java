
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
//import java.time.LocalDate;
//import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.Scanner;


/**
 *
 * @author Socrates
 */
public class GuReservationSimulator {
    static Scanner input = new Scanner(System.in);
    static ArrayList<UserEquipment> UeArr = new ArrayList<UserEquipment>();
    static OnlineChargingSystem OCS;
    static double defaultGU = 0;
    // count by hours
    static double chargingPeriods = 168;
    static double reportInterval = 1;
    static double dataCollectionPeriods = 1;
    static int[] cellIDs;
    static String sampleIndexStr = "sample_";
    static ArrayList<ExpResult> resultArr = new ArrayList<ExpResult>();

    
    public static void main(String[] args) throws FileNotFoundException {
    	// print reservation scheme options
        
    	getRandomSampleIndex();
//        System.out.print("Enter the number of devices : ");
//        int numOfDevices = input.nextInt();
//        System.out.println("");
    	
    	File deviceFile = new File("numOfDevices.txt");
    	Scanner deviceFileInput = new Scanner(deviceFile);
    	int numOfDevices = deviceFileInput.nextInt();
    	deviceFileInput.close();
    	
    	PrintWriter pw = new PrintWriter("numOfDevices.txt");
    	int newDevicesNum = numOfDevices + 1;
    	pw.print(newDevicesNum);
    	pw.close();
    	
        cellIDs = new int[numOfDevices];

        String[] reservationSchemes = {
        		"Fixed scheme",
        		"Multiplicative scheme",
        		"Inventory-based Reservation Scheme"
        };
        
//        for(int i = 0; i < reservationSchemes.length; i++) {
//        	System.out.printf("%2d . %s\n", i+1, reservationSchemes[i]);
//        }
//        System.out.print("Choose the reservation scheme : ");
//        int option = input.nextInt();
        int option = 3;
            
//        System.out.println("");
            
        // configure the experiment
        double totalDataAllowance = dataAllowanceSetting(numOfDevices);
            
        switch(option) {
            case 1 : OCS = fixedScheme(totalDataAllowance);
                break;
            case 2 : OCS = multiplicativeScheme(totalDataAllowance);
                break;
            case 3 : OCS = inventoryBasedReservationScheme(totalDataAllowance);
            	break;
        }
            
        // add the user equipments into the array
        initializeUserEquipments(numOfDevices, option);
        readTotalUsageFile();
            
        // stimulate that time is moving
//        int deviceCount = 0;
        double timePeriod = 0;
        reportCurrentStatus(timePeriod++);
//        int loopCount = 0;
          
        setTotalDemandAndDataRate();
          
        while(chargingProcessContinue(OCS.getRemainingDataAllowance(), timePeriod)) {
        	if(timePeriod % reportInterval == 0) {
        		reportCurrentStatus(timePeriod);
        	}
          	
        	System.out.println("Time period : " + timePeriod);
        	for(int i = 0; i < UeArr.size(); i++) {
          		UserEquipment ue = UeArr.get(i);
          		DailyUsage ueDailyUsage = ue.getDailyUsage();
          		int intTime = (new Double(timePeriod)).intValue();
          		double consumedGU = ueDailyUsage.getHourlyUsage(intTime);
          		
          		System.out.println("UE ID : " + ue.getUeID());
          		System.out.println("Consumed GU : " + consumedGU);
          		System.out.println("");
          		
          		ue.completeSession(consumedGU, timePeriod);
          	}
              
//        	System.out.printf("Remaining data allowance : %10.2f\n", OCS.getRemainingDataAllowance());
              
        	timePeriod += 1;
        }
          
        // get total signals of this operation
        countTotalSignals(UeArr);
          
        // print experiment configuration
        System.out.printf("Number of devices : %d\n", numOfDevices);
        System.out.printf("Reservation scheme : %s\n", reservationSchemes[option - 1]);
        System.out.printf("Monthly data allowance : %3.0f\n", totalDataAllowance);
        System.out.printf("Remaining data allowance : %3.0f\n", OCS.getABMF().getRemainingDataAllowance());
        System.out.println("Data collection period : " + dataCollectionPeriods);
        System.out.printf("Default GU : %5.0f\n", defaultGU);
          
        writeExperimentResult(numOfDevices, reservationSchemes[option - 1], totalDataAllowance, defaultGU);
    }
    

	private static void getRandomSampleIndex() throws FileNotFoundException {
		File sampleIndexFile = new File("sampleIndex.txt");
		Scanner sampleIndexFileInput = new Scanner(sampleIndexFile);
		int sampleIndex = sampleIndexFileInput.nextInt();
		
		if(sampleIndex < 10) {
			sampleIndexStr += "0" + sampleIndex + "_";
		}else {
			sampleIndexStr += sampleIndex + "_";
		}
		sampleIndexFileInput.close();
		
//		PrintWriter pw = new PrintWriter("sampleIndex.txt");
//		pw.println(++sampleIndex);
//		pw.close();
	}

	private static void initializeUserEquipments(int numOfDevices, int option) throws FileNotFoundException {
		System.out.println("++++++++++++++++++++");
    	// randomly select the user equipment
    	int[] ueIDs = new int[numOfDevices];
    	for(int i = 0; i < numOfDevices; i++) {
    		ueIDs[i] = i + 1;
    		/*
    		int selectedNewID = (int)(Math.random() * 20);
    		
    		// check whether the selected ID is in the list
    		boolean idInTheList = false;
    		for(int j = 0; j < ueIDs.length; j++) {
    			if(selectedNewID == ueIDs[j]) {
    				idInTheList = true;
    				break;
    			}
    		}
    		
    		
    		if(idInTheList) {
    			i--;
    		}else {
    			ueIDs[i] = selectedNewID;
    		}
    		*/
    	}
    	
    	
    	// read selected UE IDs
    	/*
    	String selectedIDsFile = "IDs.csv";
    	File file = new File(selectedIDsFile);
    	
    	Scanner inputFile = new Scanner(file);
    	
    	// remove title
    	inputFile.nextLine();
    	
    	for(int i = 0; i < ueIDs.length; i++) {
    		ueIDs[i] = inputFile.nextInt();
    	}
    	
    	inputFile.close();
    	
    	
    	for(int i = 0; i < ueIDs.length; i++) {
    		System.out.println("Cell ID : " + ueIDs[i]);
    	}
    	
    	Arrays.sort(ueIDs);
    	*/
    	
    	dataCollectionPeriods = 0;
    	
    	
    	double[] totalDemands = new double[numOfDevices];
    	double[] dataUsages = new double[numOfDevices];
    	
    	if(option == 3) {
    		// enter some variable that IRS needs
//    		System.out.print("Enter data collection periods(hour 1 ~ 168) : ");
//        	dataCollectionPeriods = input.nextDouble();
    		dataCollectionPeriods = 10;
        	
        	// read period length file
    		/*
        	String periodFileName = "periods.txt";
        	File periodFile = new File(periodFileName);
        	Scanner periodFileInput = new Scanner(periodFile);
        	
        	dataCollectionPeriods = periodFileInput.nextDouble();
        	
        	// write period file
        	PrintWriter pw = new PrintWriter(periodFileName);
        	double newPeriodLength = dataCollectionPeriods + 1;
        	pw.print(newPeriodLength);
        	pw.close();
        	System.out.println("");
        	*/
        	
        	
//        	System.out.print("Enter report interval(hour) : ");
//        	reportInterval = input.nextDouble();
        	reportInterval = dataCollectionPeriods;
        	System.out.println("");
        	
        	Hashtable<String, double[]> dataRateAndTotalUsage = getPeriodicalDataUsageAndTotalUsage(numOfDevices, ueIDs);
        	
        	totalDemands = (double[]) dataRateAndTotalUsage.get("totalUsage");
        	dataUsages = (double[]) dataRateAndTotalUsage.get("dataRate");
    	}
    	
    	
    	
		for(int i = 0; i < ueIDs.length; i++) {
			int ueID = ueIDs[i];
			
			if(option == 1 || option == 2) {
				// fixed scheme
				UeArr.add(new UserEquipment(ueID, OCS, "FS"));
			}else if(option == 2) {
				// multiplicative scheme
				UeArr.add(new UserEquipment(ueID, OCS, "MS"));
			}else if(option == 3) {
				// Inventory-based reservation scheme
				UeArr.add(new UserEquipment(ueID, "Regular", OCS, chargingPeriods, dataCollectionPeriods, reportInterval, totalDemands[i], dataUsages[i], "IRS"));
			}
		}
	}

	// File IO Functions
	// read quota usage in each time period from the file
	private static void readTotalUsageFile() throws FileNotFoundException {
		/*
		String fileName = "usage_01.csv";
		File file = new File(fileName);
		Scanner inputFile = new Scanner(file);
		
		// remove title
		inputFile.nextLine();
		
//		int countDevice = 0;
		
		while(inputFile.hasNext()) {
			String tuple = inputFile.nextLine();
			String[] tupleData = tuple.split(",");
			int cellID = Integer.parseInt(tupleData[0]);
			double time = Double.parseDouble(tupleData[1]);
			double totalUsage = Double.parseDouble(tupleData[2]);
			
			for(int i = 0; i < UeArr.size(); i++) {
				UserEquipment ue = UeArr.get(i);
				
				if(cellID == ue.getUeID()) {
					DailyUsage ueDailyUsage = ue.getDailyUsage();
					int intTime = (new Double(time)).intValue();
					ueDailyUsage.addHourlyUsage(intTime, totalUsage);
					
					System.out.println("==================================");
					System.out.println("Cell ID : " + cellID);
					System.out.println("Time : " + time);
					System.out.println("Total usage : " + totalUsage);
//					countDevice++;
					break;
				}
			}
			
//			if(countDevice >= UeArr.size()) {
//				break;
//			}
		}
		
		inputFile.close();
		*/
		
		for(int day = 1; day <= 7; day++) {
			String dateString = sampleIndexStr + "kb_shrink_2013_11_0" + day + "_";
			for(int hour = 0; hour <= 23; hour++) {
				String fileName = "";
				if(hour < 10) {
					fileName = dateString + "0" + hour + ".csv";
				}else {
					fileName = dateString + hour + ".csv";
				}
				
				File file = new File(fileName);
				Scanner inputFile = new Scanner(file);
				
				System.out.println("Read log file : " + fileName);
				
				// remove title
				inputFile.nextLine();
				
				while(inputFile.hasNext()) {
					String tuple = inputFile.nextLine();
					String[] tupleArr = tuple.split(",");
					
					int ueID = Integer.parseInt(tupleArr[0]);
					double internetUsage = Math.ceil(Double.parseDouble(tupleArr[1]));
					
					int time = (day - 1) * 24 + hour;
					
					// insert the Internet usage into UE
					for(int i = 0; i < UeArr.size(); i++) {
						UserEquipment ue = UeArr.get(i);
						
						if(ueID == ue.getUeID()) {
							DailyUsage ueDailyUsage = ue.getDailyUsage();
							
							ueDailyUsage.addHourlyUsage(time, internetUsage);
							
							break;
						}
					}
				}
				
				inputFile.close();
			}
		}
	}
	
	private static Hashtable<String, double[]> getPeriodicalDataUsageAndTotalUsage(int numberOfDevices, int[] cellIDs) throws FileNotFoundException {
		Hashtable<String, double[]> dataRateAndTotalUsage = new Hashtable<String, double[]>();
		double[] dataRate = new double[numberOfDevices];
		double[] totalUsage = new double[numberOfDevices];
		
		readDataRateFile(cellIDs, dataRate, totalUsage);
		
		dataRateAndTotalUsage.put("dataRate", dataRate);
		dataRateAndTotalUsage.put("totalUsage", totalUsage);
		
		return dataRateAndTotalUsage;
	}
	
	// read periodical data rate and total usage
	private static void readDataRateFile(int[] ueIDs, double[] dataRates, double[] totalUsageArr) throws FileNotFoundException {
		// dataCollectionPeriods
		String dataCollectionPeriodFileName = "";
		int dataCollectionPeriodsInt = (int)dataCollectionPeriods;
		if(dataCollectionPeriodsInt < 10) {
			dataCollectionPeriodFileName = sampleIndexStr + "cycleTimeOptimalGU_0" + dataCollectionPeriodsInt + ".csv";
		}else {
			dataCollectionPeriodFileName = sampleIndexStr + "cycleTimeOptimalGU_" + dataCollectionPeriodsInt + ".csv";
		}
		
		File file = new File(dataCollectionPeriodFileName);
		
		Scanner inputFile = new Scanner(file);
		
		// remove title
		inputFile.nextLine();
		
		while(inputFile.hasNext()) {
			String tuple = inputFile.nextLine();
			String[] tupleArr = tuple.split(",");
			
			int ueID = Integer.parseInt(tupleArr[0]);
			double totalInternetUsage = Double.parseDouble(tupleArr[1]);
			double dataRate = Double.parseDouble(tupleArr[2]);
			
			for(int i = 0; i < ueIDs.length; i++) {
				int currentUeID = ueIDs[i];
				
				if(currentUeID == ueID) {
					dataRates[i] = dataRate;
					totalUsageArr[i] = totalInternetUsage;
					
					break;
				}
			}
		}
		
		inputFile.close();
		
	}
	
	// Devices report current status, only dynamic devices report current status
	private static void reportCurrentStatus(double currentTime){
		for(int i = 0; i < UeArr.size(); i++) {
			UserEquipment ue = UeArr.get(i);
			ue.reportCurrentStatus(currentTime);
		}
	}
	
	// Count functions
	private static void countTotalSignals(ArrayList<UserEquipment> devicesArr) {
		double totalSignals = 0;
		
		for(int i = 0; i < devicesArr.size(); i++) {
			UserEquipment device = devicesArr.get(i);
			
			double signals = device.getProducedSignals();
			totalSignals += signals;
			System.out.printf("Signals : %3.0f\n", signals);
			System.out.printf("Session successful rate : %5.0f", device.getSuccessfulRate() * 100);
			System.out.println("%");
		}
		System.out.printf("Total signals : %5.0f\n", totalSignals);
		
	}
	
	// set the total data allowance with normal distribution
	private static double dataAllowanceSetting(int numOfDevices) {
		/*
		// store the data allowance of data plans in an array
		double[] dataAllowanceArr = {500, 1024, 3072, 5120};
		
		int numberOfDataPlans = dataAllowanceArr.length;
		double normalDistributionLowerBound = -4;
		double normalDistributionUpperBound = 4;
		
		double intervalRange = (normalDistributionUpperBound - normalDistributionLowerBound) / numberOfDataPlans;
		
		int[] dataPlanUserCountArr = new int[numberOfDataPlans];
		Random rand = new Random();
		for(int i = 0; i < numOfDevices; i++) {
			double randNum = rand.nextGaussian();
			
			int interval = (int)(Math.floor((randNum - normalDistributionLowerBound) / intervalRange));
			dataPlanUserCountArr[interval] += 1;
		}
		
		// aggregate the total allowance
		double totalDataAllowance = 0;
		for(int i = 0; i < dataAllowanceArr.length; i++) {
			totalDataAllowance += dataAllowanceArr[i] * dataPlanUserCountArr[i];
		}
		
		// change monthly allowance to weekly allowance
		return totalDataAllowance / 4;
		*/
		
		// change MB to KB
//		return Math.ceil(numOfDevices * 500 / 4) * 1024 * 0.8;
		return 500 * 1024;
	}
	

	// configure the reservation schemes
    private static OnlineChargingSystem fixedScheme(double totalDataAllowance) throws FileNotFoundException{
    	// hyper-parameters
        System.out.print("Enter the default GU(MB) for fixed scheme : ");
        defaultGU = input.nextDouble();
        System.out.println("");
        
        // read default GU value from a txt file
        /*
        File defaultGU_file = new File("defaultGU.txt");
        Scanner defaultGU_input = new Scanner(defaultGU_file);
        
        defaultGU = defaultGU_input.nextDouble();
        defaultGU_input.close();
        */
        
        // write next default GU in a txt file
        /*
        double nextDefaultGU = defaultGU + 50;
        File nextDefaultGU_file = new File("defaultGU.txt");
        PrintWriter pw = new PrintWriter(nextDefaultGU_file);
        pw.println(nextDefaultGU);
        pw.close();
        */
        
        // configure online charging function for fixed scheme
        OnlineChargingFunctionFixedScheme OCF = new OnlineChargingFunctionFixedScheme(defaultGU, chargingPeriods);
        // configure account balance management function
        AccountBalanceManagementFunction ABMF = new AccountBalanceManagementFunction(totalDataAllowance);
        
        // create an instance for online charging system
        OnlineChargingSystem OCS = new OnlineChargingSystem(UeArr, OCF, ABMF, "FS");
        
        return OCS;
    }

    private static OnlineChargingSystem multiplicativeScheme(double totalDataAllowance) {
    	// hyper-parameters
        System.out.print("Enter default GU(MB) for multiplicative scheme : ");
        defaultGU = input.nextDouble();
        System.out.println("");
        
        System.out.print("Enter C : ");
        double c = input.nextDouble();
        System.out.println("");
        
        // configure online charging function for multiplicative scheme
        OnlineChargingFunctionMultiplicativeScheme OCF = new OnlineChargingFunctionMultiplicativeScheme(defaultGU, c, chargingPeriods);
        // configure account balance management function
        AccountBalanceManagementFunction ABMF = new AccountBalanceManagementFunction(totalDataAllowance);
        
        OnlineChargingSystem OCS = new OnlineChargingSystem(UeArr, OCF, ABMF, "MS");
        
        return OCS;
        
    }
    
    private static OnlineChargingSystem inventoryBasedReservationScheme(double totalDataAllowance) {
    	// view the test data and record the total usage and periodical data usage
    	
    	
    	// hyper-parameters
//    	System.out.print("Enter the charging period(days) : ");
//    	chargingPeriods = input.nextDouble();
//    	System.out.println("");
    	
//		System.out.print("Enter default GU(MB) for inventory-based reservation scheme : ");
//		defaultGU = input.nextDouble();
    	// in IRS the default GU of each device is obtained from total demand and default GU calculation
		defaultGU = 10;
//		defaultGU = defaultGU * 130;
//		System.out.println("");
		
//		System.out.print("Enter the signals of each report");
//		double signalsPerReport = input.nextDouble();
//		System.out.println("");
		
		double signalsPerReport = 1;
		
//		System.out.print("Enter the signals of each order");
//		double signalsPerOrder = input.nextDouble();
//		System.out.println("");
		
		double signalsPerOrder = 1;
		
		// configure online charging function for IRS
		OnlineChargingFunctionInventoryBasedReservationScheme OCF = new OnlineChargingFunctionInventoryBasedReservationScheme(defaultGU, chargingPeriods, signalsPerReport, signalsPerOrder);
		// configure account balance management function
		AccountBalanceManagementFunction ABMF = new AccountBalanceManagementFunction(totalDataAllowance);
    	
    	
		return new OnlineChargingSystem(UeArr, OCF, ABMF, "IRS");
	}
    
    private static boolean chargingProcessContinue(double remainingDataAllowance, double timePeriod) {
    	boolean chargingProcessContinue = true;
    	
    	/*
    	if(remainingDataAllowance <= 0 && timePeriod > chargingPeriods * 24 && getSumOfRemainingGuInUEs() <= 0) {
    		chargingProcessContinue = false;
    	}
    	*/
    	
    	if(timePeriod > chargingPeriods) {
    		chargingProcessContinue = false;
    	}
    	
    	
    	return chargingProcessContinue;
    }
    
    private static double getSumOfRemainingGuInUEs() {
    	double sumOfRemainingGU = 0;
    	
    	for(int i = 0; i < UeArr.size(); i++) {
    		UserEquipment currentUE = UeArr.get(i);
    		double remainingGU = currentUE.getCurrentGU();
    		sumOfRemainingGU += remainingGU;
    	}
    	
    	return sumOfRemainingGU;
    }
    
    private static void writeExperimentResult(int numOfDevices, String reservationScheme, double totalDataAllowance, double defaultGU) throws FileNotFoundException {
    	Date date = new Date();
    	String dateStr = date.toString();
    	
    	String[] dateStrArr = dateStr.split(" ");
    	String timeStr = dateStrArr[3].replaceAll(":", "_");
    	String filename = dateStrArr[5] + dateStrArr[1] + dateStrArr[2] + "_" + timeStr;
    	
    	String logFilename = filename + ".txt";
    	
    	PrintWriter pw = new PrintWriter(logFilename);
    	
    	double totalSignals = 0;
    	
    	// print experiment configuration
    	pw.printf("Number of devices : %d\n", numOfDevices);
    	pw.printf("Reservation scheme : %s\n", reservationScheme);
    	pw.printf("Monthly data allowance : %3.0f\n", totalDataAllowance);
    	pw.printf("Default GU : %5.0f\n", defaultGU);
    	pw.println("Data collection period : " + dataCollectionPeriods);
    	pw.println();
		
    	double totalSucdcessfulRate = 0;
    	double totalInteractionTimes = 0;
		for(int i = 0; i < UeArr.size(); i++) {
			UserEquipment device = UeArr.get(i);
			
			double signals = device.getProducedSignals();
			double interactionTimes = device.interaction;
			totalSignals += signals;
			totalInteractionTimes += interactionTimes;
			pw.printf("UE ID : %5d ", device.getUeID());
			pw.printf("Signals : %3.0f\n", signals);
			pw.printf("Interaction times : %3.0f\n", interactionTimes);
			pw.printf("Session successful rate : %5.0f", device.getSuccessfulRate() * 100);
			pw.println("%");
			
			totalSucdcessfulRate += device.getSuccessfulRate() * 100;
		}
		pw.printf("Average successful rate : %f", (totalSucdcessfulRate / numOfDevices));
		pw.println("%");
		pw.printf("Total signals : %5.0f\n", totalSignals);
		pw.printf("Total interaction times : %5.0f\n", totalInteractionTimes);
		
		pw.close();
		
		// print short experiment result
		String shortResultName = filename + "_short.txt";
		
		PrintWriter shortPW = new PrintWriter(shortResultName);
		shortPW.printf("%f , %f , %f", totalInteractionTimes, totalSignals, (totalSucdcessfulRate / numOfDevices));
		shortPW.close();
		
	}
    
    // 傳送所有 UE 的總需求以及data rate 到 OCS
    public static void setTotalDemandAndDataRate() {
    	for(int i = 0; i < UeArr.size(); i++) {
    		UserEquipment ue = UeArr.get(i);
    		
    		int ueID = ue.getUeID();
    		double totalDemand = ue.getTotalDemand();
    		double dataRate = ue.getPeriodicalDataUsage();
    		
    		OCS.setTotalDemandAndDataRate(ueID, totalDemand, dataRate);
    	}
    }
}
