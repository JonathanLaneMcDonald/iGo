public class GameConfiguration {

	public int boardSize = 9;
	public int boardArea = 81;
	public double komi = 6.5;
	public int maxMoves = 162;

	public GameConfiguration(int boardSize, double komi)
	{
		this.boardSize = boardSize;
		this.komi = komi;

		boardArea = boardSize*boardSize;
		maxMoves = boardArea*2;
	}
}
