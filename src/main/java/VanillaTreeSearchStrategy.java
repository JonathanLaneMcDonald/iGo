import java.util.Optional;

public class VanillaTreeSearchStrategy implements Strategy{

	int searchesPerTurn;
	int topK;
	double expansionProbability;
	double resignationThreshold;

	MonteCarloTreeSearch mcts;

	public VanillaTreeSearchStrategy(int boardSize, double komi, int searchesPerTurn)
	{
		topK = 3;
		expansionProbability = 1;
		resignationThreshold = 0.00;

		this.searchesPerTurn = searchesPerTurn;

		initializeGame(boardSize, komi);
	}

	@Override
	public void initializeGame(int boardSize, double komi) {
		mcts = new MonteCarloTreeSearch(boardSize, komi, 0.25);
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
