package csmp.utl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.WebhookType;
import net.dv8tion.jda.api.entities.channel.attribute.IWebhookContainer;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.requests.ErrorResponse;

public class DiscordUtil {

	/**
	 * Discordのメッセージ1件あたりの最大文字数.
	 */
	public static final int MAX_MESSAGE_LENGTH = 2000;

	/**
	 * ロガー
	 */
	private static Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	/**
	 * チャンネルのwebhook URLを取得する。存在しない場合は作成する.
	 * 取得できなかった場合は、理由をhookに送信してnullを返す。
	 * @param hook 応答用のフック
	 * @param channel webhookを取得するチャンネル（テキスト、フォーラムなど）
	 * @return webhook URL
	 */
	public static String getWebhookUrl(InteractionHook hook, IWebhookContainer channel) {
		try {
			List<Webhook> webhookList = channel.retrieveWebhooks().complete();
			for (Webhook webhook : webhookList) {
				if (webhook.getType() == WebhookType.INCOMING && webhook.getToken() != null) {
					return webhook.getUrl();
				}
			}
			Webhook webhook = channel.createWebhook(channel.getJDA().getSelfUser().getName()).complete();
			return webhook.getUrl();
		} catch (Exception e) {
			logger.error("webhook作成エラー guild id:" +
					channel.getGuild().getId() + " guild name:" + channel.getGuild().getName(), e);
			if (isMissingPermissionsException(e)) {
				sendMessage(hook, "webhookの作成・取得ができませんでした。権限設定を見直すか、別のチャンネルを作成して実施してください。");
			} else {
				sendMessage(hook, "予期せぬエラーが発生しました。Twitter ID:@kg_masashigeまで連絡してください。");
			}
			return null;
		}
	}

	/**
	 * スレッドへ投稿するためのwebhook URLを取得する.
	 * @param hook 応答用のフック
	 * @param parentChannel スレッドの親チャンネル
	 * @param threadId スレッドID
	 * @return webhook URL
	 */
	public static String getWebhookUrl(InteractionHook hook, IWebhookContainer parentChannel, String threadId) {
		String webhookUrl = getWebhookUrl(hook, parentChannel);
		if (webhookUrl == null) {
			return null;
		}
		return webhookUrl + "?thread_id=" + threadId;
	}

	private static boolean isMissingPermissionsException(Throwable t) {
		if (t instanceof InsufficientPermissionException) {
			return true;
		}
		if (t instanceof ErrorResponseException ere
				&& ere.getErrorResponse() == ErrorResponse.MISSING_PERMISSIONS) {
			return true;
		}
		if (t.getCause() != null) {
			return isMissingPermissionsException(t.getCause());
		}
		return false;
	}

	/**
	 * チャンネルを閲覧できるメンバーの一覧を取得する.
	 * botは除外する。
	 * @param guild サーバー
	 * @param channel 閲覧権限を確認するチャンネル。nullの場合は確認しない。
	 * @param role 絞り込むロール。nullの場合は絞り込まない。
	 * @return ユーザーID → 表示名
	 */
	public static Map<String, String> getMemberIdMap(Guild guild, GuildChannel channel, Role role) {
		GuildChannel permissionChannel = channel;
		if (permissionChannel instanceof ThreadChannel thread) {
			// スレッドは親チャンネルの権限で判定する。
			permissionChannel = thread.getParentChannel();
		}

		Map<String, String> userMap = new HashMap<>();
		for (Member member : guild.getMembers()) {
			if (member.getUser().isBot()) {
				// botはスルー
				continue;
			}
			if (permissionChannel != null && !member.hasPermission(permissionChannel, Permission.VIEW_CHANNEL)) {
				// 読み込み権限がなければスルー
				continue;
			}
			if (role != null && !member.getRoles().contains(role)) {
				continue;
			}
			userMap.put(member.getId(), member.getEffectiveName());
		}
		return userMap;
	}

	/**
	 * 応答済み（deferReply済み）のインタラクションに、本人だけに見えるメッセージを送る.
	 * @param hook フック
	 * @param message メッセージ
	 */
	public static void sendMessage(InteractionHook hook, String message) {
		sendMessage(hook, message, true);
	}

	/**
	 * 応答済み（deferReply済み）のインタラクションにメッセージを送る.
	 * 2000文字を超える場合は分割して送る。
	 * @param hook フック
	 * @param message メッセージ
	 * @param isEphemeral 本人だけに見せる場合はtrue
	 */
	public static void sendMessage(InteractionHook hook, String message, boolean isEphemeral) {
		for (String chunk : splitMessage(message)) {
			hook.sendMessage(chunk).setEphemeral(isEphemeral).complete();
		}
	}

	/**
	 * 未応答のインタラクションに応答する.
	 * 2000文字を超える場合は、2件目以降をフォローアップとして送る。
	 * @param event イベント
	 * @param message メッセージ
	 * @param isEphemeral 本人だけに見せる場合はtrue
	 */
	public static void reply(IReplyCallback event, String message, boolean isEphemeral) {
		List<String> chunks = splitMessage(message);
		event.reply(chunks.get(0)).setEphemeral(isEphemeral).complete();
		for (int i = 1; i < chunks.size(); i++) {
			event.getHook().sendMessage(chunks.get(i)).setEphemeral(isEphemeral).complete();
		}
	}

	/**
	 * チャンネルにメッセージを投稿する.
	 * 2000文字を超える場合は分割して投稿する。
	 * @param channel チャンネル
	 * @param message メッセージ
	 */
	public static void sendToChannel(MessageChannel channel, String message) {
		for (String chunk : splitMessage(message)) {
			channel.sendMessage(chunk).queue();
		}
	}

	/**
	 * メッセージを2000文字以内に分割する.
	 * できるだけ改行位置で区切る。
	 * @param message メッセージ
	 * @return 分割したメッセージ。空のメッセージの場合も1件返す。
	 */
	public static List<String> splitMessage(String message) {
		List<String> list = new ArrayList<>();
		String rest = message == null ? "" : message;
		while (rest.length() > MAX_MESSAGE_LENGTH) {
			int index = rest.lastIndexOf('\n', MAX_MESSAGE_LENGTH);
			if (index <= 0) {
				index = MAX_MESSAGE_LENGTH;
			}
			list.add(rest.substring(0, index));
			rest = rest.substring(index);
			if (rest.startsWith("\n")) {
				rest = rest.substring(1);
			}
		}
		if (!rest.isEmpty() || list.isEmpty()) {
			list.add(rest);
		}
		return list;
	}

	/**
	 * 文字数の上限に収まるよう末尾を切り詰める.
	 * @param text 文字列
	 * @param maxLength 上限
	 * @return 切り詰めた文字列
	 */
	public static String truncate(String text, int maxLength) {
		if (text == null || text.length() <= maxLength) {
			return text;
		}
		return text.substring(0, maxLength - 1) + "…";
	}

	/**
	 * コマンドを実行したメンバーの表示名を取得する.
	 * @param event イベント
	 * @return 表示名
	 */
	public static String getDisplayName(GenericCommandInteractionEvent event) {
		Member member = event.getMember();
		if (member != null) {
			return member.getEffectiveName();
		}
		return event.getUser().getEffectiveName();
	}

}
