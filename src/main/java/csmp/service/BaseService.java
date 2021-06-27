package csmp.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public abstract class BaseService {
	public static final MediaType POST = MediaType.get("text/plain; charset=utf-8");

	protected String get(String url) {
        Request request = new Request.Builder().url(url)
                .get().build();
        return doRequest(request);
	}

	protected String authKey = null;

	protected String post(String url, FormMap params) {
        FormBody.Builder formBuilder = new FormBody.Builder();
		for (Entry<String, List<String>> entry : params.get().entrySet()) {
			for (String value : entry.getValue()) {
				formBuilder.add(entry.getKey(), value);
			}
		}
        FormBody formBody = formBuilder.build();

        Request request = new Request.Builder().url(url)
                .addHeader("Authorization", "Bearer " + authKey)
        		.post(formBody).build();
        return doRequest(request);
	}

	private String doRequest(Request request) {
		OkHttpClient client = new OkHttpClient();
		Response response = null;
		ResponseBody responseBody = null;
        try {
        	response = client.newCall(request).execute();
			responseBody = response.body();
			if (!response.isSuccessful()) {
				return null;
			}

			if (responseBody == null) {
				return null;
			}

			return responseBody.string();

		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			if (responseBody != null) {
				responseBody.close();
			}

			if (response != null) {
				response.close();
			}

		}

	}

	protected class FormMap {
		Map<String, List<String>> params = new HashMap<>();

		public void put(String key, String value) {
			List<String> list = params.get(key);
			if (list == null) {
				list = new ArrayList<>();
				params.put(key, list);
			}
			list.add(value);
		}

		public Map<String, List<String>> get() {
			return params;
		}

	}

}
