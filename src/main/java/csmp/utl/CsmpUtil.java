package csmp.utl;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.util.Map;

import net.arnx.jsonic.JSON;

public class CsmpUtil {

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

}
