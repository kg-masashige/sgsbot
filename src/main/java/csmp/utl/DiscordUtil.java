package csmp.utl;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.webhook.Webhook;
import org.javacord.api.entity.webhook.WebhookBuilder;

import csmp.bot.model.DiscordMessageData;

public class DiscordUtil {

	public static String getWebhookUrl(DiscordMessageData dmd) {
		return getWebhookUrl(dmd, 0);
	}


	private static String getWebhookUrl(DiscordMessageData dmd, int count) {

		ServerTextChannel tc = (ServerTextChannel)dmd.getChannel();
		Server guild = dmd.getGuild();
		Webhook webhook;
		try {
			List<Webhook> webhookList = tc.getWebhooks().get();
			if (webhookList == null || webhookList.isEmpty()) {
				webhook = new WebhookBuilder(tc)
						.setName(guild.getName())
						.create().get();
			} else {
				webhook = webhookList.get(0);
			}
		} catch (InterruptedException | ExecutionException e) {
			// リトライする
			if (count < 5) {
				return getWebhookUrl(dmd, count + 1);
			} else {
				dmd.getChannel().sendMessage("webhookの管理ができませんでした。");
				e.printStackTrace();
				return null;
			}
		}


		String webhookUrl = "https://discordapp.com/api/webhooks/"
				+ webhook.getIdAsString() + "/" + webhook.getToken().get();

		return webhookUrl;
	}
}
