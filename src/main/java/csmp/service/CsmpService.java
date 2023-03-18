package csmp.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import csmp.bot.model.ScheduleCommandData;
import net.arnx.jsonic.JSON;

public class CsmpService extends BaseService {

	private Map<Long, Map<Object,Object>> guildScenarioInfo = new ConcurrentHashMap<>();

	private static CsmpService instance;

	public synchronized static CsmpService getInstance() {
		if (instance == null) {
			instance = new CsmpService();
		}
		return instance;
	}

	private CsmpService() {
		if (csmpUrl == null) {
			csmpUrl = System.getenv("CHARACTER_SHEETS_URL");
			csmpUrl = csmpUrl.endsWith("/") ? csmpUrl : csmpUrl + "/";
		}
		if (authKey == null) {
			authKey = System.getenv("CHARACTER_SHEETS_AUTHKEY");
		}
	}

	private String csmpUrl = null;

	/**
	 * シナリオシート情報を取得する.
	 * @param sheetUrl シナリオシートURL.
	 * @return シナリオ秘密Map
	 */
	public Map<Object, Object> getScenarioSheetInfo(String sheetUrl) {

		String key = getKey(sheetUrl);

		if (key == null) {
			return null;
		}

		String dispUrl = csmpUrl + "sgScenario/display?ajax=1&key=" + key;
		String result = get(dispUrl);
		if (result != null) {
			Map<Object, Object> map = JSON.decode(result);
			Map<String, Object> baseMap = (Map<String, Object>)map.get("base");
			if (baseMap != null && "1".equals(baseMap.get("publicview"))) {
				String openUrl = dispUrl.replace("display", "openSecret");
				FormMap params = new FormMap();
				params.put("pass", text(baseMap, "publicviewpass"));
				String secret = post(openUrl, params);
				Map<Object, Object> secretMap = JSON.decode(secret);

				return secretMap;
			}

		}

		return null;

	}

	/**
	 * マップから文字を取り出す。なければ空文字.
	 * @param map シナリオ情報マップ
	 * @param key キー
	 * @return 値。null to blank.
	 */
	public static String text(Map<String, Object> map, String key) {
		Object result = map.get(key);
		if (result == null) {
			result = "";
		}
		return result.toString();
	}

	private static Map<String, String> guildExecutionMap = new ConcurrentHashMap<>();

	private static synchronized boolean checkGuildId(String guildId) {
		boolean isExecution = guildExecutionMap.containsKey(guildId);
		if (!isExecution) {
			guildExecutionMap.put(guildId, guildId);
			return true;
		}
		return false;
	}

	private static synchronized void removeGuildId(String guildId) {
		guildExecutionMap.remove(guildId);
	}

	/**
	 * 日程調整ページ作成.
	 */
	public Map<String, Object> createScheduleAdjustment(ScheduleCommandData data) {
		String guildId = data.getGuildId();
		String serverName = data.getServerName();
		String webhook = data.getWebhook();
		String authorIdName = data.getAuthorIdName();
		List<String> userIdNameList = data.getUserIdNameList();
		String roleId = data.getRoleId();

		try {
			if (!checkGuildId(guildId)) {
				Map<String, Object> map = new HashMap<>();
				map.put("result", "ng");
				map.put("error", "複数のデイコードが同時に動作しているため、処理を中断しました。");

				return map;
			}

			String registerUrl = csmpUrl + "schedule/create";
			FormMap params = new FormMap();
			params.put("guildId", guildId);
			params.put("serverName", serverName);
			params.put("webhook", webhook);
			params.put("authorIdName", authorIdName);
			for (String userIdName : userIdNameList) {
				params.put("userIdNames", userIdName);
			}
			if (roleId != null) {
				params.put("roleId", roleId);
			}
			if (data.isForce()) {
				params.put("force", "true");
			}
			if (data.isSlashCommand()) {
				params.put("slashCommand", "true");
			}

			String result = post(registerUrl, params);

			if (result != null) {
				Map<String, Object> map = JSON.decode(result);
				if (map.containsKey("key")) {
					map.put("url", csmpUrl + "schedule/edit?key=" + map.get("key"));
				}

				return map;
			}

		} finally {
			removeGuildId(guildId);
		}

		return null;
	}

