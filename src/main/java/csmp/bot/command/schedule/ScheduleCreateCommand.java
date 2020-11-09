package csmp.bot.command.schedule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.permission.Role;
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

	public static int MAX_USER_SIZE = 50;

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

		String roleName = null;
		String webhookChannelName = null;
		String linkUrl = null;

		if (dmd.getCommandArray().length > 1) {
			if (!"-role".equals(dmd.getCommandArray()[1])
					&& !"-link".equals(dmd.getCommandArray()[1])) {
				webhookChannelName = dmd.getCommandArray()[1];
			}
			for (int i = 1; i < dmd.getCommandArray().length; i++) {
				if ("-role".equals(dmd.getCommandArray()[i])) {
					if (dmd.getCommandArray().length - 1 > i) {
						roleName = dmd.getCommandArray()[i + 1];
					} else {
						dmd.getChannel().sendMessage("-roleの後にはロール名を指定してください。");
						return;
					}
				}
				if ("-link".equals(dmd.getCommandArray()[i])) {
					if (dmd.getCommandArray().length - 1 > i) {
						linkUrl = dmd.getCommandArray()[i + 1];
					} else {
						dmd.getChannel().sendMessage("-linkの後には紐付けたいデイコードのURLを指定してください。");
						return;
					}
				}
			}
		}

		Role role = null;
		if (roleName != null) {
			List<Role> roles = dmd.getGuild().getRolesByName(roleName);
			if (!roles.isEmpty()) {
				role = roles.get(0);
			} else {
				dmd.getChannel().sendMessage(roleName + "のロールが見つかりません。");
				return;
			}
		}


		String webhookUrl = null;
		ServerTextChannel webhookChannel = null;
		if (webhookChannelName != null) {
			// 通知先チャンネルを別にする。
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

        ServerTextChannel stc = null;
        if (dmd.getCommandArray()[0].equals("/スケジュールforCh")
    		&& dmd.getChannel() instanceof ServerTextChannel) {
        	guildId += "#" + dmd.getChannel().getIdAsString();
        	stc = (ServerTextChannel)dmd.getChannel();
        	serverName += "#" + stc.getName();
        }

        Map<String,String> memberMap = DiscordUtil.getMemberIdMap(dmd.getGuild(), stc, role);

        // Discord障害が解消されるまで、オーナーのID、名前を設定しておく。
        if (memberMap.isEmpty()) {
        	memberMap.put(author.getIdAsString(), authorName);
        }

        List<String> userIdNameList = new ArrayList<>();
        for (Entry<String, String> entry : memberMap.entrySet()) {
			userIdNameList.add(entry.getKey() + ":" + entry.getValue());
		}

        if (userIdNameList.size() > MAX_USER_SIZE) {
        	dmd.getChannel().sendMessage("作成できるスケジュールは" + MAX_USER_SIZE + "人までです。\n"
        			+ MAX_USER_SIZE + "人以下のチャンネルを作成して、/スケジュールforChコマンドで作成してください。");
        	return;
        }

        String roleId = null;
        if (role != null) {
        	roleId = role.getIdAsString();
        	guildId += "&" + roleId;
        	serverName += "(" + roleName + ")";
        }

        Map<String, Object> result = null;
        if (linkUrl == null) {
    		result = CsmpService.getInstance().createScheduleAdjustment(
    				guildId, serverName, webhookUrl, authorIdName, userIdNameList, roleId
    				);
        } else {
        	int keyStartIndex = linkUrl.indexOf("key=");
        	if (keyStartIndex < 0) {
        		dmd.getChannel().sendMessage("入力されたURLがデイコードのURLではありません。");
        		return;
        	}

        	int keyEndIndex = linkUrl.indexOf("&", keyStartIndex);
        	if (keyEndIndex < 0) {
        		keyEndIndex = linkUrl.length();
        	}
        	String linkKey = linkUrl.substring(keyStartIndex + 4, keyEndIndex);
        	result = CsmpService.getInstance().linkScheduleAdjustment(guildId, webhookUrl, authorIdName, roleId, linkKey);
        }

		if (result == null) {
			dmd.getChannel().sendMessage("エラーが発生しました。再度コマンドを実行してください。");
		} else if (!"ok".equals(result.get("result"))) {
			dmd.getChannel().sendMessage(String.valueOf(result.get("error")));
		} else {
			if (result.containsKey("url")) {

				String message = "デイコードを設定しました。下記URLから調整する候補日の日程を入力してください。\n"
						+ "参加者の方は、候補日日程入力が終わった連絡を受けてから日程状況一覧＞各個人の日程入力ページへと移動し、入力してください。\n"
					+  "[候補日設定用URL](" + result.get("url") + " )";
				if (webhookChannel != null) {
					webhookChannel.sendMessage(message);
				} else {
					dmd.getChannel().sendMessage(message);
				}
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
		list.add(new CommandHelpData("/スケジュール -role <ロール名>",
				"日程調整用のページを作成する。",
				"ロール名で指定したロールを持つユーザのみを参加者として登録する。「-role <ロール名>」は他のコマンドとの組み合わせも可能。"
				+ "-role以降は一番最後に指定する。「/スケジュールforCh <通知先チャンネル名> -role <ロール名>」など。"
				));

		return new CommandHelpData(list);
	}
}
