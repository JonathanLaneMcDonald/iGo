import java.util.*;
import java.util.stream.Collectors;

public class iGo
{
	/*
	Method for submitting moves and handling consequences
		assumptions:
			1) we start with an empty board and all moves are legal

		method for move submission and update:
		1) check if the move is legal (there's a boolean array for that)
		2) if legal, place the stone on the board, resulting in a group of one or more stones for the current player
		3) floodfill the new group with the integer index of the current move, logically connecting all stones in the new group
			during the floodfill process:
				I) track board positions of stones are in the current group
				II) track board positions of liberties belonging to the current group
				III) record a set of group IDs belonging to the other player (in case we're removing a group, we'll have a list of affected groups to follow up)
		4) record who owns the newly created group and how many liberties it has by making two assignments, one in ownership and one in liberties
		5) repeat steps 3 and 4 for any adjacent belonging to the other player
		6) if a group runs out of liberties, then repeat step 3, send the group stones array to a "remove stones" method, and update bordering groups accordingly
		7) if a group has a single liberty, then visit that liberty to see if it can connect to get more liberties. if not, i think it's a suicide.
	 */

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
	Ko ko;

	int[] board;
	int[] ownership;
	int[] liberties;
	int[] legalForBlack;
	int[] legalForWhite;

	int diagnosticOutput;

	static ArrayList<ArrayList<Integer>> neighbors;

	public iGo(int side, int diagnosticLevel)
	{
		this.side = side;
		area = side*side;
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

	public iGo(int side)
	{
		this(side, 0);
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
				if(diagnosticOutput >= 1) {
					System.out.println(player == 1 ? "Black to move" : "White to move");
				}

				board[mv] = mv;
				ownership[mv] = player;
				var ffr = floodfill(mv);
				liberties[ffr.groupID] = ffr.groupLiberties.size();

				if(ffr.groupLiberties.size() == 1)
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

					if(diagnosticOutput >= 2) {
						System.out.println("These Stones Have Been Captured");
						display(ffr.groupStones);
					}

					for(int groupID : ffr.adjacentGroups)
					{
						var adj = floodfill(groupID);

						if(liberties[groupID] == 1 && liberties[groupID] < adj.groupLiberties.size())
						{
							if(diagnosticOutput >= 2) {
								System.out.println("These Adjacent Stones Are No Longer In Atari");
								display(adj.groupStones);
							}

							libertiesNeedingReview.addAll(adj.groupLiberties);
						}

						liberties[groupID] = adj.groupLiberties.size();
					}
				}
				else
				{
					if(ffr.groupLiberties.size() == 1)
					{
						if(diagnosticOutput >= 2) {
							System.out.println("These Stones Are Now In Atari");
							display(ffr.groupStones);
						}

						libertiesNeedingReview.addAll(ffr.groupLiberties);
					}

					liberties[ffr.groupID] = ffr.groupLiberties.size();
				}

				if(diagnosticOutput >= 2) {
					System.out.println("Step 2: Handle Adjacent Groups");
					displayFloodfillResult(ffr);
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
			if(numberOfStonesRemoved.size() == 1)
				registerKo(numberOfStonesRemoved.stream().findFirst().get(), -player);

			if(diagnosticOutput >= 3) {
				displayGroupsAndOwnership();
			}
			return true;
		}
		else
		{
			if(diagnosticOutput >= 1) {
				System.out.println("Failed on the following move");
				System.out.println(player == 1 ? "Black to move" : "White to move");
				display(new HashSet<>(Collections.singletonList(mv)));
			}

			return false;
		}
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

		if(area <= board[mv] || ownership[board[mv]] == 0) {
			//displayGroupsAndOwnership();
			System.out.println("Floodfill finds that the current cell has no owner");

			return ffr;
		}

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

			if(diagnosticOutput >= 4)
				displayGroupsAndOwnership();

			visited[position] = 1;
		}

		return ffr;
	}

	public void displayGroupsAndOwnership()
	{
		System.out.println("Board State");
		display(board);
		System.out.println("Ownership State");
		display(ownership);
		System.out.println("Group State");
		display(new HashSet<>());
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

	public void display(int[] array)
	{
		for(int r = 0; r < side; r++)
		{
			for(int c = 0; c < side; c++)
			{
				var position = side*r+c;

				if(position == array[position])				System.out.print("(");
				else										System.out.print(" ");

				System.out.printf("%1$4s",array[position]);

				if(position == array[position])				System.out.print(")");
				else										System.out.print(" ");
			}
			System.out.print("\n");
		}
		System.out.print("\n");
	}
}