	/**
	 * スケジュールの紐付け.
	 * @param guildId
	 * @param webhookUrl
	 * @param authorIdName
	 * @param roleId
	 * @param linkKey
	 * @return
	 */
	public Map<String, Object> linkScheduleAdjustment(String guildId, String webhook, String authorIdName,
			String roleId, String linkKey) {
		try {
			if (!checkGuildId(guildId)) {
				Map<String, Object> map = new HashMap<>();
				map.put("result", "ng");
				map.put("error", "複数のデイコードが同時に動作しているため、処理を中断しました。");

				return map;
			}

			String registerUrl = csmpUrl + "schedule/link";
			FormMap params = new FormMap();
			params.put("guildId", guildId);
			params.put("webhook", webhook);
			params.put("authorIdName", authorIdName);
			params.put("linkKey", linkKey);
			if (roleId != null) {
				params.put("roleId", roleId);
			}

			String result = post(registerUrl, params);

			if (result != null) {
				Map<String, Object> map = JSON.decode(result);
				if (map.containsKey("key")) {
					map.put("url", csmpUrl + "schedule/edit?key=" + map.get("key"));
				}

				return map;
			}

		} finally {
			removeGuildId(guildId);
		}

		return null;
	}

	/**
	 * ギルドIDに紐づくスケジュールメンバーのマップを取得する.
	 * @param guildId
	 * @return
	 */
	public Map<String, Object> getScheduleMemberIdMap(String guildId) {
		String registerUrl = csmpUrl + "schedule/memberList";
		FormMap params = new FormMap();
		params.put("guildId", guildId);
		String result = post(registerUrl, params);

		if (result != null) {
			Map<String, Object> map = JSON.decode(result);
			if ("ok".equals(map.get("result"))) {
				Map<String, Object> sessionMap = (Map<String, Object>)map.get("sessionMap");
				return sessionMap;

			}
		}

		return null;

	}

	public void updateScheduleMembers(Map<String, Map<String, List<String>>> updateSessionMap) {
		String registerUrl = csmpUrl + "schedule/updateScheduleMembers";
		FormMap params = new FormMap();
		params.put("data", JSON.encode(updateSessionMap));
		post(registerUrl, params);
		// 更新に失敗しても通知はしない。
	}


	/**
	 * キャラクターシートとwebhookの紐付け登録をする.
	 * @param webhook webhookURL
	 * @param key キャラシのキー
	 * @return 結果
	 */
	public Map<String, Object> registerCharacterSheet(String webhook, String sheetUrl) {
		String registerUrl = csmpUrl + "cooperation/discord/registerCharacterSheet";

		String key = getKey(sheetUrl);
		if (key != null) {
			FormMap params = new FormMap();
			params.put("webhook", webhook);
			params.put("key", key);
			String result = post(registerUrl, params);

			if (result != null) {
				return JSON.decode(result);
			}
		}


		return null;

	}

	public Map<Long, Map<Object,Object>> getGuildScenarioInfo() {
		return guildScenarioInfo;
	}

	private static final String KEY_QUERY_NAME = "key=";

	/**
	 * キャラシ倉庫URLからシートのキーを取得する
	 * @param url 入力されたURL
	 * @return キー
	 */
	private String getKey(String url) {
		String sheetUrl = url;
		if (sheetUrl.startsWith("<") && sheetUrl.endsWith(">")) {
			sheetUrl = sheetUrl.substring(1, sheetUrl.length() - 1);
		}

		if (sheetUrl.contains(KEY_QUERY_NAME)) {
			String key = sheetUrl.substring(sheetUrl.indexOf(KEY_QUERY_NAME) + KEY_QUERY_NAME.length());
			if (key.contains("&")) {
				key = key.substring(0, key.indexOf("&"));
			}
			return key;
		}

		return null;

	}

}
