package csmp.service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.arnx.jsonic.JSON;

/**
 * BCDicebotと連携してダイスロール結果を取得する.
 * @author kgmas
 *
 */
public class BcDiceApiService extends BaseService {

	private static BcDiceApiService instance;

	private String bcDiceApiUrl = null;

	private Map<String, String> systemNamesMap = null;

	private Map<Long, String> guildSystemInfo = new ConcurrentHashMap<>();

	public synchronized static BcDiceApiService getInstance() {
		if (instance == null) {
			instance = new BcDiceApiService();
		}
		return instance;
	}

	private BcDiceApiService() {
		if (bcDiceApiUrl == null) {
			bcDiceApiUrl = System.getenv("BCDICEBOT_API_URL");
			bcDiceApiUrl = bcDiceApiUrl.endsWith("/") ? bcDiceApiUrl : bcDiceApiUrl + "/";
		}
	}

	/**
	 * サーバ単位のシステム名を取得する.
	 * @return システム名
	 */
	public String getGuildSystem(long guildId) {
		String system = guildSystemInfo.get(guildId);
		return system;
	}

	/**
	 * サーバ単位のシステム名を設定する.
	 * @param guildId サーバID
	 * @param system システム名
	 */
	public void putGuildSystem(long guildId, String system) {
		guildSystemInfo.put(guildId, system);
	}

	/**
	 *
	 * @param text
	 * @param guildId
	 * @return
	 */
	public String rollDice(String text, long guildId) {
		String system = getGuildSystem(guildId);
		if (system == null) {
			system = "DiceBot";
		}

		String path = "v1/diceroll?";
		path += "system=" + system;
		try {
			path += "&command=" + URLEncoder.encode(text, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		};

		String result = get(bcDiceApiUrl + path);
		if (result == null) {
			return null;
		}
		Map<String, Object> map = JSON.decode(result);
		return system + map.get("result");
	}

	public synchronized Map<String, String> getSystemNames() {
		if (systemNamesMap == null) {
			String path = "v1/names";
			String result = get(bcDiceApiUrl + path);

			if (result != null) {
				Map<String, Object> map = JSON.decode(result);
				List<Map<String, String>> list = (List<Map<String, String>>)map.get("names");
				Map<String, String> namesMap = new HashMap<>();
				for (Map<String, String> entry : list) {
					namesMap.put(entry.get("system"), entry.get("name"));
				}
				systemNamesMap = namesMap;
			}
		}

		return systemNamesMap;
	}

	public synchronized String getSystemInfo(String system) {
		String path = "v1/systeminfo?system=";
		try {
			path += URLEncoder.encode(system, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		};

		String result = get(bcDiceApiUrl + path);

		if (result != null) {
			Map<String, Object> map = JSON.decode(result);
			Map<String, String> systeminfo = (Map<String, String>)map.get("systeminfo");
			if (systeminfo != null) {
				return (String)systeminfo.get("info");
			}
		}

		return null;
	}

}
