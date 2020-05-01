package csmp.utl;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.webhook.Webhook;
import org.javacord.api.entity.webhook.WebhookBuilder;

import csmp.bot.model.DiscordMessageData;

public class DiscordUtil {

	public static String getWebhookUrl(DiscordMessageData dmd) throws InterruptedException, ExecutionException {

		ServerTextChannel tc = (ServerTextChannel)dmd.getChannel();
		Server guild = dmd.getGuild();
		List<Webhook> webhookList = tc.getWebhooks().get();
		Webhook webhook;
		if (webhookList == null || webhookList.isEmpty()) {
			webhook = new WebhookBuilder(tc)
					.setName(guild.getName())
					.create().get();
		} else {
			webhook = webhookList.get(0);
		}


		String webhookUrl = "https://discordapp.com/api/webhooks/"
				+ webhook.getIdAsString() + "/" + webhook.getToken().get();

		return webhookUrl;
	}
}
