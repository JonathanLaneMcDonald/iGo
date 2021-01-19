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

	public void facilitateGame(GameConfiguration gameConfig)
	{
		initializeGame(gameConfig.boardSize, gameConfig.komi);

		var game = new iGo(gameConfig.boardSize, gameConfig.komi);
		int nextToMove = 1;
		int moveNumber = 0;
		int consecutivePasses = 0;
		boolean playerResigns = false;
		while(consecutivePasses < 2 && moveNumber < gameConfig.maxMoves && !playerResigns) {

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
				blackStrategy.applyMoveForPlayer(nextMove.get(), nextToMove);
				whiteStrategy.applyMoveForPlayer(nextMove.get(), nextToMove);

				moveNumber ++;
				nextToMove = -nextToMove;

				game.displayGroupsAndOwnership();
			}
		}
	}
}
