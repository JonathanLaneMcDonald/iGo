public class PlayerConfiguration {
	public int playouts;
	public int topK;

	public double noiseWeight;
	public double expansionProbability;
	public double resignationThreshold;

	public PlayerConfiguration() {

	}

	public static PlayerConfiguration PresetRandom() {
		return new PlayerConfiguration();
	}
}
