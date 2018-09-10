package sh.hell.compactchess.lichess;

import sh.hell.compactchess.exceptions.ChessException;
import sh.hell.compactchess.game.TimeControl;
import sh.hell.compactchess.game.Variant;

import java.io.IOException;

public abstract class LichessEngineSelector
{
	public abstract LichessEngineSelectorResult select(LichessBot lc, Variant variant, TimeControl timeControl, long msecs, long increment, boolean rated, String opponentName, boolean botOpponent) throws IOException, ChessException, InterruptedException;
}
