import java.util.*;
import java.util.stream.Collectors;

public class MonteCarloTreeSearch {
	private class Tuple<K, V>
	{
		public K first;
		public V second;

		public Tuple(K first, V second)
		{
			this.first = first;
			this.second = second;
		}
	}

	private class Node
	{
		/*
		I'll start with the generic selector from the MC-RAVE paper
		action = argmax(Q(s,a) + c*sqrt(log(N(s))/N(s,a))
		 */

		public int move;
		public int player;

		public int totalVictories;
		public int totalSimulations;

		public Node parent;
		public ArrayList<Node> children;

		iGo cachedGame;

		public Node(Node parent, int move, int player)
		{
			this.move = move;
			this.player = player;

			totalVictories = 0;
			totalSimulations = 0;

			this.parent = parent;
			children = new ArrayList<>();

			cachedGame = null;
		}
	}

	static double c;

	int side;
	int area;
	int actionSpace;

	Node root;
	Node currentRoot;
	int nodesExpanded;
	int simulationErrors;

	Random random;

	public MonteCarloTreeSearch(int side)
	{
		c = 1;

		this.side = side;
		area = side*side;
		actionSpace = area+1;

		random = new Random();

		nodesExpanded = 0;
		simulationErrors = 0;

		// i know magic numbers are bad, but for now it's important for player to be set to -1 here for black to start the game
		root = new Node(null, -1, -1);
		expandAllForNode(root);
		currentRoot = root;
	}

	public int getNextPlayerToMove()
	{
		return -currentRoot.player;
	}

	public int getStrongestMove()
	{
		int mostSimulations = -1;
		int mostSimulatedMove = -1;
		for(var child : currentRoot.children) {
			if (mostSimulations < child.totalSimulations) {
				mostSimulations = child.totalSimulations;
				mostSimulatedMove = child.move;
			}
		}
		return mostSimulatedMove;
	}

	// boolean of whether the move could be completed
	public boolean doMove(int move)
	{
		for(var node : currentRoot.children)
			if(node.move == move) {
				currentRoot = node;
				return true;
			}
		return false;
	}

	public void displayBoard()
	{
		var game = prepareGameAtNode(currentRoot);
		game.display(new HashSet<>(Collections.singleton(currentRoot.move)));
	}

	public void displayPositionStrength()
	{
		var strength = new int[actionSpace];
		for (var child : currentRoot.children)
			strength[child.move] = child.totalSimulations;

		System.out.println("Total Simulations");
		iGo.display(strength, side, new HashSet<>(Collections.singleton(getStrongestMove())));
		System.out.println("Policy Values");
		iGo.display(getPolicy(1000), side, new HashSet<>(Collections.singleton(getStrongestMove())));
		System.out.println("Utility Values");
		iGo.display(getValue(1000), side, new HashSet<>(Collections.singleton(getStrongestMove())));
	}

	public int[] getPolicy(int base)
	{
		int totalSimulations = 0;
		for(var child : currentRoot.children)
			totalSimulations += child.totalSimulations;

		var policy = new int[actionSpace];
		for(var child : currentRoot.children)
			policy[child.move] = (base * child.totalSimulations / (1+totalSimulations));

		return policy;
	}

	public int[] getValue(int base)
	{
		var value = new int[actionSpace];
		for(var child : currentRoot.children)
			value[child.move] = (base * child.totalVictories / (1+child.totalSimulations));

		return value;
	}

	public void simulate(int numberOfSimulations, double probabilityOfExpansion)
	{
		for(int i = 0; i < numberOfSimulations; i++) {
			simulate(probabilityOfExpansion);
			if(i != 0 && i % 10000 == 0) {
				System.out.println(i + " simulations performed; " + nodesExpanded + " nodes expanded; "+simulationErrors+" simulation errors; "+getStrongestMove()+" is the strongest move");
				if(i % 100000 == 0) {
					displayBoard();
					//displayPositionStrength();
				}
			}
		}
		System.out.println(nodesExpanded + " nodes expanded; "+simulationErrors+" simulation errors; "+getStrongestMove()+" is the strongest move");
	}

