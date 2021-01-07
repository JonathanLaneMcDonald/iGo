import java.util.*;
import java.util.stream.Collectors;

public class iGo
{
	private static class MoveDiagnostics
	{
		public boolean moveIsLegal;
		public boolean moveRequiresCaptures;

		public MoveDiagnostics(boolean isLegal, boolean requiresCaptures)
		{
			moveIsLegal = isLegal;
			moveRequiresCaptures = requiresCaptures;
		}
	}

	int side;
	int area;
	int ko;

	int[] board;

	int[] legalMovesBlack;
	int[] legalMovesWhite;

	ArrayList<ArrayList<Integer>> neighbors;

	public iGo(int side)
	{
		this.side = side;
		area = side*side;
		ko = -1;

		board = new int[area];

		setupLegalMoves();
		setupNeighbors();
	}

	public iGo()
	{
		this(9);
	}

	public iGo(iGo other)
	{
		side = other.getSide();
		area = other.getArea();
		ko = other.getKo();
		board = other.getBoard().clone();
		legalMovesBlack = other.getLegalMovesBlack().clone();
		legalMovesWhite = other.getLegalMovesWhite().clone();
		neighbors = other.getNeighbors();// how does this work in java? i'm fine with a ref, but is this a ref?
	}

	public int getSide()
	{
		return side;
	}

	public int getArea()
	{
		return area;
	}

	public int getKo()
	{
		return ko;
	}

	public int[] getBoard()
	{
		return board;
	}

	public int[] getLegalMovesBlack()
	{
		return legalMovesBlack;
	}

	public int[] getLegalMovesWhite()
	{
		return legalMovesWhite;
	}

	private void setupLegalMoves()
	{
		legalMovesBlack = new int[area];
		legalMovesWhite = new int[area];

		for(int mv = 0; mv < area; mv++)
		{
			legalMovesBlack[mv] = 1;
			legalMovesWhite[mv] = 1;
		}
	}

	public ArrayList<ArrayList<Integer>> getNeighbors()
	{
		return neighbors;
	}

	private ArrayList<Integer> getNeighborsAtPosition(int board_index)
	{
		return neighbors.get(board_index);
	}

	private void setupNeighbors()
	{
		neighbors = new ArrayList<>();

		for(int r = 0; r < side; r++)
		{
			for(int c = 0; c < side; c++)
			{
				var localNeighbors = new ArrayList<Integer>();

				if(0 <= r-1)
					localNeighbors.add(side*(r-1) + c);

				if(r+1 < side)
					localNeighbors.add(side*(r+1) + c);

				if(0 <= c-1)
					localNeighbors.add(side*r + (c-1));

				if(c+1 < side)
					localNeighbors.add(side*r + (c+1));

				neighbors.add(localNeighbors);
			}
		}
	}

	public Optional<iGo> copyOnPlaceStone(int position, int player, boolean updateLegalMoves)
	{
		var newState = new iGo(this);

		if(newState.placeStone(position, player, updateLegalMoves))
			return Optional.of(newState);

		return Optional.empty();
	}

	public boolean placeStone(int board_index, int player)
	{
		return placeStone(board_index, player, true);
	}

	public boolean placeStone(int board_index, int player, boolean updateLegalMoves)
	{
		if(board_index == -1)
		{
			cancelKo();
			return true;
		}

		if(board[board_index] != 0)
			return false;

		board[board_index] = player;

		var moveDiag = moveIsLegalAndRequiresCaptures(board_index, player);

		if(moveDiag.moveIsLegal)
		{
			Set<Integer> enemyStonesCaptured = new HashSet<>();

			for(int position : getNeighborsAtPosition(board_index))
				if(board[position] == -player)
					if (hasLiberties(position).isEmpty())
						enemyStonesCaptured.addAll(removeGroup(position, -player));

			if(enemyStonesCaptured.size() == 1 && ko == board_index)
				board[enemyStonesCaptured.stream().findFirst().get()] = -player;
			else if(!moveDiag.moveRequiresCaptures || !enemyStonesCaptured.isEmpty())
			{
				cancelKo();
				if(enemyStonesCaptured.size() == 1 && identifyGroupMembers(enemyStonesCaptured.stream().findFirst().get(), player).size() == 1)
					ko = enemyStonesCaptured.stream().findFirst().get();

				if(updateLegalMoves)
					updateLegalMoves(player, board_index, enemyStonesCaptured);

				return true;
			}
		}

		board[board_index] = 0;

		return false;
	}

