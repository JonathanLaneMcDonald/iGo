import java.util.Optional;

public interface Strategy {
	/*
	There are many examples of Strategy implementations:
		1) random strategy that uniformly samples legal moves
			i'm not actually going to build a strategy that's allowed to make "not sensible" moves.

		2) random strategy that uniformly samples sensible moves
			random strategies are important because they give us a baseline for the ELO scoring

		3) monte carlo tree search with no model support
		4) monte carlo tree search with model support
			both mcts implementations can be bounded by simulation count or by a time limit

		5) human
			for human strategy, i guess it's just a time limit and some kind of interface for collecting inputs
	 */

	void initializeGame(int boardSize, double komi);

	Optional<Integer> getNextMove(int player);

	// it's boolean so we can return if the move failed, which would indicate the boards are out of sync
	boolean applyMoveForPlayer(int move, int player);

}
