package sh.hell.compactchess.lichess;

import org.junit.Test;
import sh.hell.compactchess.exceptions.ChessException;
import sh.hell.compactchess.game.EndReason;
import sh.hell.compactchess.game.Game;
import sh.hell.compactchess.game.GameStatus;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import static junit.framework.TestCase.assertEquals;

public class Tests
{
	private static void visualize(Game game) throws ChessException
	{
		System.out.println(game.toString(true));
	}

	@Test(timeout = 3000L)
	public void lichessExport() throws IOException, ChessException
	{
		System.out.println("Lichess Export\n");
		final LichessAPI lichessAPI = new LichessAPI(Files.readAllLines(Paths.get("lichess_token.txt"), Charset.forName("UTF-8")).get(0));
		final Game game = lichessAPI.exportGames(new String[]{"XdQ8329y"}).get(0);
		visualize(game);
		System.out.println(game.toPGN());
		assertEquals(81, game.plyCount);
		assertEquals(80, game.moves.size());
		assertEquals(GameStatus.BLACK_WINS, game.status);
		assertEquals(EndReason.CHECKMATE, game.endReason);
	}
}
