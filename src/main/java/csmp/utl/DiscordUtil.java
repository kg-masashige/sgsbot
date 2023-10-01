package csmp.utl;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.channel.ServerForumChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.ServerThreadChannel;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.permission.PermissionState;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Permissions;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.entity.webhook.IncomingWebhook;
import org.javacord.api.entity.webhook.Webhook;
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
		sendMessage("フォーラムチャンネルやテキストチャンネルのスレッド内でコマンドを実行する場合、作成単位をスレッドにしてください。\n"
				+ "ボイスチャンネルのチャットなどでは実行できません。", dmd, true);
		return null;

	}

	public static String getWebhookUrl(DiscordMessageData dmd, ServerTextChannel tc, String threadId) {

		String webhookUrl = getWebhookUrl(dmd, tc);
		return webhookUrl + "?thread_id=" + threadId;
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

	public static String getWebhookUrl(DiscordMessageData dmd, ServerForumChannel forumChannel, String threadId) {
		List<Webhook> list = dmd.getGuild().getAllIncomingWebhooks().join();
		for (Webhook webhook : list) {
			if (webhook.asIncomingWebhook().isPresent()) {
				IncomingWebhook incomingWebhook = webhook.asIncomingWebhook().get();
				if (incomingWebhook.getChannelId() == forumChannel.getId()) {
					return incomingWebhook.getUrl().toString() + "?thread_id=" + threadId;
				}
			}

		}
		sendMessage("フォーラムにウェブフックが作成されていません。\n"
				+ "フォーラム内でデイコードを使用する場合、手動でウェブフックを作成する必要があります。\n"
				+ "フォーラムチャンネルを編集し、ウェブフックを作成してください。", dmd);

		return null;

	}

	public static String getWebhookUrl(DiscordMessageData dmd, ServerTextChannel tc) {

		try {
			List<IncomingWebhook> webhookList = tc.getIncomingWebhooks().join();
			for (IncomingWebhook incomingWebhook : webhookList) {
				if (incomingWebhook == null || incomingWebhook.getUrl() == null) {
					continue;
				}
				return incomingWebhook.getUrl().toString();
			}

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

	private static Permissions getPermissions(ServerChannel channel, User user) {
		if (channel instanceof ServerTextChannel) {
			return ((ServerTextChannel)channel).getEffectivePermissions(user);
		} else if (channel instanceof ServerForumChannel) {
			return ((ServerForumChannel)channel).getEffectivePermissions(user);
		} else if (channel instanceof ServerThreadChannel) {
			return getPermissions(((ServerThreadChannel) channel).getParent(), user);
		}

		return null;
	}

	public static Map<String, String> getMemberIdMap(Server guild, ServerChannel channel, Role role) {
		Map<String, String> userMap = new HashMap<>();

        for (User user : guild.getMembers()) {
        	if (user.isBot()) {
        		// botはスルー
        		continue;
        	}
    		if (channel != null) {
        		Permissions permissions = getPermissions(channel, user);
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

	public static String getSystemName(String[] commandArray) {
		String targetSystem = commandArray[2];
		for (int i = 3; i < commandArray.length; i++) {
			targetSystem += " " + commandArray[i];
		}
		return targetSystem;
	}



}
