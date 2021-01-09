import java.util.*;

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
		public int sensibility;

		public int totalVictories;
		public int totalSimulations;

		public Node parent;
		public Node[] children;

		public Node(Node parent, int move, int player, int sensibility)
		{
			this.move = move;
			this.player = player;
			this.sensibility = sensibility;

			// these initial values are mainly there as normalizers and to avoid divide by zero
			totalVictories = 1;
			totalSimulations = 1;

			this.parent = parent;
			children = null;
		}
	}

	static double c;

	int side;
	int area;

	iGo canonicalGame;

	Node root;
	Node currentRoot;
	int nodesExpanded;

	Random random;

	public MonteCarloTreeSearch(int side)
	{
		c = 1;

		this.side = side;
		area = side*side;

		canonicalGame = new iGo(side);

		random = new Random();

		nodesExpanded = 0;

		// i know magic numbers are bad, but for now it's important for player to be set to -1 here for black to start the game
		root = new Node(null, -1, -1, -1);
		expandAllForNode(root);
		currentRoot = root;
	}

	// put in a weighted random version of this eventually
	public int getStrongestMove()
	{
		int mostSimulatedMove = -1;
		if(currentRoot.children != null)
		{
			int mostSimulations = 0;
			for(var child : currentRoot.children)
			{
				if(child.sensibility == 1) {
					if (mostSimulations < child.totalSimulations) {
						mostSimulations = child.totalSimulations;
						mostSimulatedMove = child.move;
					}
				}
			}
		}
		return mostSimulatedMove;
	}

	public void doMove(int move)
	{
		if(currentRoot.children != null)
			currentRoot = currentRoot.children[move];
	}

	public void displayBoard()
	{
		var game = prepareGameAtNode(currentRoot);
		game.display(new HashSet<>(Collections.singleton(currentRoot.move)));
	}

	public void displayPositionStrength()
	{
		if(currentRoot.children != null) {
			var strength = new int[area];
			for (var child : currentRoot.children)
				strength[child.move] = (int)(100*child.totalVictories / (double) child.totalSimulations);
			iGo.display(strength, side);
		}
		else
			System.out.println("Simulations have not been performed at this node");
	}

	public void simulate(int numberOfSimulations, double probabilityOfExpansion)
	{
		for(int i = 0; i < numberOfSimulations; i++) {
			simulate(probabilityOfExpansion);
			if(i != 0 && i % 1000 == 0) {
				System.out.println(i + " simulations performed; " + nodesExpanded + " nodes expanded; "+getStrongestMove()+" is the strongest move");
				if(i % 10000 == 0)
					displayPositionStrength();
			}
		}
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
		while(walker.parent != null)
		{
			walker.totalSimulations ++;
			if(victor == walker.player)
				walker.totalVictories ++;
			walker = walker.parent;
		}
	}

	private iGo prepareGameAtNode(Node currentNode)
	{
		var game = new iGo(side);
		for(var move : lineageToMoveset(currentNode))
			game.placeStone(move.first, move.second);
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
		leaf.children = new Node[area];

		var game = prepareGameAtNode(leaf);

		var sensibilityOfMoves = game.getSensibleMovesForPlayerAsArray(-leaf.player);
		for(int move = 0; move < area; move++)
			leaf.children[move] = new Node(leaf, move, -leaf.player, sensibilityOfMoves[move]);

		nodesExpanded ++;
	}

	private Optional<Node> getMostVisitableChildNode(Node currentNode)
	{
		if(currentNode.children == null)
			return Optional.empty();

		int totalSimulations = 0;
		for(var child : currentNode.children)
			if(child.sensibility == 1)
				totalSimulations += child.totalSimulations;

		int bestMove = -1;
		double bestScore = 0;
		for(var child : currentNode.children)
		{
			if(child.sensibility == 1) {
				double score = (double) child.totalVictories / child.totalSimulations + c * Math.pow(Math.log(totalSimulations) / child.totalSimulations, 0.5);
				if (bestScore < score) {
					bestScore = score;
					bestMove = child.move;
				}
			}
		}

		if(bestMove != -1)
			return Optional.of(currentNode.children[bestMove]);
		else
			return Optional.empty();
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
		while(consecutivePasses < 2)
		{
			var sensibleMoves = game.getSensibleMovesForPlayer(player);
			if(sensibleMoves.isEmpty())
				consecutivePasses ++;
			else
			{
				consecutivePasses = 0;
				var mv = sensibleMoves.get(random.nextInt(sensibleMoves.size()));
				game.placeStone(mv, player);
				player = -player;
			}
		}

		if(game.getSimpleTerminalScore() > 0)
			return 1;
		else
			return -1;
	}
}
