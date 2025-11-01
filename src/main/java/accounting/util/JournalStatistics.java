package accounting.util;

public class JournalStatistics {
    private final double totalIn;
    private final double totalOut;
    private final long inCount;
    private final long outCount;
    private final double netChange;

    public JournalStatistics(double totalIn, double totalOut, long inCount, long outCount) {
        this.totalIn = totalIn;
        this.totalOut = totalOut;
        this.inCount = inCount;
        this.outCount = outCount;
        this.netChange = totalIn - totalOut;
    }

    public double getTotalIn() {
        return totalIn;
    }

    public double getTotalOut() {
        return totalOut;
    }

    public long getInCount() {
        return inCount;
    }

    public long getOutCount() {
        return outCount;
    }

    public double getNetChange() {
        return netChange;
    }
}
