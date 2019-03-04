import java.util.Arrays;
import java.util.Hashtable;

public class DailyUsage {
	// key is time(from 1 to 168, which is the last hour of a week), value is total usage in that time
	Hashtable<Integer, Double> hourlyUsage;
	
	public DailyUsage() {
		this.hourlyUsage = new Hashtable<Integer, Double>();
	}
	
	public DailyUsage(int hour, double totalUsage) {
		this.hourlyUsage = new Hashtable<Integer, Double>();
		this.hourlyUsage.put(hour, totalUsage);
	}
	
	// add hourly total usage to hash table
	public void addHourlyUsage(int hour, double hourlyUsage) {
		this.hourlyUsage.put(hour, hourlyUsage);
	}
	
	// get the total usage in a single hour
	public double getHourlyUsage(int hour) {
		double hourlyUsage = 0;
		
		if(this.hourlyUsage.containsKey(hour)) {
			hourlyUsage = (double)this.hourlyUsage.get(hour);
		}
		
		return hourlyUsage;
	}
	
	// get hours list in hash table
	public int[] getHoursList() {
		int[] hoursList = new int[this.hourlyUsage.size()];
		
		// get key set in the hash table
		Object[] keyList = this.hourlyUsage.keySet().toArray();
		
		for(int i = 0; i < keyList.length; i++) {
			hoursList[i] = (int)keyList[i];
		}
		
		// sort the array by time
		Arrays.sort(hoursList);
		
		return hoursList;
	}
}
