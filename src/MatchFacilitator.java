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

}
