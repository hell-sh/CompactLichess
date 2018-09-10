package sh.hell.compactchess.lichess;

import sh.hell.compactchess.engine.Engine;

@SuppressWarnings("WeakerAccess")
public class LichessEngineSelectorResult
{
	public String abortReason = null;
	public String startMessage;
	public String engineName;
	public Engine engine;
}
