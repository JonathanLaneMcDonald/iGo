import java.util.Optional;
import java.util.Scanner;

public class HumanStrategy implements Strategy{

	Scanner scanner;

	public HumanStrategy() {
		scanner = new Scanner(System.in);
	}

	@Override
	public void initializeGame(MatchConfiguration matchConfig) {
		// nothing to do
	}

	@Override
	public Optional<Integer> getNextMove(int player) {
		if(player == 1)
			System.out.print("Black to move");
		else
			System.out.print("White to move");
		System.out.println(" ('pass' to pass, 'res' to resign)");

		var input = scanner.nextLine();

		try {
			return Optional.of(Integer.parseInt(input));
		}
		catch (NumberFormatException e) {
			System.out.println("You didn't enter a number, resigning.");
		}

		return Optional.empty();
	}

	@Override
	public boolean applyMoveForPlayer(int move, int player) {
		// nothing to do here
		return true;
	}
}
