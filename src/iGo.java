import java.util.*;
import java.util.stream.Collectors;

public class iGo
{

	private class Ko
	{
		public int koPosition;
		public int restrictedPlayer;

		public Ko(int koPosition, int restrictedPlayer)
		{
			this.koPosition = koPosition;
			this.restrictedPlayer = restrictedPlayer;
		}
	}

	int side;
	int area;
	double komi;
	Ko ko;

	int[] board;
	int[] ownership;
	int[] liberties;
	int[] legalForBlack;
	int[] legalForWhite;

	int diagnosticOutput;

	static ArrayList<ArrayList<Integer>> neighbors;

	public iGo(int side)
	{
		this(side, 0);
	}

	public iGo(int side, int diagnosticLevel)
	{
		this.side = side;
		area = side*side;
		komi = 6.5;
		ko = new Ko(-1, -1);

		board = new int[area];
		for(int stone = 0; stone < area; stone++)
			board[stone] = area;

		ownership = new int[area];
		liberties = new int[area];

		diagnosticOutput = diagnosticLevel;

		setupLegalMoves();
		setupNeighbors();
	}

	public iGo(iGo other)
	{
		side = other.getSide();
		area = other.getArea();
		komi = other.getKomi();
		ko = other.getKo();

		board = other.getBoard();
		ownership = other.getOwnership();
		liberties = other.getLiberties();
		legalForBlack = other.getLegalForBlack();
		legalForWhite = other.getLegalForWhite();

		diagnosticOutput = other.getDiagnosticOutput();

		// the neighbor status is static, so it should be shared arleady
	}

	public int getSide()
	{
		return side;
	}

	public int getArea()
	{
		return area;
	}

	public double getKomi()
	{
		return komi;
	}

	public Ko getKo()
	{
		return new Ko(ko.koPosition, ko.restrictedPlayer);
	}

	public int[] getBoard()
	{
		return board.clone();
	}

	public int[] getOwnership()
	{
		return ownership.clone();
	}

	public int[] getLiberties()
	{
		return liberties.clone();
	}

	public int[] getLegalForBlack()
	{
		return legalForBlack.clone();
	}

	public int[] getLegalForWhite()
	{
		return legalForWhite.clone();
	}

	public int getDiagnosticOutput()
	{
		return diagnosticOutput;
	}

