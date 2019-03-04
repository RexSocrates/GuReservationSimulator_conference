import java.util.ArrayList;

public class SinglePeriodAllocatedGUs {
	private double period;
	private ArrayList<Double> allocatedGUs;
	
	public SinglePeriodAllocatedGUs(double period) {
		this.period = period;
		this.allocatedGUs = new ArrayList<Double>();
	}
	
	public SinglePeriodAllocatedGUs(double period, ArrayList<Double> allocatedGUs) {
		this.period = period;
		this.allocatedGUs = allocatedGUs;
	}
	
	public void addAllocatedGU(double allocatedGU) {
		this.allocatedGUs.add(allocatedGU);
	}
	
	public double getSumOfGUs() {
		double totalAllocatedGUs = 0;
		for(int i = 0; i < this.allocatedGUs.size(); i++) {
			totalAllocatedGUs += this.allocatedGUs.get(i);
		}
		
		return totalAllocatedGUs;
	}
}
