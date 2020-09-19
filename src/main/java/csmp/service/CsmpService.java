package csmp.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;

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
				String secret = post(openUrl, Entity.form(new Form().param("pass", text(baseMap, "publicviewpass"))));
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

	/**
	 * セッション予定日登録.
	 * @param guildId ギルドID
	 * @param webhook webhookURL
	 * @param dates 登録日付
	 * @param message リマインドメッセージ
	 * @return 登録情報
	 */
	public Map<String, Object> registerSchedule(String guildId, String webhook, String dates, String message) {

		String registerUrl = csmpUrl + "sgsbot/registerSchedule";

		String result = post(registerUrl, Entity.form(new Form()
				.param("guildId", guildId)
				.param("webhook", webhook)
				.param("dates", dates)
				.param("message", message)
				));

		if (result != null) {
			return JSON.decode(result);
		}

		return null;

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
	public Map<String, Object> createScheduleAdjustment(String guildId, String serverName, String webhook, String authorIdName,
			List<String> userIdNameList, String roleId) {

		try {
			if (!checkGuildId(guildId)) {
				Map<String, Object> map = new HashMap<>();
				map.put("result", "ng");
				map.put("error", "複数のデイコードが同時に動作しているため、処理を中断しました。");

				return map;
			}

			String registerUrl = csmpUrl + "/schedule/create";
			Form form = new Form();
			form.param("guildId", guildId);
			form.param("serverName", serverName);
			form.param("webhook", webhook);
			form.param("authorIdName", authorIdName);
			for (String userIdName : userIdNameList) {
				form.param("userIdNames", userIdName);
			}
			if (roleId != null) {
				form.param("roleId", roleId);
			}

			String result = post(registerUrl, Entity.form(form));

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
		String registerUrl = csmpUrl + "/schedule/memberList";
		Form form = new Form();
		form.param("guildId", guildId);
		String result = post(registerUrl, Entity.form(form));

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
		String registerUrl = csmpUrl + "/schedule/updateScheduleMembers";
		Form form = new Form();
		form.param("data", JSON.encode(updateSessionMap));
		post(registerUrl, Entity.form(form));
		// 更新に失敗しても通知はしない。
	}


	/**
	 * セッション予定日削除.
	 * @param guildId ギルドID
	 * @param dates カンマ区切りの日付
	 * @return 削除情報
	 */
	public Map<String, Object> deleteSchedule(String guildId, String dates) {

		String registerUrl = csmpUrl + "sgsbot/deleteSchedule";

		String result = post(registerUrl, Entity.form(new Form()
				.param("guildId", guildId)
				.param("dates", dates)
				));

		if (result != null) {
			return JSON.decode(result);
		}

		return null;

	}

	/**
	 * サーバ情報の取得。セッション予定日.
	 * @param guildId ギルドID
	 * @return サーバ情報。現在はセッション予定日情報。
	 */
	public Map<String, Object> getGuildScheduleInfo(String guildId) {

		String registerUrl = csmpUrl + "sgsbot/listSchedule";

		String result = post(registerUrl, Entity.form(new Form()
				.param("guildId", guildId)
				));

		if (result != null) {
			return JSON.decode(result);
		}

		return null;

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
			String result = post(registerUrl, Entity.form(new Form()
					.param("webhook", webhook)
					.param("key", key)
					));

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
