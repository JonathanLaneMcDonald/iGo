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
