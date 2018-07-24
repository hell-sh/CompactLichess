package sh.hell.compactchess.lichess;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Scanner;

public class LichessBot extends Thread
{
	final LichessEngineSelector engineSelector;
	final ArrayList<LichessBotGame> activeGames = new ArrayList<>();
	final LichessAPI lichessAPI;
	private Thread thread;

	LichessBot(LichessAPI lichessAPI, LichessEngineSelector engineSelector)
	{
		this.lichessAPI = lichessAPI;
		this.engineSelector = engineSelector;
	}


	public int countGames()
	{
		synchronized(activeGames)
		{
			return activeGames.size();
		}
	}

	public int countPlayerGames()
	{
		int count = 0;
		synchronized(activeGames)
		{
			for(LichessBotGame game : activeGames)
			{
				if(!game.opponent.startsWith("BOT "))
				{
					count++;
				}
			}
		}
		return count;
	}

	public int countBotGames()
	{
		int count = 0;
		synchronized(activeGames)
		{
			for(LichessBotGame game : activeGames)
			{
				if(game.opponent.startsWith("BOT "))
				{
					count++;
				}
			}
		}
		return count;
	}

	@Override
	public void run()
	{
		try
		{
			InputStream is = lichessAPI.sendRequest("GET", "/api/stream/event");
			BufferedInputStream bis = new BufferedInputStream(is);
			Scanner sc = new Scanner(bis).useDelimiter("\\n");
			while(!this.thread.isInterrupted())
			{
				try
				{
					String line = sc.next();
					if(!line.equals(""))
					{
						JsonObject event = Json.parse(line).asObject();
						switch(event.get("type").asString())
						{
							case "challenge":
								JsonObject challenge = event.get("challenge").asObject();
								lichessAPI.sendRequest("POST", "/challenge/" + challenge.get("id").asString() + "/accept");
								break;
							case "gameStart":
								synchronized(activeGames)
								{
									activeGames.add(new LichessBotGame(this, event.get("game").asObject().get("id").asString()));
								}
								Thread.sleep(3000);
								break;
							default:
								System.out.println(line);
						}
					}
				}
				catch(FileNotFoundException ignored)
				{
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
}
