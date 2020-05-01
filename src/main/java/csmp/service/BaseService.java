package csmp.service;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;

public abstract class BaseService {

	protected String get(String url) {
		return request(url, null);
	}

	protected String post(String url, Entity<Form> entity) {
		return request(url, entity);
	}

	protected String request(String url, Entity<Form> entity) {
		Client client = ClientBuilder.newClient();

		Builder request = client.target(url).request();
		Response response;
		if (entity != null) {
			response = request.post(entity);
		} else {
			response = request.get();
		}

		String result = null;
		if (response.getStatus() == 200) {
			result = response.readEntity(String.class);
		}
		response.close();

		return result;
	}
}
