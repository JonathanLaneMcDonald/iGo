public class MonteCarloTreeSearch {
	private class Node
	{
		/*
		I'll start with the generic selector from the MC-RAVE paper
		action = argmax(Q(s,a) + c*sqrt(log(N(s))/N(s,a))
		 */

		public int area;
		public int player;

		public iGo game;

		public int[] sensibilityAtChild;

		public int[] victoriesAtChild;
		public int[] simulationsAtChild;
		public Node[] children;

		public Node parent;

		public Node(int area)
		{
			// initialize board and player and set parent node to null
			this.area = area;
			player = 1; // black to start -- always

			game = new iGo(area);

			sensibilityAtChild = game.getSensibleMovesForPlayerAsArray(player);

			victoriesAtChild = new int[area];
			simulationsAtChild = new int[area];

			children = new Node[area];

			parent = null; // because this is the root node
		}

		public Node(Node parent, int move, int player)
		{
			// copy-construct node and make a move
		}

	}
}
