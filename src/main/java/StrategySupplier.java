import java.util.Optional;

public class StrategySupplier {

	public enum StrategyType {
		Random,
		VanillaMCTS,
		ModelEnabledMCTS,
		Human
	}

	StrategyType selectedStrategy;

	public StrategySupplier(StrategyType strategy) {
		selectedStrategy = strategy;
	}

	public Optional<Strategy> getStrategy(int boardSize, double komi, int playouts) {
		switch(selectedStrategy) {
			case Random:
				return Optional.of(new RandomStrategy(boardSize, komi));
			case VanillaMCTS:
				return Optional.of(new VanillaTreeSearchStrategy(boardSize, komi, playouts));
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