	private void updatePositionLegality(int position)
	{
		// now check once for each player
		assert board[position] == 0;

		board[position] = 1;
		var diag = moveIsLegalAndRequiresCaptures(position, 1);
		if(diag.moveIsLegal)
			legalMovesBlack[position] = 1;
		else
			legalMovesBlack[position] = 0;

		board[position] = -1;
		diag = moveIsLegalAndRequiresCaptures(position, -1);
		if(diag.moveIsLegal)
			legalMovesWhite[position] = 1;
		else
			legalMovesWhite[position] = 0;

		board[position] = 0;
	}

	private void cancelKo()
	{
		if(ko == -1)
			return;

		updatePositionLegality(ko);
		ko = -1;
	}

	private void updateLegalMoves(int currentPlayer, int movePosition, Set<Integer> enemyStonesCaptured)
	{
		// the current move position is no longer a legal move
		if(movePosition != -1)
		{
			legalMovesBlack[movePosition] = 0;
			legalMovesWhite[movePosition] = 0;
		}

		// then check every single liberty and every cleared space to see if they're safe for both players
		Set<Integer> positionsToCheck = new HashSet<>();

		// check liberties near the current move
		positionsToCheck.addAll(hasLiberties(movePosition));

		// add the removed stones, themselves
		positionsToCheck.addAll(enemyStonesCaptured);

		// add anything that might be adjacent to the group I just joined
		for(int stone : identifyAdjacentStones(Collections.singleton(movePosition)))
			positionsToCheck.addAll(hasLiberties(stone));

		// finally, check liberties of groups that bordered any captured groups
		for(int stone : identifyAdjacentStones(enemyStonesCaptured))
			positionsToCheck.addAll(hasLiberties(stone));

		//displayBoard(movePosition);
		//System.out.println("Below is a map of all positions we're updating for the legality thing");
		//display(board, positionsToCheck);

		// manually check those positions
		for(int mv : positionsToCheck)
			updatePositionLegality(mv);

		// do something about the ko situation
		if(ko != -1)
		{
			if(currentPlayer == 1)
			{
				legalMovesBlack[ko] = 1;
				legalMovesWhite[ko] = 0;
			}
			else
			{
				legalMovesBlack[ko] = 0;
				legalMovesWhite[ko] = 1;
			}
		}
	}

	private MoveDiagnostics moveIsLegalAndRequiresCaptures(int board_index, int player)
	{
		if(getNeighborsAtPosition(board_index).stream().anyMatch(pos -> board[pos] == 0))
			return new MoveDiagnostics(true, false);

		if(!hasLiberties(board_index).isEmpty())
			return new MoveDiagnostics(true, false);

		if(getNeighborsAtPosition(board_index).stream().anyMatch(pos -> board[pos] == -player && hasLiberties(pos).isEmpty()))
			return new MoveDiagnostics(true, true);

		return new MoveDiagnostics(false, false);
	}

	private Set<Integer> identifyAdjacentStones(Set<Integer> stones)
	{
		Set<Integer> adjacentStones = new HashSet<>();
		for(int stone : stones)
			adjacentStones.addAll(getNeighborsAtPosition(stone).stream().filter(pos -> board[pos] != 0).collect(Collectors.toList()));
		return adjacentStones;
	}

	private Set<Integer> removeGroup(int position, int player)
	{
		var groupMembers = identifyGroupMembers(position, player);
		for(int member : groupMembers)
			board[member] = 0;
		return groupMembers;
	}

