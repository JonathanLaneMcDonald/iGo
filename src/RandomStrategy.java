import java.util.Random;

public class RandomStrategy implements Strategy{

	iGo game;
	Random random;

	public RandomStrategy(int boardSize, double komi) {
		random = new Random();
		initializeGame(boardSize, komi);
	}

	@Override
	public void initializeGame(int boardSize, double komi) {
		game = new iGo(boardSize, komi);
	}

	@Override
	public int getNextMove(int player) {
		var sensibleMoves = game.getSensibleMovesForPlayer(player);
		return sensibleMoves.get(random.nextInt(sensibleMoves.size()));
	}

	@Override
	public boolean applyMoveForPlayer(int move, int player) {
		return game.placeStone(move, player);
	}
}