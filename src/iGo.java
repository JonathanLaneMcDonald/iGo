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

	public Optional<iGo> copyOnPlaceStone(int position, int player)
	{
		var newState = new iGo(this);

		if(newState.placeStone(position, player))
			return Optional.of(newState);

		return Optional.empty();
	}

	public boolean placeStone(int board_index, int player)
	{
		if(board_index == -1)
		{
			ko = -1;
			return true;
		}

		if(board[board_index] != 0)
			return false;

		board[board_index] = player;

		var moveDiag = moveIsLegalAndRequiresCaptures(board_index, player);

		if(moveDiag.moveIsLegal)
		{
			ArrayList<Integer> enemyStonesCaptured = new ArrayList<>();

			for(int position : getNeighborsAtPosition(board_index))
				if(board[position] == -player)
					if(!hasLiberties(position, -player))
						enemyStonesCaptured.addAll(removeGroup(position, -player));

			if(enemyStonesCaptured.size() == 1 && ko == board_index)
				board[enemyStonesCaptured.get(0)] = -player;
			else if(!moveDiag.moveRequiresCaptures || !enemyStonesCaptured.isEmpty())
			{
				ko = -1;
				if(enemyStonesCaptured.size() == 1)
					ko = enemyStonesCaptured.get(0);
				return true;
			}
		}

		board[board_index] = 0;

		return false;
	}

	private MoveDiagnostics moveIsLegalAndRequiresCaptures(int board_index, int player)
	{
		if(getNeighborsAtPosition(board_index).stream().anyMatch(pos -> board[pos] == 0))
			return new MoveDiagnostics(true, false);

		if(hasLiberties(board_index, player))
			return new MoveDiagnostics(true, false);

		if(getNeighborsAtPosition(board_index).stream().anyMatch(pos -> board[pos] == -player && !hasLiberties(pos, -player)))
			return new MoveDiagnostics(true, true);

		return new MoveDiagnostics(false, false);
	}

	private ArrayList<Integer> removeGroup(int position, int player)
	{
		ArrayList<Integer> removedStones = new ArrayList<>();

		Stack<Integer> stack = new Stack<>();
		stack.add(position);
		stack.addAll(getNeighborsAtPosition(position).stream().filter(pos -> board[pos] == player).collect(Collectors.toList()));

		while(!stack.empty())
		{
			var newPos = stack.pop();

			stack.addAll(getNeighborsAtPosition(newPos).stream().filter(pos -> board[pos] == player).collect(Collectors.toList()));

			board[newPos] = 0;

			removedStones.add(newPos);
		}
		return removedStones;
	}

	private boolean hasLiberties(int position, int player)
	{
		int[] visited = new int[area];

		Stack<Integer> stack = new Stack<>();
		stack.add(position);

		while(!stack.empty())
		{
			var newPos = stack.pop();

			if(getNeighborsAtPosition(newPos).stream().anyMatch(pos -> board[pos] == 0))
				return true;

			stack.addAll(getNeighborsAtPosition(newPos).stream().filter(pos -> board[pos] == player && visited[pos] == 0).collect(Collectors.toList()));

			visited[newPos] = 1;
		}

		return false;
	}

	public void displayBoard()	{
		display(board, -1);
	}

	public void displayBoard(int position)	{
		display(board, position);
	}

	public void display(int[] array, int highlightPosition)
	{
		for(int r = 0; r < side; r++)
		{
			for(int c = 0; c < side; c++)
			{
				var position = side*r+c;

				if(position == highlightPosition)		System.out.print("(");
				else									System.out.print(" ");

				if(array[side*r+c] == 1)				System.out.print("X");
				if(array[side*r+c] == -1)				System.out.print("O");
				if(array[side*r+c] == 0)				System.out.print("-");

				if(position == highlightPosition)		System.out.print(")");
				else									System.out.print(" ");
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

	public int auditLegalMoves()
	{
		return 0;
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
			if(copyOnPlaceStone(mv, player).isPresent())
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
