package sh.hell.compactchess.lichess;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import sh.hell.compactchess.engine.Engine;
import sh.hell.compactchess.engine.EngineBuilder;
import sh.hell.compactchess.exceptions.ChessException;
import sh.hell.compactchess.exceptions.InvalidMoveException;
import sh.hell.compactchess.game.Color;
import sh.hell.compactchess.game.Game;
import sh.hell.compactchess.game.Move;
import sh.hell.compactchess.game.TimeControl;
import sh.hell.compactchess.game.Variant;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class LichessBotGame extends Thread
{
	public final Game game;
	private final String id;
	private final LichessBot lichessBot;
	String opponent;
	private String engineName;
	private Engine engine;
	private EngineBuilder fallbackEngine;
	private Color botColor;
	private int lastScore = 0;
	private String endReason = null;

	LichessBotGame(LichessBot lichessBot, String id)
	{
		this.id = id;
		this.lichessBot = lichessBot;
		this.game = new Game();
		this.engineName = "Engine not yet determined.";
		this.start();
	}

	@Override
	public void run()
	{
		try
		{
			try
			{
				InputStream is = lichessBot.lichessAPI.sendRequest("GET", "/api/bot/game/stream/" + id);
				BufferedInputStream bis = new BufferedInputStream(is);
				Scanner sc = new Scanner(bis).useDelimiter("\\n");
				while(!this.isInterrupted())
				{
					String line = sc.next();
					if(!line.equals(""))
					{
						//System.out.println(lichessBot.baseUrl + "/" + id + " > " + line);
						JsonObject obj = Json.parse(line).asObject();
						JsonObject state = null;
						if(obj.get("type").asString().equals("gameFull"))
						{
							botColor = (obj.get("white").asObject().get("id").asString().equals(lichessBot.lichessAPI.getProfile().get("id").asString()) ? Color.WHITE : Color.BLACK);
							JsonObject opponentObject = obj.get(botColor == Color.WHITE ? "black" : "white").asObject();
							String opponentName = opponentObject.get("name").asString();
							String opponentTitle = opponentObject.get("title").isString() ? opponentObject.get("title").asString() : "";
							if(opponentTitle.equals(""))
							{
								opponent = opponentName;
							}
							else
							{
								opponent = opponentTitle + " " + opponentName;
							}
							game.timeControl = (obj.get("speed").asString().equals("correspondence") ? TimeControl.UNLIMITED : TimeControl.SUDDEN_DEATH);
							state = obj.get("state").asObject();
							if(game.timeControl == TimeControl.SUDDEN_DEATH)
							{
								game.whitemsecs = state.get("wtime").asLong();
								game.blackmsecs = state.get("btime").asLong();
								game.increment = state.get("winc").asLong();
								if(game.increment > 0)
								{
									game.timeControl = TimeControl.INCREMENT;
								}
							}
							game.variant = Variant.fromKey(obj.get("variant").asObject().get("key").asString());
							LichessEngineSelectorResult selectorResult = null;
							String abortReason;
							if(game.variant == null)
							{
								abortReason = "Sorry, for now I only accept Standard, King of the Hill, Three-check, Antichess, Horde, Racing Kings and From Position.";
							}
							else
							{
								selectorResult = lichessBot.engineSelector.select(lichessBot, game.variant, game.timeControl, game.whitemsecs, game.increment, obj.get("rated").asBoolean(), opponentName, opponentTitle.equals("BOT"));
								abortReason = selectorResult.abortReason;
							}
							if(selectorResult == null)
							{
								abortReason = "selectorResult is null";
							}
							if(abortReason != null)
							{
								lichessBot.lichessAPI.sendRequest("POST", "/api/bot/game/" + id + "/abort");
								endReason = "Aborted: " + abortReason;
								sendMessage("player", abortReason);
								sendMessage("spectator", abortReason);
								break;
							}
							engineName = selectorResult.engineName;
							engine = selectorResult.engine;
							fallbackEngine = selectorResult.fallbackEngine;
							game.loadFEN(obj.getString("initialFen", game.variant.startFEN));
							game.start();
							if(!state.get("moves").asString().equals("") && game.moves.size() == 0)
							{
								String[] moves = state.get("moves").asString().split(" ");
								for(String move : moves)
								{
									game.uciMove(move).commit();
								}
							}
							else
							{
								sendMessage("player", selectorResult.startMessage);
								//sendMessage("player", "I never leave games, but if you claim victory because I apparently did, you will get blocked.");
								sendMessage("spectator", "No need to wait until the end of this game to challenge me — I can play infinite simultaneous games.");
							}
							System.out.println(lichessBot.lichessAPI.baseUrl + "/" + id + " > Started playing " + opponent + ".");
							if(botColor == Color.BLACK)
							{
								final LichessBotGame ligame = this;
								new Thread(()->
								{
									try
									{
										Thread.sleep(20000);
										if(game.plyCount <= 1)
										{
											lichessBot.lichessAPI.sendRequest("POST", "/api/bot/game/" + id + "/abort");
											ligame.sendMessage("player", "Please actually play if you challenge me.");
											ligame.endReason = "Opponent played no move after 20 seconds.";
											ligame.interrupt();
										}
									}
									catch(InterruptedException ignored)
									{
									}
									catch(IOException e)
									{
										if(!e.getMessage().contains("Server returned HTTP response code: 500"))
										{
											e.printStackTrace();
										}
									}
								}, "LichessBotGame Auto Abort").start();
							}
						}
						else if(obj.get("type").asString().equals("gameState"))
						{
							state = obj;
						}
						else if(obj.get("type").asString().equals("chatLine"))
						{
							String text = obj.get("text").asString();
							if(obj.get("username").asString().equals("lichess"))
							{
								if(text.equals("Takeback sent"))
								{
									sendMessage(obj.get("room").asString(), "Sorry, I can't accept takebacks yet.");
								}
								else if(text.endsWith(" offers draw"))
								{
									sendMessage(obj.get("room").asString(), "Sorry, I can't accept draws yet.");
								}
								//else if(text.endsWith("+ 15 seconds") && obj.get("room").getAsString().equals("player"))
								//{
								//	sendMessage("player", "No need to give me extra time.");
								//}
								else if(obj.get("room").asString().equals("player"))
								{
									System.out.println(lichessBot.lichessAPI.baseUrl + "/" + id + " > " + text);
								}
							}
							else if(!obj.get("username").asString().equals(lichessBot.lichessAPI.getProfile().get("username").asString()))
							{
								System.out.println(lichessBot.lichessAPI.baseUrl + "/" + id + " > " + obj.get("room").asString() + " " + obj.get("username").asString() + ": " + text);
								if(text.startsWith("!"))
								{
									String response;
									switch(text)
									{
										case "!info":
											response = "Bot API: https://lichess.org/api#tag/Chess-Bot";
											break;
										case "!id":
										case "!identify":
										case "!name":
										case "!version":
										case "!engine":
										case "!software":
											response = engineName;
											break;
										case "!stats":
										case "!statistics":
										case "!games":
											response = "I'm currently playing " + lichessBot.countPlayerGames() + " player(s) and " + lichessBot.countBotGames() + " bot(s). " + lichessBot.countGames() + " game(s) total.";
											break;
										case "!eval":
										case "!cp":
										case "!centipawns":
											response = "After " + (obj.get("room").asString().equals("player") ? "your" : "my opponent's") + " last move, I think I have " + this.lastScore + " centipawns.";
											break;
										case "!hardware":
											response = "GTX 970 4 GB; Intel Xeon X5460, 8 Cores @ 3,16 GHz; 32 GB RAM; Windows 8.1";
											break;
										default:
											response = "Unknown command —  I only know !info, !name, !stats, !eval and !hardware.";
											break;
									}
									if(response != null)
									{
										sendMessage(obj.get("room").asString(), response);
										System.out.println(lichessBot.lichessAPI.baseUrl + "/" + id + " > " + obj.get("room").asString() + " " + lichessBot.lichessAPI.getProfile().get("username").asString() + ": " + response);
									}
								}
							}
						}
						if(state != null)
						{
							if(!state.get("moves").asString().equals(""))
							{
								String[] moves = state.get("moves").asString().split(" ");
								while(moves.length > game.moves.size())
								{
									int offset = (moves.length - game.moves.size());
									String move = moves[moves.length - offset];
									game.uciMove(move).commit();
								}
							}
							game.whitemsecs = state.get("wtime").asLong();
							game.blackmsecs = state.get("btime").asLong();
							//System.out.println(game.toString(true, true, true));
							if(game.toMove == botColor)
							{
								long mslimit = 30000;
								if(game.timeControl != TimeControl.UNLIMITED && game.plyCount > 2)
								{
									if(game.whitemsecs <= 5000)
									{
										mslimit = 1000;
									}
									else if(game.whitemsecs <= 7500)
									{
										mslimit = 3000;
									}
									else if(game.whitemsecs <= 25000)
									{
										mslimit = 5000;
									}
									else if(game.whitemsecs <= 120000)
									{
										mslimit = 10000;
									}
								}
								String bestMove = null;
								try
								{
									engine.evaluate(game, mslimit).awaitConclusion();
									//System.out.println("i Score: " + engine.score);
									//sendMessage("player", "Score: " + engine.score);
									//sendMessage("spectator", "Score: " + engine.score);
									Move bestMove_ = engine.getBestMove();
									if(bestMove_ != null)
									{
										bestMove = engine.getBestMove().toUCI();
										this.lastScore = engine.score;
									}
								}
								catch(InvalidMoveException e)
								{
									System.out.println(lichessBot.lichessAPI.baseUrl + "/" + id + " > " + e.getMessage());
								}
								catch(Exception e)
								{
									e.printStackTrace();
								}
								if(bestMove == null && fallbackEngine != null)
								{
									try
									{
										System.out.println(lichessBot.lichessAPI.baseUrl + "/" + id + " > Using fallback engine.");
										Engine fallback = fallbackEngine.build();
										fallback.evaluate(game, mslimit).awaitConclusion();
										bestMove = fallback.bestMove;
										this.lastScore = engine.score;
									}
									catch(Exception e)
									{
										e.printStackTrace();
									}
								}
								if(bestMove != null)
								{
									try
									{
										lichessBot.lichessAPI.sendRequest("POST", "/api/bot/game/" + id + "/move/" + bestMove);
										if(botColor == Color.WHITE && game.plyCount <= 2)
										{
											final LichessBotGame ligame = this;
											new Thread(()->
											{
												try
												{
													Thread.sleep(20000);
													if(game.plyCount <= 2)
													{
														lichessBot.lichessAPI.sendRequest("POST", "/api/bot/game/" + id + "/abort");
														ligame.sendMessage("player", "Please actually play if you challenge me.");
														ligame.endReason = "Opponent played no move after 20 seconds.";
														ligame.interrupt();
													}
												}
												catch(InterruptedException ignored)
												{
												}
												catch(IOException e)
												{
													if(!e.getMessage().contains("Server returned HTTP response code: 500"))
													{
														e.printStackTrace();
													}
												}
											}, "LichessBotGame Auto Abort").start();
										}
									}
									catch(IOException e)
									{
										if(!e.getMessage().contains("Server returned HTTP response code: 400") || game.variant != Variant.STANDARD)
										{
											e.printStackTrace();
										}
										endReason = "An HTTP error occurred.";
										break;
									}
								}
								else
								{
									lichessBot.lichessAPI.sendRequest("POST", "/api/bot/game/" + id + "/resign");
									endReason = "The engine found no move.";
									break;
								}
							}
						}
					}
				}
			}
			catch(NoSuchElementException ignored)
			{
				if(this.endReason == null)
				{
					this.endReason = "The stream ended.";
				}
			}
			catch(FileNotFoundException e)
			{
				this.endReason = "A HTTP 404 error occurred.";
			}
			catch(IOException e)
			{
				if(e.getMessage().contains("Server returned HTTP response code: 429"))
				{
					new Thread(()->
					{
						try
						{
							Thread.sleep(60000);
							System.out.println(lichessBot.lichessAPI.baseUrl + "/" + id + " > Waiting 60 seconds to start game...");
							new LichessBotGame(lichessBot, id);
						}
						catch(Exception e1)
						{
							e1.printStackTrace();
						}
					}, "LichessBotGame Throttler").start();
					endReason = null;
				}
				else if(!e.getMessage().contains("Server returned HTTP response code: 400"))
				{
					e.printStackTrace();
				}
			}
			if(endReason == null)
			{
				endReason = "Reason unknown.";
			}
			System.out.println(lichessBot.lichessAPI.baseUrl + "/" + id + " > Finished playing " + opponent + ": " + endReason);
			if(game.plyCount > 2)
			{
				sendMessage("player", "Good game. Well played.");
			}
			if(lichessBot.countGames() > 1)
			{
				sendMessage("spectator", "If you're watching Lichess TV and you're still here, reload to see another game.");
			}
		}
		catch(ChessException e)
		{
			if(!e.getMessage().equals(""))
			{
				e.printStackTrace();
			}
		}
		synchronized(lichessBot.activeGames)
		{
			lichessBot.activeGames.remove(this);
		}
		if(engine != null)
		{
			engine.dispose();
		}
	}

	private void sendMessage(String room, String text)
	{
		try
		{
			lichessBot.lichessAPI.sendPOSTRequest("/api/bot/game/" + id + "/chat", "room=" + room + "&text=" + URLEncoder.encode(text, "UTF-8"));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
