import java.util.Optional;

public class StrategySupplier {

	public enum StrategyType {
		Random,
		VanillaMCTS,
		ModelEnabledMCTS,
		Human
	}

	StrategyType selectedStrategy;
	MatchConfiguration matchConfig;
	int playouts;

	public StrategySupplier(StrategyType strategy, MatchConfiguration config, int mctsPlayouts) {
		selectedStrategy = strategy;
		matchConfig = config;
		playouts = mctsPlayouts;
	}

	public Optional<Strategy> getStrategy() {
		switch(selectedStrategy) {
			case Random:
				return Optional.of(new RandomStrategy(matchConfig.boardSize, matchConfig.komi));
			case VanillaMCTS:
				return Optional.of(new VanillaTreeSearchStrategy(matchConfig.boardSize, matchConfig.komi, playouts));
			case ModelEnabledMCTS:
				System.out.println("We don't have model enabled mcts yet");
				return Optional.empty();
			case Human:
				System.out.println("It doesn't make sense to have human here ;)");
				return Optional.empty();
		}
		System.out.println("You didn't actually select an sort of Strategy");
		return Optional.empty();
	}
}
