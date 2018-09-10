import org.junit.Test;
import sh.hell.compactchess.exceptions.ChessException;
import sh.hell.compactchess.game.EndReason;
import sh.hell.compactchess.game.Game;
import sh.hell.compactchess.game.GameStatus;
import sh.hell.compactchess.lichess.LichessAPI;

import java.io.IOException;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

public class Tests
{
	private static void visualize(Game game)
	{
		System.out.println(game.toString(true));
	}

	@Test(timeout = 10000L)
	public void importGame() throws ChessException, IOException
	{
		System.out.println("Import Game\n");
		final LichessAPI lichessAPI = new LichessAPI();
		final Game game = Game.fromPGN("[Event \"Grandmasters-Young Masters\"]\n" + "[Site \"Sochi URS\"]\n" + "[Date \"1970.10.??\"]\n" + "[EventDate \"?\"]\n" + "[Round \"9\"]\n" + "[Result \"1-0\"]\n" + "[White \"Viktor Kupreichik\"]\n" + "[Black \"Mikhail Tal\"]\n" + "[ECO \"B57\"]\n" + "[WhiteElo \"?\"]\n" + "[BlackElo \"?\"]\n" + "[PlyCount \"61\"]\n" + "\n" + "1.e4 c5 2.Nf3 d6 3.d4 cxd4 4.Nxd4 Nc6 5.Nc3 Nf6 6.Bc4 Qb6\n" + "7.Nb3 e6 8.Be3 Qc7 9.f4 a6 10.Bd3 b5 11.a3 Be7 12.Qf3 Bb7\n" + "13.O-O Rc8 14.Rae1 O-O 15.Qh3 b4 16.Nd5 exd5 17.exd5 Nb8\n" + "18.Bd4 g6 19.Rf3 Bxd5 20.Rfe3 Bd8 21.Qh4 Nbd7 22.Qh6 Qb7\n" + "23.Rg3 Nc5 24.Nxc5 dxc5 25.f5 cxd4 26.fxg6 fxg6 27.Bxg6 Kh8\n" + "28.Qxf8+ Ng8 29.Bf5 Rb8 30.Re8 Qf7 31.Rh3 1-0").get(0);
		String url = lichessAPI.importGame(game);
		assertNotNull(url);
		System.out.println(url);
		assertTrue(url.startsWith("https://lichess.org/"));
	}

	@Test(timeout = 10000L)
	public void exportGame() throws IOException, ChessException
	{
		System.out.println("Export Game\n");
		final LichessAPI lichessAPI = new LichessAPI();
		final Game game = lichessAPI.exportGames(new String[]{"XdQ8329y"}).get(0);
		visualize(game);
		System.out.println(game.toPGN());
		assertEquals(81, game.plyCount);
		assertEquals(80, game.moves.size());
		assertEquals(GameStatus.BLACK_WINS, game.status);
		assertEquals(EndReason.CHECKMATE, game.endReason);
	}
}
