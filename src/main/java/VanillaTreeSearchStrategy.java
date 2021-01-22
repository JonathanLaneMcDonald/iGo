import java.util.Optional;

public class VanillaTreeSearchStrategy implements Strategy{

	MatchConfiguration matchConfig;
	PlayerConfiguration playerConfig;

	int searchesPerTurn;
	int topK;
	double noiseWeight;
	double expansionProbability;
	double resignationThreshold;

	MonteCarloTreeSearch mcts;

	public VanillaTreeSearchStrategy(MatchConfiguration matchConfig, PlayerConfiguration playerConfig)
	{
		this.matchConfig = matchConfig;
		this.playerConfig = playerConfig;

		topK = 1;
		noiseWeight = 0;
		expansionProbability = 1;
		resignationThreshold = 0.00;

		this.searchesPerTurn = searchesPerTurn;

		initializeGame(matchConfig);
	}

	@Override
	public void initializeGame(MatchConfiguration matchConfig) {
		mcts = new MonteCarloTreeSearch(matchConfig.boardSize, matchConfig.komi, noiseWeight);
	}

	@Override
	public Optional<Integer> getNextMove(int player) {
		mcts.simulate(searchesPerTurn, expansionProbability);

		if(mcts.nextPlayerChanceToWinExceeds(20, resignationThreshold)) {
			return Optional.of(mcts.getWeightedRandomStrongestMoveFromTopK(topK));
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
