import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HistoricalGame {

	private int boardSize;
	private double komi;
	private List<Integer> moves;

	public HistoricalGame() {
	}

	public HistoricalGame(int boardSize, double komi, List<Integer> moves) {
		this.boardSize = boardSize;
		this.komi = komi;
		this.moves = moves;
	}

	public static Optional<HistoricalGame> parseFromString(String potentialHistoricalGame) {
		var components = potentialHistoricalGame.split(" ");

		if(components.length == 3) {
			try {
				int boardSize = Integer.parseInt(components[0]);
				double komi = Double.parseDouble(components[1]);
				List<Integer> moves = SGFParser.stringToMoves(components[2], boardSize);
				return Optional.of(new HistoricalGame(boardSize, komi, moves));
			}
			catch(NumberFormatException e) {
				System.out.println("NumberFormatException in HistoricalGame::parseFromString()");
			}
		}

		return Optional.empty();
	}
}
