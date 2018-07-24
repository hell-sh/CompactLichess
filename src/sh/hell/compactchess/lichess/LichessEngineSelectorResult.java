package sh.hell.compactchess.lichess;

import sh.hell.compactchess.engine.Engine;
import sh.hell.compactchess.engine.EngineBuilder;

public class LichessEngineSelectorResult
{
	public String abortReason = null;
	public String startMessage;
	public String engineName;
	public Engine engine;
	public EngineBuilder fallbackEngine;
}
