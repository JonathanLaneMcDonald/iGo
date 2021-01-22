import java.util.Optional;
import java.util.Random;

public class RandomStrategy implements Strategy{

	iGo game;
	Random random;

	public RandomStrategy(MatchConfiguration matchConfig) {
		random = new Random();
		initializeGame(matchConfig);
	}

	@Override
	public void initializeGame(MatchConfiguration matchConfig) {
		game = new iGo(matchConfig.boardSize, matchConfig.komi);
	}

	@Override
	public Optional<Integer> getNextMove(int player) {
		var sensibleMoves = game.getSensibleMovesForPlayer(player);

		if(sensibleMoves.isEmpty())
			return Optional.of(game.getArea());
		else
			return Optional.of(sensibleMoves.get(random.nextInt(sensibleMoves.size())));
	}

	@Override
	public boolean applyMoveForPlayer(int move, int player) {
		return game.placeStone(move, player);
	}
}
