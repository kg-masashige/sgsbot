package csmp.bot.command.schedule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.permission.PermissionState;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Permissions;
import org.javacord.api.entity.user.User;

import csmp.bot.command.IDiscordCommand;
import csmp.bot.model.CommandHelpData;
import csmp.bot.model.DiscordMessageData;
import csmp.service.CsmpService;
import csmp.utl.DiscordUtil;

/**
 * スケジュール追加コマンド.
 * @author kgmas
 *
 */
public class ScheduleCreateCommand implements IDiscordCommand {

	private static int MAX_USER_SIZE = 50;

	@Override
	public boolean judgeExecute(DiscordMessageData dmd) {
		if (dmd.getGuild() == null) {
			return false;
		}
		if (dmd.getCommandArray()[0].equals("/スケジュール")) {
			return true;
		}

		// チャンネル単位追加
		if (dmd.getCommandArray()[0].equals("/スケジュールforCh")) {
			return true;
		}

		return false;
	}

	@Override
	public boolean checkInput(DiscordMessageData dmd) {
		return true;
	}

	@Override
	public void execute(DiscordMessageData dmd) throws InterruptedException, ExecutionException {

		String webhookUrl = null;
		ServerTextChannel webhookChannel = null;
		if (dmd.getCommandArray().length > 1) {
			// 通知先チャンネルを別にする。
			String webhookChannelName = dmd.getCommandArray()[1];
			List<ServerChannel> channelList = dmd.getGuild().getChannelsByName(webhookChannelName);
			for (ServerChannel serverChannel : channelList) {
				if (serverChannel instanceof ServerTextChannel) {
					webhookChannel = (ServerTextChannel)serverChannel;
					webhookUrl = DiscordUtil.getWebhookUrl(dmd, webhookChannel);
					break;
				}
			}
			if (webhookUrl == null) {
				dmd.getChannel().sendMessage(webhookChannelName + "のテキストチャンネルが見つかりません。");
			}

		} else {
			webhookUrl = DiscordUtil.getWebhookUrl(dmd);
		}

		if (webhookUrl == null) {
			return;
		}

        String guildId = dmd.getGuild().getIdAsString();
        String serverName = dmd.getGuild().getName();
        MessageAuthor author = dmd.getMessage().getMessageAuthor();
        User authorUser = author.asUser().orElse(null);
        String authorName = author.getDisplayName();
        if (authorUser != null) {
        	authorName = authorUser.getNickname(dmd.getGuild()).orElse(authorUser.getDisplayName(dmd.getGuild()));
        }
        String authorIdName = author.getIdAsString() + ":" + authorName;

        List<String> userIdNameList = new ArrayList<>();

        ServerTextChannel stc = null;
        if (dmd.getText().equals("/スケジュールforCh")
    		&& dmd.getChannel() instanceof ServerTextChannel) {
        	guildId += "#" + dmd.getChannel().getIdAsString();
        	stc = (ServerTextChannel)dmd.getChannel();
        	serverName += "#" + stc.getName();
        }
        for (User user : dmd.getGuild().getMembers()) {
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

			String userIdName = user.getIdAsString() + ":" +
					user.getNickname(dmd.getGuild()).orElse(user.getDisplayName(dmd.getGuild()));
			userIdNameList.add(userIdName);
		}

        if (userIdNameList.size() > MAX_USER_SIZE) {
        	dmd.getChannel().sendMessage("作成できるスケジュールは" + MAX_USER_SIZE + "人までです。\n"
        			+ MAX_USER_SIZE + "人以下のチャンネルを作成して、/スケジュールforChコマンドで作成してください。");
        	return;
        }

		Map<String, Object> result = CsmpService.getInstance().createScheduleAdjustment(
				guildId, serverName, webhookUrl, authorIdName, userIdNameList
				);
		if (result == null) {
			dmd.getChannel().sendMessage("エラーが発生しました。再度コマンドを実行してください。");
		} else if (!"ok".equals(result.get("result"))) {
			dmd.getChannel().sendMessage(String.valueOf(result.get("error")));
		} else {
			String message = "日程調整用URL： " + result.get("url");
			if (webhookChannel != null) {
				webhookChannel.sendMessage(message);
			} else {
				dmd.getChannel().sendMessage(message);
			}
		}

	}

	@Override
	public void warning(DiscordMessageData dmd) {
	}

	@Override
	public CommandHelpData getCommandHelpData() {
		List<CommandHelpData> list = new ArrayList<>();
		list.add(new CommandHelpData("/スケジュール",
				"日程調整用のページを作成する。",
				"作成したページURLがチャンネルに通知される。サーバ内のユーザ数が" + MAX_USER_SIZE + "人を超えるとエラー。"
				));
		list.add(new CommandHelpData("/スケジュールforCh",
				"コマンドを発行したチャンネル単位に日程調整用のページを作成する。",
				"コマンドは「/スケジュールforCh」固定。チャンネル内のユーザ数が" + MAX_USER_SIZE + "人を超えるとエラー。"
				));
		list.add(new CommandHelpData("/スケジュール <通知先チャンネル名>",
				"日程調整用のページを作成する。",
				"作成したページURLが通知先チャンネルに通知される。（通知先チャンネル名がtestなら 「/スケジュール test」）"
				));
		list.add(new CommandHelpData("/スケジュールforCh <通知先チャンネル名>",
				"コマンドを発行したチャンネル単位に日程調整用のページを作成する。",
				"作成したページURLが通知先チャンネルに通知される。（通知先チャンネル名がtestなら 「/スケジュールforCh test」）"
				));

		return new CommandHelpData(list);
	}
}
