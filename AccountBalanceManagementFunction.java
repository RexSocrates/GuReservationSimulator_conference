
public class AccountBalanceManagementFunction {
    // 單位 : MB
    private double totalDataAllowance;
    private double remainingDataAllowance;
    
    public AccountBalanceManagementFunction(double totalDataAllowance) {
        // transfer GB to MB
        this.totalDataAllowance = totalDataAllowance;
        this.remainingDataAllowance = totalDataAllowance;
    }

    public double getTotalDataAllowance() {
        return totalDataAllowance;
    }

    public void setTotalDataAllowance(double totalDataAllowance) {
        this.totalDataAllowance = totalDataAllowance;
    }

    public double getRemainingDataAllowance() {
        return remainingDataAllowance;
    }

    public void setRemainingDataAllowance(double remainingDataAllowance) {
        this.remainingDataAllowance = remainingDataAllowance;
    }
}
