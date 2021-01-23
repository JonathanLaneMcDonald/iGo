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

	public static PlayerConfiguration PresetVanillaMCTS() {
		var config = new PlayerConfiguration();
		config.playouts = 50;
		config.topK = 3;
		config.noiseWeight = 0.25;
		config.expansionProbability = 1.0;
		config.resignationThreshold = 0.0;
		return config;
	}

	public static PlayerConfiguration PresetSeriousVanillaMCTS() {
		var config = new PlayerConfiguration();
		config.playouts = 400;
		config.topK = 1;
		config.noiseWeight = 0.0;
		config.expansionProbability = 1.0;
		config.resignationThreshold = 0.0;
		return config;
	}
}
