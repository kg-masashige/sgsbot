package csmp.utl;

import java.text.Normalizer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateUtil {

	/**
	 * カンマ区切りの日付を正しいフォーマットに修正する.
	 * @param textArray カンマ区切りの日付
	 * @return 正しいフォーマットのカンマ区切りの日付
	 */
	public static String toFormatDateArray(String textArray) {
		String result = "";
		textArray = textArray.replaceAll("、", ",");
		for (String text : textArray.split(",")) {
			result += toFormatDate(text) + ",";
		}
		return result.substring(0, result.length() - 1);
	}

	/**
	 * 日付フォーマットの取得
	 * @return sdf
	 */
	public static SimpleDateFormat getDateFormat() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
		sdf.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
		return sdf;
	}

	/**
	 * 日付フォーマットの取得（曜日こみ）
	 * @return sdf
	 */
	public static SimpleDateFormat getDateFormatWeek() {
	    SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd(EEE)", Locale.JAPANESE);
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
        return sdf;
	}

	/**
	 * 日付を一番近い未来のyyyy/MM/ddに変換する。
	 * @param text テキスト。
	 * @return 日付テキスト（yyyy/MM/dd）
	 */
	public static String toFormatDate(String text) {
		text = text.trim();
		String[] excludeArray = {"(", " ", "（", "　", "日"};
		for (String exclude : excludeArray) {
			if (text.contains(exclude)) {
				text = text.substring(0, text.indexOf(exclude));
			}
		}
		text = text.replace("月", "/");
		text = text.replace("年", "/");
		text = text.replaceAll("-", "/");
		text = Normalizer.normalize(text, Normalizer.Form.NFKC);

		String[] ymdArray = text.split("/");
		if (ymdArray.length < 2) {
			return "";
		}
		String dd = ymdArray[ymdArray.length - 1];
		if (dd.length() == 1) {
			dd = "0" + dd;
		} else if (dd.length() > 3) {
			dd = dd.substring(0, 2);
		}
		String mm = ymdArray[ymdArray.length - 2];
		if (mm.length() == 1) {
			mm = "0" + mm;
		} else if (mm.length() > 3) {
			mm = mm.substring(0, 2);
		}
		Calendar c = Calendar.getInstance();
		String yyyy = String.valueOf(c.get(Calendar.YEAR));

		try {
			SimpleDateFormat sdf = getDateFormat();
			Date d = sdf.parse(yyyy + "/" + mm + "/" + dd);
			if (d.compareTo(c.getTime()) == -1) {
				yyyy = String.valueOf(c.get(Calendar.YEAR) + 1);
			}
			d = sdf.parse(yyyy + "/" + mm + "/" + dd);

			return sdf.format(d);
		} catch (ParseException e) {
			return "";
		}

	}

}
