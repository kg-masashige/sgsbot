package csmp.utl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.permission.PermissionState;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Permissions;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.entity.webhook.Webhook;
import org.javacord.api.entity.webhook.WebhookBuilder;

import csmp.bot.model.DiscordMessageData;

public class DiscordUtil {

	public static String getWebhookUrl(DiscordMessageData dmd) {
		return getWebhookUrl(dmd, (ServerTextChannel)dmd.getChannel(), 0);
	}

	public static String getWebhookUrl(DiscordMessageData dmd, ServerTextChannel tc) {
		return getWebhookUrl(dmd, tc, 0);
	}

	private static String getWebhookUrl(DiscordMessageData dmd, ServerTextChannel tc, int count) {

		Webhook webhook;
		try {
			List<Webhook> webhookList = tc.getWebhooks().get();
			if (webhookList == null || webhookList.isEmpty()) {
				webhook = new WebhookBuilder(tc)
						.setName(dmd.getGuild().getApi().getApplicationInfo().get().getName())
						.create().get();
			} else {
				webhook = webhookList.get(0);
			}
		} catch (InterruptedException | ExecutionException e) {
			// リトライする
			if (count < 5) {
				return getWebhookUrl(dmd, tc, count + 1);
			} else {
				dmd.getChannel().sendMessage("webhookの作成・取得ができませんでした。権限設定を見直してください。");
				System.err.println("webhook作成エラー guild id:" +
						dmd.getGuild().getIdAsString() + " guild name:" + dmd.getGuild().getName());
				return null;
			}
		}


		String webhookUrl = "https://discordapp.com/api/webhooks/"
				+ webhook.getIdAsString() + "/" + webhook.getToken().get();

		return webhookUrl;
	}

	public static Map<String, String> getMemberIdMap(Server guild, ServerTextChannel stc, Role role) {
		Map<String, String> userMap = new HashMap<>();

        for (User user : guild.getMembers()) {
        	if (user.isBot()) {
        		// botはスルー
        		continue;
        	}
    		if (stc != null) {
        		Permissions permissions = stc.getEffectivePermissions(user);
        		if (permissions.getState(PermissionType.READ_MESSAGES) == PermissionState.DENIED) {
        			// 読み込み権限がなければスルー
        			continue;
        		}
    		}
    		if (role != null) {
    			List<Role> roles = user.getRoles(guild);
    			boolean roleFlag = false;
    			for (Role userRole : roles) {
					if (role.equals(userRole)) {
						roleFlag = true;
						break;
					}
				}
    			if (!roleFlag) {
    				continue;
    			}
    		}

    		userMap.put(user.getIdAsString(),
    				user.getNickname(guild).orElse(user.getDisplayName(guild)));
		}

        return userMap;

	}

}
