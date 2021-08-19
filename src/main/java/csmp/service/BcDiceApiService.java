package csmp.service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

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

	private Map<String, Map<String, Object>> systemInfoMap = null;

	private Map<Long, String> guildSystemInfo = new ConcurrentHashMap<>();

	private Map<Long, Map<String, Map<String, Object>>> guildTableInfo = new ConcurrentHashMap<>();

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

		systemInfoMap = new ConcurrentHashMap<>();
		getSystemNames();
	}

	/**
	 * サーバ単位のテーブル情報を取得する.
	 * @param guildId
	 * @return
	 */
	public Map<String, Map<String, Object>> getTableInfo(long guildId) {
		Map<String, Map<String, Object>> tableInfo = guildTableInfo.get(guildId);
		return tableInfo;
	}

	/**
	 * サーバ単位のテーブル情報を設定する.
	 * @param guildId
	 * @param tableInfo
	 */
	public void setTableInfo(long guildId, Map<String, Map<String, Object>> tableInfo) {
		if (tableInfo == null) {
			guildTableInfo.remove(guildId);
		} else {
			guildTableInfo.put(guildId, tableInfo);
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

		// コマンドパターン判定
		Map<String, Object> systemInfo = getSystemInfo(system);
		if (systemInfo == null) {
			return null;
		}
		Pattern pattern = (Pattern)systemInfo.get("bot_command_pattern");
		if (pattern != null && !pattern.matcher(text).matches()) {
			return null;
		}

		String path = "v2/game_system/" + system + "/roll?command=";
		try {
			path += URLEncoder.encode(text, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		};

		String result = get(bcDiceApiUrl + path);
		if (result == null) {
			return null;
		}
		Map<String, Object> map = JSON.decode(result);
		return system + map.get("text");
	}

	public synchronized Map<String, String> getSystemNames() {
		if (systemNamesMap == null) {
			String path = "/v2/game_system";
			String result = get(bcDiceApiUrl + path);

			if (result != null) {
				Map<String, Object> map = JSON.decode(result);
				List<Map<String, String>> list = (List<Map<String, String>>)map.get("game_system");
				Map<String, String> namesMap = new ConcurrentHashMap<>();
				for (Map<String, String> entry : list) {
					namesMap.put(entry.get("id"), entry.get("name"));
				}
				systemNamesMap = namesMap;
			}
		}

		return systemNamesMap;
	}

	private Map<String, Object> getSystemInfo(String system) {
		Map<String, Object> systemInfo = systemInfoMap.get(system);
		if (systemInfo != null) {
			return systemInfo;
		}

		String path = "v2/game_system/";
		try {
			path += URLEncoder.encode(system, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		};

		String result = get(bcDiceApiUrl + path);
		if (result != null) {
			systemInfo = JSON.decode(result);
			String commandPattern = (String)systemInfo.get("command_pattern");
			if (commandPattern.contains("|\\d+B\\d+|") && !commandPattern.contains("|\\d+D\\d+|")) {
				commandPattern = commandPattern.replace("|\\d+B\\d+|", "|\\d+[BD]\\d+|") + ".*";
			}
			try {
				Pattern pattern = Pattern.compile(commandPattern, Pattern.CASE_INSENSITIVE);
				systemInfo.put("bot_command_pattern", pattern);
			} catch (Exception e) {
				e.printStackTrace();
			}


			systemInfoMap.put(system, systemInfo);
			return systemInfo;
		}
		return null;

	}

	public String getSystemInfoMessage(String system) {
		Map<String, Object> systemInfo = getSystemInfo(system);
		return (String)systemInfo.get("help_message");
	}

}
