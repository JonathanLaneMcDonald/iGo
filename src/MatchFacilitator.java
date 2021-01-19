import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

public class MatchFacilitator {

	Strategy blackStrategy;
	Strategy whiteStrategy;

	public MatchFacilitator(Strategy forBlack, Strategy forWhite)
	{
		blackStrategy = forBlack;
		whiteStrategy = forWhite;
	}

	public void initializeGame(int boardSize, double komi)
	{
		blackStrategy.initializeGame(boardSize, komi);
		whiteStrategy.initializeGame(boardSize, komi);
	}

	public int facilitateGame(GameConfiguration gameConfig)
	{
		initializeGame(gameConfig.boardSize, gameConfig.komi);

		var game = new iGo(gameConfig.boardSize, gameConfig.komi);
		int nextToMove = 1;
		int consecutivePasses = 0;
		boolean playerResigns = false;
		var moves = new ArrayList<Integer>();
		while(consecutivePasses < 2 && moves.size() < gameConfig.maxMoves && !playerResigns) {

			Optional<Integer> nextMove;
			if (nextToMove == 1) {
				nextMove = blackStrategy.getNextMove(1);
			}
			else {
				nextMove = whiteStrategy.getNextMove(-1);
			}

			if (nextMove.isEmpty()) {
				playerResigns = true;
			}
			else {
				if (nextMove.get() == gameConfig.boardArea) {
					consecutivePasses++;
				}
				else {
					consecutivePasses = 0;
				}

				game.placeStone(nextMove.get(), nextToMove);
				//game.display(new HashSet<>(Collections.singleton(nextMove.get())));

				moves.add(nextMove.get());
				blackStrategy.applyMoveForPlayer(nextMove.get(), nextToMove);
				whiteStrategy.applyMoveForPlayer(nextMove.get(), nextToMove);

				nextToMove = -nextToMove;
			}
		}

		//return gameConfig.boardSize + " " + gameConfig.komi + " " + SGFParser.movesToSGF(moves, gameConfig.boardSize);

		if(game.getSimpleTerminalScore() < 0)
			return -1;
		else
			return 1;
	}
}
