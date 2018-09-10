package sh.hell.compactchess.lichess;

import sh.hell.compactchess.engine.Engine;
import sh.hell.compactchess.engine.EngineBuilder;
import sh.hell.compactchess.exceptions.ChessException;
import sh.hell.compactchess.game.TimeControl;
import sh.hell.compactchess.game.Variant;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

public class ExampleLichessBot
{
	public static void main(String[] args) throws ChessException, IOException
	{
		LichessEngineSelector engineSelector = new LichessEngineSelector()
		{
			@Override
			public LichessEngineSelectorResult select(LichessBot lc, Variant variant, TimeControl timeControl, long msecs, long increment, boolean rated, String opponentName, boolean botOpponent) throws IOException, InterruptedException
			{
				LichessEngineSelectorResult res = new LichessEngineSelectorResult();
				res.engineName = "Leela Chess Zero v0.7 at Network ID 190";
				String startNote = "Leela Chess Zero's moves might take a while depending on many factors.";
				final HashMap<String, String> uciOptions = new HashMap<>();
				uciOptions.put("MoveOverhead", "150");
				uciOptions.put("Move Overhead", "150");
				uciOptions.put("Ponder", "true");
				if(timeControl == TimeControl.UNLIMITED || msecs > 10800000)
				{
					res.abortReason = "Sorry, time controls above Classic are disabled for now.";
				}
				else if(variant != Variant.STANDARD)
				{
					if(rated)
					{
						res.abortReason = "Sorry, for now I only accept classical/unrated variant games.";
					}
					else
					{
						startNote = "Leela Chess Zero doesn't support variants, so you're playing against Stockfish 9.";
						res.engineName = "Multi-variant Stockfish 9";
						uciOptions.put("Threads", "2");
						res.engine = new Engine("stockfish_9_multivariant.exe", uciOptions);
					}
				}
				else if(botOpponent)
				{
					if(lc.countBotGames() > 1)
					{
						res.abortReason = "Sorry, I'm already playing a bot right now. I'm not exclusively for Bot VS Bot games.";
					}
					else
					{
						res.engine = new Engine("lczero.exe", Arrays.asList("--threads", "2"), uciOptions);
					}
				}
				else if(rated && msecs <= 15000 && increment == 0)
				{
					startNote = "Because Leela has some issues with UltraBullet, rated games are played with Stockfish 9.";
					res.engineName = "Stockfish 9";
					res.engine = new EngineBuilder("stockfish_9.exe", uciOptions).build();
				}
				else
				{
					res.engine = new Engine("lczero.exe", Arrays.asList("--threads", "1"));
				}
				res.startMessage = startNote + " Good luck. Have fun!";
				return res;
			}
		};
		new LichessAPI("XXXXXXXXXXXXXXXX").startBot(engineSelector);
	}
}
