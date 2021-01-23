import java.util.Optional;

public class VanillaTreeSearchStrategy implements Strategy{

	MatchConfiguration matchConfig;
	PlayerConfiguration playerConfig;

	MonteCarloTreeSearch mcts;

	public VanillaTreeSearchStrategy(MatchConfiguration matchConfig, PlayerConfiguration playerConfig)
	{
		this.matchConfig = matchConfig;
		this.playerConfig = playerConfig;

		initializeGame(matchConfig);
	}

	@Override
	public void initializeGame(MatchConfiguration matchConfig) {
		mcts = new MonteCarloTreeSearch(matchConfig.boardSize, matchConfig.komi, playerConfig.noiseWeight);
	}

	@Override
	public Optional<Integer> getNextMove(int player) {
		mcts.simulate(playerConfig.playouts, playerConfig.expansionProbability);

		if(mcts.nextPlayerChanceToWinExceeds(20, playerConfig.resignationThreshold)) {
			return Optional.of(mcts.getWeightedRandomStrongestMoveFromTopK(playerConfig.topK));
		}
		else {
			return Optional.empty();
		}
	}

	@Override
	public boolean applyMoveForPlayer(int move, int player) {
		return (mcts.getNextPlayerToMove() == player) && mcts.doMove(move);
	}
}
