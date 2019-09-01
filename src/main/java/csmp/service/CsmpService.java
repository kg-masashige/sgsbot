package csmp.service;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

import csmp.utl.HttpConnectionUtil;
import discord4j.core.object.util.Snowflake;
import net.arnx.jsonic.JSON;

public class CsmpService {

	private static Map<Snowflake, Map<Object,Object>> guildScenarioInfo = new HashMap<>();

	/**
	 * キャラクターシートのベースURL取得。
	 * @return テスト環境だとlocal。
	 */
	private static String getBaseURL() {
		return System.getenv("CHARACTER_SHEETS_URL");
	}

	/**
	 * シナリオシート情報を取得する.
	 * @param sheetUrl シナリオシートURL.
	 * @return シナリオ秘密Map
	 */
	public static Map<Object, Object> getScenarioSheetInfo(String sheetUrl) {
		// TODO 入力チェック
		try {
			if (sheetUrl.startsWith("<") && sheetUrl.endsWith(">")) {
				sheetUrl = sheetUrl.substring(1, sheetUrl.length() - 1);
			}
			String dispUrl = sheetUrl.replace("edit.html", "display") + "&ajax=1";
			HttpURLConnection con = HttpConnectionUtil.getConnection(dispUrl);
			OutputStreamWriter writer = new OutputStreamWriter(con.getOutputStream());
			writer.close();
			if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
				Map<Object, Object> map = JSON.decode(con.getInputStream());
				con.disconnect();
				Map<String, Object> baseMap = (Map<String, Object>)map.get("base");
				if (baseMap != null && "1".equals(baseMap.get("publicview"))) {
					String openUrl = dispUrl.replace("display", "openSecret");
					HttpURLConnection secretCon = HttpConnectionUtil.getConnection(openUrl);
					OutputStreamWriter secretWriter = new OutputStreamWriter(secretCon.getOutputStream());
					secretWriter.write("pass=" + text(baseMap, "publicviewpass"));
					secretWriter.flush();
					secretWriter.close();
					Map<Object, Object> secretMap = JSON.decode(secretCon.getInputStream());
					secretCon.disconnect();

					return secretMap;
				}
			}
		} catch (Exception e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
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
	public static Map<String, Object> registerSchedule(String guildId, String webhook, String dates, String message) {

		try {
			String registerUrl = getBaseURL() + "sgsbot/registerSchedule";

			HttpURLConnection con = HttpConnectionUtil.getConnection(registerUrl);
			OutputStreamWriter writer = new OutputStreamWriter(con.getOutputStream());
			writer.write("guildId=" + guildId);
			writer.write("&webhook=" + webhook);
			writer.write("&dates=" + dates);
			writer.write("&message=" + message);
			writer.flush();
			writer.close();
			if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
				Map<String, Object> map = JSON.decode(con.getInputStream());
				con.disconnect();

				return map;
			}
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		return null;

	}

	/**
	 * セッション予定日削除.
	 * @param guildId ギルドID
	 * @param dates カンマ区切りの日付
	 * @return 削除情報
	 */
	public static Map<String, Object> deleteSchedule(String guildId, String dates) {

		try {
			String registerUrl = getBaseURL() + "sgsbot/deleteSchedule";

			HttpURLConnection con = HttpConnectionUtil.getConnection(registerUrl);
			OutputStreamWriter writer = new OutputStreamWriter(con.getOutputStream());
			writer.write("guildId=" + guildId);
			writer.write("&dates=" + dates);
			writer.flush();
			writer.close();
			if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
				Map<String, Object> map = JSON.decode(con.getInputStream());
				con.disconnect();

				return map;
			}
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		return null;

	}

	/**
	 * サーバ情報の取得。セッション予定日.
	 * @param guildId ギルドID
	 * @return サーバ情報。現在はセッション予定日情報。
	 */
	public static Map<String, Object> getGuildScheduleInfo(String guildId) {

		try {
			String url = getBaseURL() + "sgsbot/listSchedule";

			HttpURLConnection con = HttpConnectionUtil.getConnection(url);
			OutputStreamWriter writer = new OutputStreamWriter(con.getOutputStream());
			writer.write("guildId=" + guildId);
			writer.flush();
			writer.close();
			if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
				Map<String, Object> map = JSON.decode(con.getInputStream());
				con.disconnect();

				return map;
			}
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

		return null;

	}

	public static Map<Snowflake, Map<Object,Object>> getGuildScenarioInfo() {
		return guildScenarioInfo;
	}

}
