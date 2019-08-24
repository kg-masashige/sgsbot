package csmp.utl;

import java.net.HttpURLConnection;
import java.net.URL;

public class HttpConnectionUtil {
	/**
	 * HTTP接続情報を取得する.
	 * @param site シナリオシートURL
	 * @return 接続情報
	 */
	public static HttpURLConnection getConnection(String site) {
		HttpURLConnection con = null;

		try {
			URL url = new URL(site);
			con = (HttpURLConnection)url.openConnection();
			con.setDoOutput(true);
			con.setConnectTimeout(100000);
			con.setReadTimeout(100000);
			con.setRequestMethod("POST");
		} catch (Exception e) {
			e.printStackTrace();
		}

		return con;
	}
}
