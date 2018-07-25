package sh.hell.compactchess.lichess;

public class ListageAPI extends LichessAPI
{
	public ListageAPI()
	{
		super("https://listage.ovh", null);
	}

	public ListageAPI(String token)
	{
		super("https://listage.ovh", token);
	}
}
