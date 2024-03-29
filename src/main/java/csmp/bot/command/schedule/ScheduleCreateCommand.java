package csmp.bot.command.schedule;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.channel.ServerForumChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.ServerThreadChannel;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionChoice;
import org.javacord.api.interaction.SlashCommandOptionType;

import csmp.bot.command.IDiscordCommand;
import csmp.bot.command.IDiscordSlashCommand;
import csmp.bot.model.CommandHelpData;
import csmp.bot.model.DiscordMessageData;
import csmp.bot.model.ScheduleCommandData;
import csmp.service.CsmpService;
import csmp.utl.DiscordUtil;

/**
 * スケジュール追加コマンド.
 * @author kgmas
 *
 */
public class ScheduleCreateCommand implements IDiscordCommand, IDiscordSlashCommand {

	/**
	 * ロガー
	 */
	private static Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

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
					&& !"-link".equals(dmd.getCommandArray()[1])
					&& !"-force".equals(dmd.getCommandArray()[1])) {
				webhookChannelName = dmd.getCommandArray()[1];
			}
			for (int i = 1; i < dmd.getCommandArray().length; i++) {
				if ("-role".equals(dmd.getCommandArray()[i])) {
					if (dmd.getCommandArray().length - 1 > i) {
						roleName = dmd.getCommandArray()[i + 1];
					} else {
						DiscordUtil.sendMessage("ロール名を指定してください。", dmd);
						return;
					}
				}
				if ("-link".equals(dmd.getCommandArray()[i])) {
					if (dmd.getCommandArray().length - 1 > i) {
						linkUrl = dmd.getCommandArray()[i + 1];
					} else {
						DiscordUtil.sendMessage("紐付けたいデイコードのURLを指定してください。", dmd);
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
				DiscordUtil.sendMessage(roleName + "のロールが見つかりません。", dmd);
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
					webhookChannel = (ServerTextChannel) serverChannel;
					webhookUrl = DiscordUtil.getWebhookUrl(dmd, webhookChannel);
					break;
				}
			}
			if (webhookUrl == null) {
				DiscordUtil.sendMessage(webhookChannelName + "のテキストチャンネルが見つかりません。", dmd);
			}

		} else {
			webhookUrl = DiscordUtil.getWebhookUrl(dmd);
		}

		if (webhookUrl == null) {
			return;
		}

		String guildId = dmd.getGuild().getIdAsString();
		String serverName = dmd.getGuild().getName();
		User authorUser = dmd.getUser();
		String authorName = authorUser.getNickname(dmd.getGuild()).orElse(authorUser.getDisplayName(dmd.getGuild()));
		String authorIdName = authorUser.getIdAsString() + ":" + authorName;

		ServerTextChannel stc = null;
		if (dmd.getCommandArray()[0].equals("/スケジュールforCh")
				&& dmd.getChannel() instanceof ServerTextChannel) {
			guildId += "#" + dmd.getChannel().getIdAsString();
			stc = (ServerTextChannel) dmd.getChannel();
			serverName += "#" + stc.getName();
		}

		Map<String, String> memberMap = DiscordUtil.getMemberIdMap(dmd.getGuild(), stc, role);

		logger.info("guildId:" + dmd.getGuild().getIdAsString() +
				" member count:" + memberMap.size() +
				" shard num:" + dmd.getGuild().getApi().getCurrentShard());

		if (memberMap.isEmpty()) {
			// メンバーの取得に失敗。再度コマンドを実行してもらうようにメッセージを送信。
			DiscordUtil.sendMessage("メンバー情報の取得に失敗しました。時間をおいて再度コマンドを実行してください。", dmd);
			return;
		}
		List<String> userIdNameList = new ArrayList<>();
		for (Entry<String, String> entry : memberMap.entrySet()) {
			userIdNameList.add(entry.getKey() + ":" + entry.getValue());
		}

		String roleId = null;
		if (role != null) {
			roleId = role.getIdAsString();
			guildId += "&" + roleId;
			serverName += "(" + roleName + ")";
		}

		Map<String, Object> result = null;
		if (linkUrl == null) {
			ScheduleCommandData scheduleData = new ScheduleCommandData();
			scheduleData.setGuildId(guildId);
			scheduleData.setServerName(serverName);
			scheduleData.setWebhook(webhookUrl);
			scheduleData.setAuthorIdName(authorIdName);
			scheduleData.setUserIdNameList(userIdNameList);
			scheduleData.setRoleId(roleId);
			if (dmd.getInteraction() != null) {
				scheduleData.setSlashCommand(true);
			}

			if ("-force".equals(dmd.getCommandArray()[dmd.getCommandArray().length - 1])) {
				// 末尾が-forceの場合、サーバオーナーかどうかを確認する。
				User guildOwner = dmd.getGuild().getOwner().orElse(null);
				if (!authorUser.getIdAsString().equals(guildOwner.getIdAsString())) {
					DiscordUtil.sendMessage("権限上書きができるのは、Discordサーバの管理者だけです。", dmd);
					return;
				}
				scheduleData.setForce(true);

			}

			result = CsmpService.getInstance().createScheduleAdjustment(scheduleData);
		} else {
			int keyStartIndex = linkUrl.indexOf("key=");
			if (keyStartIndex < 0) {
				DiscordUtil.sendMessage("入力されたURLがデイコードのURLではありません。", dmd);
				return;
			}

			int keyEndIndex = linkUrl.indexOf("&", keyStartIndex);
			if (keyEndIndex < 0) {
				keyEndIndex = linkUrl.length();
			}
			String linkKey = linkUrl.substring(keyStartIndex + 4, keyEndIndex);
			result = CsmpService.getInstance().linkScheduleAdjustment(guildId, webhookUrl, authorIdName, roleId,
					linkKey);
		}

		if (result == null) {
			DiscordUtil.sendMessage("エラーが発生しました。再度コマンドを実行してください。", dmd);
		} else if (!"ok".equals(result.get("result"))) {
			DiscordUtil.sendMessage(String.valueOf(result.get("error")), dmd);
		} else {
			if (result.containsKey("url")) {

				String message = "デイコードを設定しました。下記URLから調整する候補日の日程を入力し、日程登録ボタンを押してください。\n"
						+ "日程登録ボタンを押した際に日程状況一覧ページをDiscordへ通知することで、他の参加者に日程調整用のページを通知できます。\n"
						+ "[候補日設定用URL](" + result.get("url") + " )";

				DiscordUtil.sendMessage(message, dmd);
			}
		}

	}

	@Override
	public void warning(DiscordMessageData dmd) {
	}

	@Override
	public CommandHelpData getCommandHelpData() {
		List<CommandHelpData> list = new ArrayList<>();
		list.add((new CommandHelpData("/schedule",
				"日程調整用のページを作成する。以下の/スケジュールで始まるコマンドは旧コマンド。")));

		list.add(new CommandHelpData("/スケジュール",
				"日程調整用のページを作成する。",
				"作成したページURLがチャンネルに通知される。"));
		list.add(new CommandHelpData("/スケジュールforCh",
				"コマンドを発行したチャンネル単位に日程調整用のページを作成する。",
				"コマンドは「/スケジュールforCh」固定。"));
		list.add(new CommandHelpData("/スケジュール <通知先チャンネル名>",
				"日程調整用のページを作成する。",
				"作成したページURLが通知先チャンネルに通知される。（通知先チャンネル名がtestなら 「/スケジュール test」）"));
		list.add(new CommandHelpData("/スケジュールforCh <通知先チャンネル名>",
				"コマンドを発行したチャンネル単位に日程調整用のページを作成する。",
				"作成したページURLが通知先チャンネルに通知される。（通知先チャンネル名がtestなら 「/スケジュールforCh test」）"));
		list.add(new CommandHelpData("/スケジュール -role <ロール名>",
				"日程調整用のページを作成する。",
				"ロール名で指定したロールを持つユーザのみを参加者として登録する。「-role <ロール名>」は他のコマンドとの組み合わせも可能。"
						+ "-role以降は一番最後に指定する。「/スケジュールforCh <通知先チャンネル名> -role <ロール名>」など。"));

		return new CommandHelpData(list);
	}

	@Override
	public SlashCommandBuilder entryCommand() {
		SlashCommandOption createUnit = SlashCommandOption.createWithChoices(
				SlashCommandOptionType.STRING, "作成単位",
				"スケジュール調整用ページを作成する単位を選択します。", true,
				Arrays.asList(
						SlashCommandOptionChoice.create("チャンネル", CreateUnit.CHANNEL.name()),
						SlashCommandOptionChoice.create("サーバー", CreateUnit.SERVER.name()),
						SlashCommandOptionChoice.create("スレッド", CreateUnit.THREAD.name())));

		SlashCommandOption role = SlashCommandOption.create(
				SlashCommandOptionType.ROLE, "ロール指定",
				"指定したロールに所属するメンバーだけで日程調整したい場合に指定してください。", false);

		SlashCommandOption channel = SlashCommandOption.create(
				SlashCommandOptionType.CHANNEL, "通知先チャンネル",
				"通知先のチャンネルを変更したい場合に設定してください。", false);

		SlashCommandOption link = SlashCommandOption.create(
				SlashCommandOptionType.STRING, "紐付けurl",
				"まだDiscordと紐付けていない日程調整用のページをこのチャンネルに紐付けたい場合にURLを指定してください。", false);

		SlashCommandOption force = SlashCommandOption.createWithChoices(
				SlashCommandOptionType.STRING, "権限上書き",
				"他の人が作成した日程調整用のページを自分の権限で上書きしたい場合は指定してください。(サーバー管理者限定オプション）", false,
				Arrays.asList(
						SlashCommandOptionChoice.create("上書きする", CreateForce.FORCE.name())
						)
				);

		SlashCommandOption noMember = SlashCommandOption.createWithChoices(
				SlashCommandOptionType.STRING, "メンバーなし",
				"自分以外のメンバーを追加せずにスケジュールを作成します。メンバーを手動で追加したい場合に設定してください。", false,
				Arrays.asList(
						SlashCommandOptionChoice.create("メンバーなし", CreateMembers.NO_MEMBER.name())
						)
				);


		return new SlashCommandBuilder().setName(getCommandName())
				.setDescription("デイコードでスケジュール調整用のページを作成します。")
				.addOption(createUnit)
				.addOption(role)
				.addOption(channel)
				.addOption(link)
				.addOption(force)
				.addOption(noMember)
				;
	}

	public enum CreateUnit {
		CHANNEL, SERVER, THREAD
	}

	public enum CreateForce {
		FORCE, NOT_FORCE
	}

	public enum CreateMembers {
		NO_MEMBER, MEMBERS
	}

	@Override
	public String getCommandName() {
		return "schedule";
	}

	@Override
	public void executeSlashCommand(DiscordMessageData dmd) throws InterruptedException, ExecutionException {
		SlashCommandInteraction interaction = dmd.getInteraction();
		List<SlashCommandInteractionOption> options = interaction.getOptions();

		Map<String, SlashCommandInteractionOption> optionMap = new HashMap<>();
		for (SlashCommandInteractionOption option : options) {
			optionMap.put(option.getName(), option);
		}

		ServerTextChannel webhookChannel = null;
		SlashCommandInteractionOption noticeChannelOption = optionMap.get("通知先チャンネル");
		if (noticeChannelOption != null) {
			ServerChannel noticeChannel = noticeChannelOption.getChannelValue().orElse(null);
			if (noticeChannel != null && noticeChannel instanceof ServerTextChannel) {
				webhookChannel = (ServerTextChannel) noticeChannel;
			}
		}

		Role role = null;
		SlashCommandInteractionOption roleOption = optionMap.get("ロール指定");
		if (roleOption != null) {
			role = roleOption.getRoleValue().orElse(null);
		}

		String linkUrl = null;
		SlashCommandInteractionOption linkOption = optionMap.get("紐付けurl");
		if (linkOption != null) {
			linkUrl = linkOption.getStringValue().orElse(null);
		}

		boolean isForce = false;
		SlashCommandInteractionOption forceOption = optionMap.get("権限上書き");
		if (forceOption != null) {
			String createForceValue = forceOption.getStringValue().orElse(null);
			CreateForce forceType = CreateForce.valueOf(createForceValue);

			if (forceType == CreateForce.FORCE) {
				isForce = true;
			}
		}

		boolean isNoMember = false;
		SlashCommandInteractionOption noMemberOption = optionMap.get("メンバーなし");
		if (noMemberOption != null) {
			String createNoMemberValue = noMemberOption.getStringValue().orElse(null);
			CreateMembers membersType = CreateMembers.valueOf(createNoMemberValue);

			if (membersType == CreateMembers.NO_MEMBER) {
				isNoMember = true;
			}
		}

		String guildId = dmd.getGuild().getIdAsString();
		String serverName = dmd.getGuild().getName();
		User authorUser = dmd.getUser();
		String authorName = authorUser.getNickname(dmd.getGuild()).orElse(authorUser.getDisplayName(dmd.getGuild()));
		String authorIdName = authorUser.getIdAsString() + ":" + authorName;

		// 作成単位の確定
		SlashCommandInteractionOption createUnitOption = optionMap.get("作成単位");
		String createUnitValue = createUnitOption.getStringValue().get();
		CreateUnit createUnit = CreateUnit.valueOf(createUnitValue);

		// スレッド、サーバテキストそれぞれの処理
		ServerChannel channel = (ServerChannel) dmd.getChannel();
		ServerTextChannel textChannel = null;
		ServerForumChannel forumChannel = null;
		String threadId = null;
		String webhookUrl = null;

		// serverTextChannelであればそのまま取得する。
		textChannel = channel.asServerTextChannel().orElse(null);
		ServerThreadChannel threadChannel = channel.asServerThreadChannel().orElse(null);

		if (threadChannel != null) {
			textChannel = threadChannel.getParent().asServerTextChannel().orElse(null);
			forumChannel = threadChannel.getParent().asServerForumChannel().orElse(null);
			threadId = threadChannel.getIdAsString();
		}

		Map<String, String> memberMap = null;
		if (isNoMember) {
			memberMap = new HashMap<>();
			memberMap.put(authorUser.getIdAsString(), authorName);
		} else if (textChannel != null) {
			memberMap = DiscordUtil.getMemberIdMap(dmd.getGuild(), textChannel, role);
		} else if (forumChannel != null) {
			memberMap = DiscordUtil.getMemberIdMap(dmd.getGuild(), forumChannel, role);
		}

		switch (createUnit) {
		case THREAD:
			if (threadId == null) {
				DiscordUtil.sendMessage("スレッド単位で実施する場合、スレッド内でコマンドを実行してください。", dmd, true);
				return;
			}
			if (textChannel != null) {
				guildId += "#" + textChannel.getIdAsString() + "/" + threadId;
				serverName += "#" + textChannel.getName() + "/" + threadChannel.getName();
			} else if (forumChannel != null) {
				guildId += "#" + forumChannel.getIdAsString() + "/" + threadId;
				serverName += "#" + forumChannel.getName() + "/" + threadChannel.getName();
			}

			break;
		case CHANNEL:
			if (textChannel == null) {
				DiscordUtil.sendMessage("チャンネル単位で実施する場合、テキストチャンネル内でコマンドを実行してください。", dmd, true);
				return;
			}
			guildId += "#" + textChannel.getIdAsString();
			serverName += "#" + textChannel.getName();

			break;
		case SERVER:
			if (textChannel == null) {
				DiscordUtil.sendMessage("サーバー単位で実施する場合、テキストチャンネル内でコマンドを実行してください。", dmd, true);
				return;
			}
			break;
		default:
			break;
		}

		if (webhookChannel != null) {
			// 通知先チャンネルを別にする。
			webhookUrl = DiscordUtil.getWebhookUrl(dmd, webhookChannel);
		} else {
			if (createUnit == CreateUnit.THREAD) {
				if (forumChannel != null) {
					webhookUrl = DiscordUtil.getWebhookUrl(dmd, forumChannel, threadId);
				} else {
					webhookUrl = DiscordUtil.getWebhookUrl(dmd, textChannel, threadId);
				}
			} else {
				webhookUrl = DiscordUtil.getWebhookUrl(dmd);
			}
		}

		if (webhookUrl == null) {
			return;
		}

		logger.info("guildId:" + dmd.getGuild().getIdAsString() +
				" member count:" + memberMap.size() +
				" shard num:" + dmd.getGuild().getApi().getCurrentShard());

		if (memberMap.isEmpty()) {
			// メンバーの取得に失敗。再度コマンドを実行してもらうようにメッセージを送信。
			DiscordUtil.sendMessage("メンバー情報の取得に失敗しました。時間をおいて再度コマンドを実行してください。", dmd);
			return;
		}
		List<String> userIdNameList = new ArrayList<>();
		for (Entry<String, String> entry : memberMap.entrySet()) {
			userIdNameList.add(entry.getKey() + ":" + entry.getValue());
		}

		String roleId = null;
		if (role != null) {
			roleId = role.getIdAsString();
			guildId += "&" + roleId;
			serverName += "(" + role.getName() + ")";
		}

		Map<String, Object> result = null;
		if (linkUrl == null) {
			ScheduleCommandData scheduleData = new ScheduleCommandData();
			scheduleData.setGuildId(guildId);
			scheduleData.setServerName(serverName);
			scheduleData.setWebhook(webhookUrl);
			scheduleData.setAuthorIdName(authorIdName);
			scheduleData.setUserIdNameList(userIdNameList);
			scheduleData.setRoleId(roleId);
			if (dmd.getInteraction() != null) {
				scheduleData.setSlashCommand(true);
			}

			if (isForce) {
				// 末尾が-forceの場合、サーバオーナーかどうかを確認する。
				User guildOwner = dmd.getGuild().getOwner().orElse(null);
				if (!authorUser.getIdAsString().equals(guildOwner.getIdAsString())) {
					DiscordUtil.sendMessage("権限上書きができるのは、Discordサーバの管理者だけです。", dmd);
					return;
				}
				scheduleData.setForce(true);

			}

			result = CsmpService.getInstance().createScheduleAdjustment(scheduleData);
		} else {
			int keyStartIndex = linkUrl.indexOf("key=");
			if (keyStartIndex < 0) {
				DiscordUtil.sendMessage("入力されたURLがデイコードのURLではありません。", dmd);
				return;
			}

			int keyEndIndex = linkUrl.indexOf("&", keyStartIndex);
			if (keyEndIndex < 0) {
				keyEndIndex = linkUrl.length();
			}
			String linkKey = linkUrl.substring(keyStartIndex + 4, keyEndIndex);
			result = CsmpService.getInstance().linkScheduleAdjustment(guildId, webhookUrl, authorIdName, roleId,
					linkKey);
		}

		if (result == null) {
			DiscordUtil.sendMessage("エラーが発生しました。再度コマンドを実行してください。", dmd);
		} else if (!"ok".equals(result.get("result"))) {
			DiscordUtil.sendMessage(String.valueOf(result.get("error")), dmd);
		} else {
			if (result.containsKey("url")) {

				String message = "デイコードを設定しました。下記URLから調整する候補日の日程を入力し、日程登録ボタンを押してください。\n"
						+ "日程登録ボタンを押した際に日程状況一覧ページをDiscordへ通知することで、他の参加者に日程調整用のページを通知できます。\n"
						+ "[候補日設定用URL](" + result.get("url") + " )";

				DiscordUtil.sendMessage(message, dmd);
			}
		}
	}
}
