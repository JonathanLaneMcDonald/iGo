import java.util.Optional;

public class StrategySupplier {

	public enum StrategyType {
		Random,
		VanillaMCTS,
		ModelEnabledMCTS,
		Human
	}

	StrategyType selectedStrategy;
	PlayerConfiguration playerConfig;

	public StrategySupplier(PlayerConfiguration playerConfig, StrategyType strategy) {
		selectedStrategy = strategy;
		this.playerConfig = playerConfig;
	}

	public Optional<Strategy> getStrategy(MatchConfiguration matchConfig) {
		switch(selectedStrategy) {
			case Random:
				return Optional.of(new RandomStrategy(matchConfig));
			case VanillaMCTS:
				return Optional.of(new VanillaTreeSearchStrategy(matchConfig, playerConfig));
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