	private void setupLegalMoves()
	{
		legalForBlack = new int[area];
		legalForWhite = new int[area];

		for(int i = 0; i < area; i++)
		{
			legalForBlack[i] = 1;
			legalForWhite[i] = 1;
		}
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

	public boolean placeStone(int mv, int player)
	{
		if(mv == -1)
		{
			unregisterKo();
			return true;
		}
		else if(mv < 0 || area <= mv)
			return false;

		if((player == 1 && legalForBlack[mv] == 1) || (player == -1 && legalForWhite[mv] == 1))
		{
			var libertiesNeedingReview = new HashSet<Integer>();

			/*
			first, handle my group. This involves:
			1) transferring ownership of the current position to myself
			2) updating the groupID to the current move position and recording information about ownership and liberties
			 */
			{
				board[mv] = mv;
				ownership[mv] = player;
				var ffr = floodfill(mv);

				libertiesNeedingReview.addAll(ffr.groupLiberties);

				if(diagnosticOutput >= 1) {
					display(new HashSet<>(Collections.singletonList(mv)));
				}

				if(diagnosticOutput >= 2) {
					System.out.println("Step 1: Handle Current Player's Group");
					displayFloodfillResult(ffr);
				}
			}

			/*
			second, handle any enemy groups that might be nearby
			1) look through a list of neighbors to identify any nearby enemy groups
			2) update their liberty counts in the table
			3) if any of them has been reduced to zero liberties, remove the group by unregistering it from the ownership array
				4) if a group is removed, update the liberty counts of its adjacent groups
				5) if updating a group's liberty count from 1 to a larger number, check to see if that liberty's legality needs updating
			 */
			var numberOfStonesRemoved = new HashSet<Integer>();
			var neighboringStonesToInvestigate = getNeighborsAtPosition(mv).stream().filter(p -> board[p] < area && ownership[board[p]] == -player).collect(Collectors.toCollection(HashSet::new));
			var neighboringGroupsToInvestigate = neighboringStonesToInvestigate.stream().map(p -> board[p]).collect(Collectors.toCollection(HashSet::new));
			for(int pos : neighboringGroupsToInvestigate)
			{
				var ffr = floodfill(pos);
				if(ffr.groupLiberties.isEmpty())
				{
					numberOfStonesRemoved.addAll(ffr.groupStones);

					for(int removedStone : ffr.groupStones)
						board[removedStone] = area;

					libertiesNeedingReview.addAll(ffr.groupStones);

					for(int groupID : ffr.adjacentGroups)
					{
						var adj = floodfill(groupID);

						libertiesNeedingReview.addAll(adj.groupLiberties);
					}
				}
				else
				{
					libertiesNeedingReview.addAll(ffr.groupLiberties);

					if(diagnosticOutput >= 2) {
						if(ffr.groupLiberties.size() == 1) {
							System.out.println("These Stones Are Now In Atari");
							display(ffr.groupStones);
						}
					}
				}
			}

			/*
			third, once the groups and liberties are updated, maybe I can use them to update the legal moves
			 */

			// if we just put a stone there, then it's not a legal move anymore
			legalForBlack[mv] = 0;
			legalForWhite[mv] = 0;

			for(int liberty : libertiesNeedingReview)
				determineLegalityAtPosition(liberty);

			// finally, don't forget to deal with the ko
			unregisterKo();
			if(numberOfStonesRemoved.size() == 1 && floodfill(mv).groupStones.size() == 1)
				registerKo(numberOfStonesRemoved.stream().findFirst().get(), -player);

			if(diagnosticOutput >= 3) {
				displayGroupsAndOwnership();
			}

			return true;
		}
		else
			return false;
	}

	private void registerKo(int koPosition, int restrictedPlayer)
	{
		ko = new Ko(koPosition, restrictedPlayer);
		determineLegalityAtPosition(koPosition);
	}

	private void unregisterKo()
	{
		if(ko.koPosition != -1) {
			var oldKoPosition = ko.koPosition;
			ko = new Ko(-1, -1);
			determineLegalityAtPosition(oldKoPosition);
		}
	}

	private boolean violatingKo(int position, int player)
	{
		return position == ko.koPosition && player == ko.restrictedPlayer;
	}

	public Set<Integer> boardContainsZombieGroups()
	{
		var questionableStones = new HashSet<Integer>();
		for(int i = 0; i < area; i++)
		{
			if(board[i] < area && ownership[board[i]] != 0) {
				var result = floodfill(i);
				if (result.groupLiberties.isEmpty())
					questionableStones.add(i);
			}
		}
		return questionableStones;
	}

	public List<Integer> getMovesLegalForBothPlayers()
	{
		var legalForBoth = new ArrayList<Integer>();
		for(int i = 0; i < area; i++)
			if(legalForBlack[i] == 1 && legalForWhite[i] == 1)
				legalForBoth.add(i);
		return legalForBoth;
	}

	public List<Integer> getMovesLegalForPlayer(int player)
	{
		var legalForPlayer = new ArrayList<Integer>();
		for(int i = 0; i < area; i++)
			if(player == 1 && legalForBlack[i] == 1)
				legalForPlayer.add(i);
			else if(player == -1 && legalForWhite[i] == 1)
				legalForPlayer.add(i);
		return legalForPlayer;
	}

	public int[] getSensibleMovesForPlayerAsArray(int player)
	{
		var sensibleMoves = new int[area];
		for(var mv : getSensibleMovesForPlayer(player))
			sensibleMoves[mv] = 1;
		return sensibleMoves;
	}

	public List<Integer> getSensibleMovesForPlayer(int player)
	{
		// i define "sensible" as 1) legal for me && 2) legal for opponent or suicide for opponent
		var sensibleForPlayer = new ArrayList<Integer>();
		for(int position = 0; position < area; position ++) {
			if (player == 1) {
				if (legalForBlack[position] == 1 && (legalForWhite[position] == 1 || moveIsSuicideForPlayer(position, -1))) {
					sensibleForPlayer.add(position);
				}
			}
			else if(player == -1) {
				if(legalForWhite[position] == 1 && (legalForBlack[position] == 1 || moveIsSuicideForPlayer(position, 1))){
					sensibleForPlayer.add(position);
				}
			}
		}
		return sensibleForPlayer;
	}

	private boolean moveIsSuicideForPlayer(int position, int player)
	{
		// if i have groups neighboring tihs position and none of them has more than one liberty
		var myNeighboringStones = getNeighborsAtPosition(position).stream().filter(p -> board[p] < area && ownership[board[p]] == player).collect(Collectors.toList());
		var myNeighboringGroups = myNeighboringStones.stream().map(p -> board[p]).filter(g -> 1 < liberties[g]).collect(Collectors.toSet());
		return !myNeighboringStones.isEmpty() && myNeighboringGroups.isEmpty();
	}

	private boolean positionIsAvailableToBothPlayers(int position)
	{
		return legalForBlack[position] == 1 && legalForWhite[position] == 1;
	}

	private void determineLegalityAtPosition(int position)
	{
		// first, if this space is open and there are spaces nearby, then this position is legal for both players
		if(getNeighborsAtPosition(position).stream().anyMatch(p -> board[p] == area || ownership[board[p]] == 0))
		{
			legalForBlack[position] = 1;
			legalForWhite[position] = 1;
		}
		else
		{
			legalForBlack[position] = positionIsLegalForPlayer(position, 1) ? 1 : 0;
			legalForWhite[position] = positionIsLegalForPlayer(position, -1) ? 1 : 0;
		}
	}

	private boolean positionIsLegalForPlayer(int position, int player)
	{
		return !violatingKo(position, player) && getNeighborsAtPosition(position).stream().anyMatch(p ->
				(board[p] < area && ownership[board[p]] == player && floodfill(p).groupLiberties.size() >= 2) ||// i can join a friendly group
				(board[p] < area && ownership[board[p]] == -player && floodfill(p).groupLiberties.size() == 1));// i can capture an enemy group
	}

	private class FloodfillResult
	{
		public int groupID;
		public Set<Integer> groupStones;
		public Set<Integer> groupLiberties;
		public Set<Integer> adjacentGroups;

		public FloodfillResult(int groupID)
		{
			this.groupID = groupID;
			groupStones = new HashSet<>();
			groupLiberties = new HashSet<>();
			adjacentGroups = new HashSet<>();
		}
	}

	private FloodfillResult floodfill(int mv)
	{
		var ffr = new FloodfillResult(mv);

		var toVisit = new Stack<Integer>();
		toVisit.push(mv);

		var visited = new int[area];

		while(!toVisit.empty())
		{
			var position = toVisit.pop();

			if(area <= board[position] || ownership[board[position]] == 0)
			{
				/* if it borders my group and nobody owns it, then it's a liberty */
				ffr.groupLiberties.add(position);
			}
			else if(ownership[board[position]] == ownership[mv])
			{
				/*	if it's part of my group, reassign the groupID to the move position - it's simple, quick, and unique */
				board[position] = mv;
				ffr.groupStones.add(position);
				toVisit.addAll(getNeighborsAtPosition(position).stream().filter(p -> visited[p] == 0).collect(Collectors.toList()));
			}
			else
			{
				/* if the other player owns it, then it's an adjacent group and may be affected by things that happen to this group,
				* so i'll keep track of the value at the board position which is, necessarily, a position within the group ;) */
				ffr.adjacentGroups.add(board[position]);
			}

			visited[position] = 1;
		}

		// this must be updated here because floodfill changes the groupID
		liberties[board[mv]] = ffr.groupLiberties.size();

		return ffr;
	}

	public double getSimpleTerminalScore()
	{
		/* getting the simple terminal score means you've satisfied the following conditions:
			1) you've sampled "sensible" moves, meaning you'll do every move except filling your own eyes, and
			2) you've played until there have been two consecutive passes, meaning...
			The two players have exhausted all moves, all groups are connected, and there are no shared liberties, so
			It should be possible to count the score by checking a very simple condition, as follows
		 */
		double score = 0;
		for(int position = 0; position < area; position++)
			if((board[position] < area && ownership[board[position]] == 1) || legalForBlack[position] == 1)
				score += 1;
			else
				score -= 1;
		return score - komi;
	}

	private int[] countLibertiesAtPosition()
	{
		var libertiesAtPosition = new int[area];
		for(int position = 0; position < area; position ++)
		{
			if(board[position] == area)
				libertiesAtPosition[position] = 0;
			else
				libertiesAtPosition[position] = liberties[board[position]];
		}
		return libertiesAtPosition;
	}

	private Set<Integer> manuallyVerifyGlobalLiberties()
	{
		var discrepantPositions = new HashSet<Integer>();
		for(int i = 0; i < area; i++)
		{
			if(board[i] < area)
			{
				var libertiesAtPosition = liberties[board[i]];// record this number ahead of time because floodfill changes the groupID
				var ffr = floodfill(i);
				if(ffr.groupLiberties.size() != libertiesAtPosition) {
					System.out.println("Group "+ffr.groupID+" "+ffr.groupLiberties.size()+"!="+libertiesAtPosition);
					discrepantPositions.addAll(ffr.groupStones);
				}
			}
		}
		return discrepantPositions;
	}

	public void displayGroupsAndOwnership()
	{
		var messedUpLibertyCounts = manuallyVerifyGlobalLiberties();
		System.out.println("Board State");
		display(board);
		System.out.println("Ownership State");
		display(ownership);
		System.out.println("Liberties State");
		display(countLibertiesAtPosition());
		System.out.println("Group State");
		display(new HashSet<>());
		System.out.println("Groups With Discrepant Liberty Counts");
		display(messedUpLibertyCounts);
		System.out.println("Liberties State");
		displayLiberties(new HashSet<>());

		if(!messedUpLibertyCounts.isEmpty())
			System.out.println("A discrepancy with liberties was detected");

	}

	public void displayFloodfillResult(FloodfillResult ffr)
	{
		System.out.println("Group Stones");
		display(ffr.groupStones);

		System.out.println("Group Liberties");
		display(ffr.groupLiberties);

		var adjacentGroupStones = new HashSet<Integer>();
		for(int groupID : ffr.adjacentGroups)
			adjacentGroupStones.addAll(floodfill(groupID).groupStones);

		System.out.println("Adjacent Group Stones");
		display(adjacentGroupStones);
	}

	public void display(Set<Integer> highlightPositions)
	{
		for(int r = 0; r < side; r++)
		{
			for(int c = 0; c < side; c++)
			{
				var position = side*r+c;

				if(highlightPositions.contains(position))	System.out.print("(");
				else										System.out.print(" ");

				if(board[side*r+c] < area && ownership[board[side*r+c]] == 1)		System.out.print("X");
				if(board[side*r+c] < area && ownership[board[side*r+c]] == -1)		System.out.print("O");
				if(area <= board[side*r+c] || ownership[board[side*r+c]] == 0)		System.out.print("-");

				if(highlightPositions.contains(position))	System.out.print(")");
				else										System.out.print(" ");
			}
			System.out.print("\n");
		}
		System.out.print("\n");
	}

	public void displayLiberties(Set<Integer> highlightPositions)
	{
		for(int r = 0; r < side; r++)
		{
			for(int c = 0; c < side; c++)
			{
				var position = side*r+c;

				if(highlightPositions.contains(position))	System.out.print("(");
				else										System.out.print(" ");

				if(legalForBlack[position] == 1 && legalForWhite[position] == 1)	System.out.print("*");
				else if(legalForBlack[position] == 1)								System.out.print("X");
				else if(legalForWhite[position] == 1)								System.out.print("O");
				else																System.out.print("-");

				if(highlightPositions.contains(position))	System.out.print(")");
				else										System.out.print(" ");
			}
			System.out.print("\n");
		}
		System.out.print("\n");
	}

	public void display(int[] array)
	{
		display(array, side, new HashSet<>());
	}

	public static void display(int[] array, int sidelength, Set<Integer> highlightPositions)
	{
		for(int r = 0; r < sidelength; r++)
		{
			for(int c = 0; c < sidelength; c++)
			{
				var position = sidelength*r+c;

				if(highlightPositions.contains(position))	System.out.print("(");
				else										System.out.print(" ");

				System.out.printf("%1$5s",array[position]);

				if(highlightPositions.contains(position))	System.out.print(")");
				else										System.out.print(" ");
			}
			System.out.print("\n");
		}
		System.out.print("\n");
	}
}
