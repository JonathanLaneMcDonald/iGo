public class MultiMatchConfiguration {

	private BoardSizeDistribution bsd;
	private double komi;

	public MultiMatchConfiguration(BoardSizeDistribution bsd, double komi) {
		this.bsd = bsd;
		this.komi = komi;
	}

	public int getBoardSize() {
		return bsd.getBoardSize();
	}

	public double getKomi() {
		return komi;
	}
}