	private void simulate(double probabilityOfExpansion)
	{
		Node leaf;

		if(random.nextDouble() < probabilityOfExpansion)
			leaf = recurseToAndExpandLeaf(currentRoot);
		else
			leaf = recurseToLeaf(currentRoot);

		var victor = randomRollout(prepareGameAtNode(leaf), leaf.player);

		backupGameOutcome(leaf, victor);
	}

	private void backupGameOutcome(Node walker, int victor)
	{
		while(walker.parent != null) {
			walker.totalSimulations++;
			if (victor == walker.player)
				walker.totalVictories++;
			walker = walker.parent;
		}
	}

	private iGo prepareGameAtNode(Node currentNode)
	{
		var game = new iGo(side);
		for(var move : lineageToMoveset(currentNode)) {
			if (!game.placeStone(move.first, move.second)) {
				simulationErrors ++;
			}
		}
		return game;
	}

	private Node recurseToAndExpandLeaf(Node currentNode)
	{
		var leaf = recurseToLeaf(currentNode);
		expandAllForNode(leaf);
		return leaf;
	}

	private Node recurseToLeaf(Node searchNode)
	{
		var nextSearchNode = getMostVisitableChildNode(searchNode);
		if(nextSearchNode.isPresent())
			return recurseToLeaf(nextSearchNode.get());
		else
			return searchNode;
	}

	private void expandAllForNode(Node leaf)
	{
		var game = prepareGameAtNode(leaf);

		game.auditSensibleMovesRecommendationsForPlayer(-leaf.player);
		for(var move : game.getSensibleMovesForPlayer(-leaf.player))
			leaf.children.add(new Node(leaf, move, -leaf.player));

		// and, finally, add the "pass" move which is always legal
		leaf.children.add(new Node(leaf, area, -leaf.player));

		nodesExpanded ++;
	}

	private Optional<Node> getMostVisitableChildNode(Node currentNode)
	{
		int totalSimulations = currentNode.children.stream().map(child -> child.totalSimulations).reduce(0, Integer::sum);

		Node bestNode = null;
		double bestScore = -1;
		for(var child : currentNode.children) {
			double score = (double) child.totalVictories / (1 + child.totalSimulations) + c * Math.pow(Math.log(totalSimulations) / (1 + child.totalSimulations), 0.5);
			if (bestScore < score) {
				bestScore = score;
				bestNode = child;
			}
		}

		if(bestNode == null)
			return Optional.empty();
		else
			return Optional.of(bestNode);
	}

	public Stack<Integer> getMovesetFromLineage()
	{
		return lineageToMoveset(currentRoot).stream().map(p -> p.first).collect(Collectors.toCollection(Stack::new));
	}

	private Stack<Tuple<Integer, Integer>> lineageToMoveset(Node walker)
	{
		var moveset = new Stack<Tuple<Integer,Integer>>();
		while(walker.parent != null)
		{
			moveset.add(new Tuple<Integer, Integer>(walker.move, walker.player));
			walker = walker.parent;
		}
		return moveset;
	}

	private int randomRollout(iGo game, int nextToPlay)
	{
		int player = nextToPlay;
		int consecutivePasses = 0;
		int movesPlayed = 0;
		while(consecutivePasses < 2 && movesPlayed < game.getArea()*3)
		{
			var sensibleMoves = game.getSensibleMovesForPlayer(player);
			if(sensibleMoves.isEmpty()) {
				consecutivePasses++;
				game.placeStone(-1, player);
			}
			else {
				consecutivePasses = 0;
				game.placeStone(sensibleMoves.get(random.nextInt(sensibleMoves.size())), player);
			}
			player = -player;
			movesPlayed ++;
		}

		// i don't want to incentivise these long, indecisive games, so if the game goes long, nobody wins
		// i'm basing this on the fact that games that go until neither player has any more sensible moves usually last ~area*1.2 on average
		if(movesPlayed > game.getArea()*2)
			return 0;

		if(game.getSimpleTerminalScore() > 0)
			return 1;
		else
			return -1;
	}
}