	private Set<Integer> identifyGroupMembers(int position, int player)
	{
		int[] visited = new int[area];

		Set<Integer> groupMembers = new HashSet<>();

		Stack<Integer> stack = new Stack<>();
		stack.add(position);
		stack.addAll(getNeighborsAtPosition(position).stream().filter(pos -> board[pos] == player).collect(Collectors.toList()));

		while(!stack.empty())
		{
			var newPos = stack.pop();

			stack.addAll(getNeighborsAtPosition(newPos).stream().filter(pos -> board[pos] == player && visited[pos] == 0).collect(Collectors.toList()));

			visited[newPos] = 1;

			groupMembers.add(newPos);
		}
		return groupMembers;
	}

	private Set<Integer> hasLiberties(int position)
	{
		int player = board[position];
		int[] visited = new int[area];

		Stack<Integer> stack = new Stack<>();
		stack.add(position);

		Set<Integer> liberties = new HashSet<>();

		while(!stack.empty())
		{
			var newPos = stack.pop();

			liberties.addAll(getNeighborsAtPosition(newPos).stream().filter(pos -> board[pos] == 0).collect(Collectors.toList()));

			stack.addAll(getNeighborsAtPosition(newPos).stream().filter(pos -> board[pos] == player && visited[pos] == 0).collect(Collectors.toList()));

			visited[newPos] = 1;
		}

		return liberties;
	}

	public void displayBoard()	{
		display(board, new HashSet<>(Collections.singletonList(-1)));
	}

	public void displayBoard(int position)	{
		display(board, new HashSet<>(Collections.singletonList(position)));
	}

	public void display(int[] array, Set<Integer> highlightPositions)
	{
		for(int r = 0; r < side; r++)
		{
			for(int c = 0; c < side; c++)
			{
				var position = side*r+c;

				if(highlightPositions.contains(position))	System.out.print("(");
				else										System.out.print(" ");

				if(array[side*r+c] == 1)					System.out.print("X");
				if(array[side*r+c] == -1)					System.out.print("O");
				if(array[side*r+c] == 0)					System.out.print("-");

				if(highlightPositions.contains(position))	System.out.print(")");
				else										System.out.print(" ");
			}
			System.out.print("\n");
		}
		System.out.print("\n");
	}

	public void display(int[] a, int[] b, Set<Integer> highlightPositions)
	{
		for(int r = 0; r < side; r++)
		{
			for(int c = 0; c < side; c++)
			{
				var position = side*r+c;

				if(highlightPositions.contains(position))	System.out.print("(");
				else										System.out.print(" ");

				if(a[side*r+c] == 1 && b[side*r+c] == 1)	System.out.print("*");
				else if(a[side*r+c] == 1)					System.out.print("X");
				else if(b[side*r+c] == 1)					System.out.print("O");
				else										System.out.print("-");

				if(highlightPositions.contains(position))	System.out.print(")");
				else										System.out.print(" ");
			}
			System.out.print("\n");
		}
		System.out.print("\n");
	}

	public int auditLegalMoves(String message)
	{
		var discrepancies = new HashSet<Integer>();

		var baselineLegalForBlack = legalMovesForPlayerBaseline(1);
		discrepancies.addAll(compareLegalMovesLists(legalMovesBlack, baselineLegalForBlack));

		var baselineLegalForWhite = legalMovesForPlayerBaseline(-1);
		discrepancies.addAll(compareLegalMovesLists(legalMovesWhite, baselineLegalForWhite));

		if(!discrepancies.isEmpty())
		{
			System.out.println(message);
			System.out.println("Board State");
			display(board, discrepancies);
			System.out.println("Legal Moves");
			display(baselineLegalForBlack, baselineLegalForWhite, discrepancies);
			System.out.println("************************************************************");
		}

		return discrepancies.size();
	}

	private Set<Integer> compareLegalMovesLists(int[] a, int[] b)
	{
		var positionsWithErrors = new HashSet<Integer>();
		for(int i = 0; i < a.length; i++)
			if (a[i] != b[i])
				positionsWithErrors.add(i);
		return positionsWithErrors;
	}

	public int[] legalMovesForPlayerBaseline(int player)
	{
		var legalMoves = new int[area];

		for(int mv = 0; mv < area; mv++)
		{
			if(copyOnPlaceStone(mv, player, false).isPresent())
			{
				legalMoves[mv] = 1;
			}
			else
			{
				legalMoves[mv] = 0;
			}
		}

		return legalMoves;
	}
}
