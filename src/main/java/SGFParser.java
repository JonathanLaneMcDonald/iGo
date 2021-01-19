import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SGFParser
{
	private final BufferedReader _fileBuffer;
	private boolean endOfFileReached;
	private final int side;

	SGFParser(String filename, int side)
	{
		endOfFileReached = false;
		this.side = side;

		try
		{
			_fileBuffer = new BufferedReader(new FileReader(filename));
		}
		catch(FileNotFoundException e)
		{
			throw new RuntimeException("File Not Found");
		}
	}

	public boolean endOfFile()
	{
		return endOfFileReached;
	}

	private Move sgfToMove(String s)
	{
		if(s.equals("pass"))
			return new Move();
		if(s.length() != 2)
			return new Move();

		int r = s.charAt(0) - 97;
		int c = s.charAt(1) - 97;
		return new Move(r, c, side);
	}

	public static String movesToSGF(ArrayList<Integer> moves, int boardSize)
	{
		var sgfMoves = new StringBuilder("");

		for(int i = 0; i < moves.size(); i++) {
			if(moves.get(i) == boardSize*boardSize)
				sgfMoves.append("pass");
			else {
				sgfMoves.append((char) (97 + (moves.get(i) / boardSize)));
				sgfMoves.append((char) (97 + (moves.get(i) % boardSize)));
			}
			if(i < moves.size()-1)
				sgfMoves.append(',');
		}

		return sgfMoves.toString();
	}

	public List<Move> getNextMoveSet()
	{
		String line;

		try
		{
			line = _fileBuffer.readLine();
		}
		catch(IOException e)
		{
			throw new RuntimeException("File Read Error");
		}

		if(line != null)
		{
			return stringToMoves(line);
		}
		else
		{
			endOfFileReached = true;
		}

		return new ArrayList<>();
	}

	public List<Move> stringToMoves(String line)
	{
		return Arrays.stream(line.split(",")).map(this::sgfToMove).collect(Collectors.toList());
	}
}
