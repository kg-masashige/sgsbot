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
	 * サーバ単位のオリジナル表を1件登録する. 同じコマンドの表は上書きする.
	 * @param guildId サーバID
	 * @param command 反応させるコマンド
	 * @param dice 振るダイス
	 * @param title 表の名前
	 * @param resultMap 出目 → 文章
	 */
	public synchronized void putTable(long guildId, String command, String dice, String title,
			Map<String, String> resultMap) {
		Map<String, Map<String, Object>> tableInfo = guildTableInfo.computeIfAbsent(guildId,
				key -> new ConcurrentHashMap<>());
		Map<String, Object> detailMap = new ConcurrentHashMap<>();
		detailMap.put("dice", dice);
		detailMap.put("title", title);
		detailMap.put("result", new ConcurrentHashMap<>(resultMap));
		tableInfo.put(command, detailMap);
	}

	/**
	 * サーバ単位のオリジナル表を振る.
	 * @param guildId サーバID
	 * @param command 登録したコマンド
	 * @return 結果の文章. 該当する表が登録されていない場合はnull.
	 */
	@SuppressWarnings("unchecked")
	public String rollTable(long guildId, String command) {
		Map<String, Map<String, Object>> tableInfo = getTableInfo(guildId);
		if (tableInfo == null) {
			return null;
		}
		Map<String, Object> tableDetail = tableInfo.get(command);
		if (tableDetail == null) {
			return null;
		}
		String dice = (String)tableDetail.get("dice");
		String title = (String)tableDetail.get("title");
		Map<String, String> resultMap = (Map<String, String>)tableDetail.get("result");

		String rollResult = rollDice(dice, guildId);
		if (rollResult == null) {
			String system = getGuildSystem(guildId);
			return "ダイスロールが失敗しました。システム：" + system + " コマンド：" + dice;
		}
		String[] rollResultArray = rollResult.split(" ", -1);
		String value = rollResultArray[rollResultArray.length - 1];
		String explain = resultMap.get(value);
		if (explain == null) {
			explain = "";
		}
		return title + "(" + value + ") → " + explain.replaceAll("\\\\n", "\n");
	}

	/**
	 * システムのIDまたは表示名から、システムIDを取得する.
	 * @param name システムのIDまたは表示名
	 * @return システムID. 見つからない場合はnull.
	 */
	public String resolveSystemId(String name) {
		Map<String, String> systemNames = getSystemNames();
		if (systemNames == null || name == null) {
			return null;
		}
		if (systemNames.containsKey(name)) {
			return name;
		}
		for (Map.Entry<String, String> entry : systemNames.entrySet()) {
			if (name.equals(entry.getValue())) {
				return entry.getKey();
			}
		}
		return null;
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
