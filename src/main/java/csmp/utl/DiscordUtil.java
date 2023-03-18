package csmp.utl;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.permission.PermissionState;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Permissions;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.entity.webhook.IncomingWebhook;
import org.javacord.api.entity.webhook.WebhookBuilder;
import org.javacord.api.exception.MissingPermissionsException;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;

import csmp.bot.model.DiscordMessageData;

public class DiscordUtil {

	/**
	 * ロガー
	 */
	private static Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static String getWebhookUrl(DiscordMessageData dmd) {
		if (dmd.getChannel() instanceof ServerTextChannel) {
			return getWebhookUrl(dmd, (ServerTextChannel)dmd.getChannel());
		}
		sendMessage("スケジュール作成のコマンドはテキストチャンネル内で実施してください。\n"
				+ "スレッド内やボイスチャンネル内のチャットでは使用できません。", dmd, true);
		return null;

	}


	private static boolean isMissingPermissionsException(Throwable t) {
		if (t instanceof MissingPermissionsException) {
			return true;
		}
		if (t.getCause() != null) {
			return isMissingPermissionsException(t.getCause());
		}
		return false;
	}

	public static String getWebhookUrl(DiscordMessageData dmd, ServerTextChannel tc) {

		List<IncomingWebhook> webhookList = tc.getIncomingWebhooks().join();
		for (IncomingWebhook incomingWebhook : webhookList) {
			if (incomingWebhook == null || incomingWebhook.getUrl() == null) {
				continue;
			}
			return incomingWebhook.getUrl().toString();
		}

		try {
			IncomingWebhook webhook = new WebhookBuilder(tc)
					.setName(dmd.getGuild().getApi().getCachedApplicationInfo().getName())
					.create().join();

			return webhook.getUrl().toString();
		} catch (Exception e) {
			logger.error("webhook作成エラー guild id:" +
					dmd.getGuild().getIdAsString() + " guild name:" + dmd.getGuild().getName(), e);
			if (isMissingPermissionsException(e)) {
				sendMessage("webhookの作成・取得ができませんでした。権限設定を見直すか、別のチャンネルを作成して実施してください。", dmd, true);
			} else {
				sendMessage("予期せぬエラーが発生しました。Twitter ID:@kg_masashigeまで連絡してください。", dmd, true);
			}

			return null;
		}
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
        		if (permissions.getState(PermissionType.VIEW_CHANNEL) == PermissionState.DENIED) {
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

	public static void sendMessage(String message, DiscordMessageData dmd, boolean isEphemeral) {
		if (dmd.getInteraction() != null) {
			InteractionImmediateResponseBuilder responder = dmd.getInteraction().createImmediateResponder();
			if (isEphemeral) {
				responder = responder.setFlags(MessageFlag.EPHEMERAL);
			}
			try {
				responder.setContent(message).respond().join();
			} catch (Exception e) {
				dmd.getChannel().sendMessage(message + "\n\n※アプリケーションの応答に失敗したため、チャンネル向けメッセージで送付します。");
			}
		} else {
			dmd.getChannel().sendMessage(message);
		}
	}

	public static void sendMessage(String message, DiscordMessageData dmd) {
		sendMessage(message, dmd, true);
	}

}
