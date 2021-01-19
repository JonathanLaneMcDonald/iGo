public class Move
{
	public int index;

	public Move(int r, int c, int side)
	{
		index = side*r + c;
	}

	public Move()
	{
		index = -1;
	}
}
