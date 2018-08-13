package sh.hell.compactchess.lichess;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import sh.hell.compactchess.exceptions.ChessException;
import sh.hell.compactchess.game.Game;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;

public class LichessAPI
{
	final String baseUrl;
	private final String token;
	private JsonObject profile = null;

	public LichessAPI()
	{
		this("https://lichess.org", null);
	}

	public LichessAPI(String token)
	{
		this("https://lichess.org", token);
	}

	LichessAPI(String baseURL, String token)
	{
		this.baseUrl = baseURL;
		this.token = token;
	}

	public InputStream sendRequest(String method, String endpoint) throws IOException
	{
		HttpsURLConnection con = (HttpsURLConnection) new URL(baseUrl + endpoint).openConnection();
		con.setRequestMethod(method);
		if(token != null)
		{
			con.setRequestProperty("Authorization", "Bearer " + token);
		}
		con.setRequestProperty("Accept", "*/*");
		if(method.equals("POST"))
		{
			con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			con.setRequestProperty("Content-Length", "0");
		}
		con.setRequestProperty("User-Agent", "CompactChess (+https://hell.sh/CompactChess)");
		con.setDoInput(true);
		con.setDoOutput(method.equals("POST"));
		con.setUseCaches(false);
		try
		{
			return con.getInputStream();
		}
		catch(ConnectException e)
		{
			System.out.println("[NETW] Retrying " + method + " " + endpoint);
			return sendRequest(method, endpoint);
		}
		catch(IOException e)
		{
			if(e.getMessage().contains("Server returned HTTP response code: 504 for URL"))
			{
				System.out.println("[NETW] Retrying " + method + " " + endpoint);
				return sendRequest(method, endpoint);
			}
			else
			{
				throw e;
			}
		}
	}

	public InputStream sendPOSTRequest(String endpoint, String data) throws IOException
	{
		HttpsURLConnection con = (HttpsURLConnection) new URL(baseUrl + endpoint).openConnection();
		con.setRequestMethod("POST");
		if(token != null)
		{
			con.setRequestProperty("Authorization", "Bearer " + token);
		}
		con.setRequestProperty("Accept", "*/*");
		con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		con.setRequestProperty("Content-Length", String.valueOf(data.length()));
		con.setRequestProperty("User-Agent", "CompactChess (+https://hell.sh/CompactChess)");
		con.setDoInput(true);
		con.setDoOutput(true);
		con.setUseCaches(false);
		try
		{
			con.connect();
			OutputStreamWriter writer = new OutputStreamWriter(con.getOutputStream());
			writer.write(data);
			writer.flush();
			return con.getInputStream();
		}
		catch(ConnectException e)
		{
			System.out.println("[NETW] Retrying POST " + endpoint);
			return sendPOSTRequest(endpoint, data);
		}
		catch(IOException e)
		{
			if(e.getMessage().contains("Server returned HTTP response code: 504 for URL"))
			{
				System.out.println("[NETW] Retrying POST " + endpoint);
				return sendPOSTRequest(endpoint, data);
			}
			else
			{
				throw e;
			}
		}
	}

	public String importGame(final Game game) throws IOException, ChessException
	{
		final JsonObject jsonObject = new JsonObject();
		jsonObject.add("pgn", game.toPGN());
		final String data = jsonObject.toString();
		HttpsURLConnection con = (HttpsURLConnection) new URL(baseUrl + "/import").openConnection();
		con.setRequestMethod("POST");
		con.setRequestProperty("Accept", "*/*");
		con.setRequestProperty("Content-Type", "application/json");
		con.setRequestProperty("Content-Length", String.valueOf(data.length()));
		con.setRequestProperty("User-Agent", "CompactChess (+https://hell.sh/CompactChess)");
		con.setDoInput(true);
		con.setDoOutput(true);
		con.setUseCaches(false);
		con.setInstanceFollowRedirects(false);
		try
		{
			con.connect();
			OutputStreamWriter writer = new OutputStreamWriter(con.getOutputStream());
			writer.write(data);
			writer.flush();
			return baseUrl + con.getHeaderField("Location");
		}
		catch(ConnectException e)
		{
			System.out.println("[NETW] Retrying Import");
			return importGame(game);
		}
		catch(IOException e)
		{
			if(e.getMessage().contains("Server returned HTTP response code: 504 for URL"))
			{
				System.out.println("[NETW] Retrying Import");
				return importGame(game);
			}
			else
			{
				throw e;
			}
		}
	}

	public ArrayList<Game> exportGames(String[] ids) throws IOException, ChessException
	{
		StringBuilder ids_str = new StringBuilder();
		ids_str.append(ids[0]);
		for(int i = 1; i < ids.length; i++)
		{
			ids_str.append(",").append(ids[i]);
		}
		InputStream is = this.sendPOSTRequest("/games/export/_ids?moves=true&tags=true&clocks=true&evals=true&opening=true", ids_str.toString());
		Scanner scanner = new Scanner(is, "UTF-8");
		scanner.useDelimiter("\\A");
		String pgn = scanner.next();
		scanner.close();
		is.close();
		return Game.fromPGN(pgn);
	}

	public JsonObject getProfile() throws IOException
	{
		if(this.profile == null)
		{
			InputStream is = sendRequest("GET", "/api/account");
			Scanner scanner = new Scanner(is, "UTF-8");
			scanner.useDelimiter("\n");
			String res = scanner.next();
			scanner.close();
			is.close();
			this.profile = Json.parse(res).asObject();
		}
		return this.profile;
	}

	public boolean isBotAccount() throws IOException
	{
		return this.getProfile().getString("title", "").equals("BOT");
	}

	public boolean upgradeToBotAccount() throws IOException
	{
		if(this.isBotAccount())
		{
			return true;
		}
		this.sendPOSTRequest("https://lichess.org/api/bot/account/upgrade", "");
		this.profile = null;
		return this.isBotAccount();
	}

	public LichessBot startBot(LichessEngineSelector engineSelector) throws ChessException, IOException
	{
		if(!this.isBotAccount())
		{
			throw new ChessException("Account has to be a valid bot account.");
		}
		LichessBot bot = new LichessBot(this, engineSelector);
		bot.start();
		return bot;
	}
}
