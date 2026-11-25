package csmp.bot.command.schedule;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import csmp.bot.command.ISlashCommand;
import csmp.bot.model.CommandHelpData;
import csmp.bot.model.ScheduleCommandData;
import csmp.service.CsmpService;
import csmp.utl.DiscordUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.attribute.IWebhookContainer;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

/**
 * スケジュール追加コマンド.
 * @author kgmas
 *
 */
public class ScheduleCreateCommand implements ISlashCommand {

	/**
	 * ロガー
	 */
	private static Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private static final String OPTION_UNIT = "作成単位";
	private static final String OPTION_ROLE = "ロール指定";
	private static final String OPTION_CHANNEL = "通知先チャンネル";
	private static final String OPTION_LINK = "紐付けurl";
	private static final String OPTION_FORCE = "権限上書き";
	private static final String OPTION_NO_MEMBER = "メンバーなし";

	private static final String THREAD_ONLY_MESSAGE =
			"フォーラムチャンネルやテキストチャンネルのスレッド内でコマンドを実行する場合、作成単位をスレッドにしてください。\n"
			+ "ボイスチャンネルのチャットなどでは実行できません。";

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
	public CommandHelpData getCommandHelpData() {
		return new CommandHelpData("/schedule",
				"日程調整用のページを作成する。",
				"作成単位（チャンネル・サーバー・スレッド）、ロール、通知先チャンネルなどを指定できる。");
	}

	@Override
	public SlashCommandData createCommandData() {
		OptionData createUnit = new OptionData(OptionType.STRING, OPTION_UNIT,
				"スケジュール調整用ページを作成する単位を選択します。", true)
				.addChoice("チャンネル", CreateUnit.CHANNEL.name())
				.addChoice("サーバー", CreateUnit.SERVER.name())
				.addChoice("スレッド", CreateUnit.THREAD.name());
		OptionData role = new OptionData(OptionType.ROLE, OPTION_ROLE,
				"指定したロールに所属するメンバーだけで日程調整したい場合に指定してください。", false);
		OptionData channel = new OptionData(OptionType.CHANNEL, OPTION_CHANNEL,
				"通知先のチャンネルを変更したい場合に設定してください。", false);
		OptionData link = new OptionData(OptionType.STRING, OPTION_LINK,
				"まだDiscordと紐付けていない日程調整用のページをこのチャンネルに紐付けたい場合にURLを指定してください。", false);
		OptionData force = new OptionData(OptionType.STRING, OPTION_FORCE,
				"他の人が作成した日程調整用のページを自分の権限で上書きしたい場合は指定してください。(サーバー管理者限定オプション）", false)
				.addChoice("上書きする", CreateForce.FORCE.name());
		OptionData noMember = new OptionData(OptionType.STRING, OPTION_NO_MEMBER,
				"自分以外のメンバーを追加せずにスケジュールを作成します。メンバーを手動で追加したい場合に設定してください。", false)
				.addChoice("メンバーなし", CreateMembers.NO_MEMBER.name());

		return Commands.slash(getCommandName(), "デイコードでスケジュール調整用のページを作成します。")
				.setContexts(InteractionContextType.GUILD)
				.addOptions(createUnit, role, channel, link, force, noMember);
	}

	@Override
	public String getCommandName() {
		return "schedule";
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		// webhookの取得やデイコードへの登録に時間がかかるため、先に応答を保留する。
		event.deferReply(true).queue();
		InteractionHook hook = event.getHook();
		Guild guild = event.getGuild();

		StandardGuildMessageChannel webhookChannel = null;
		OptionMapping noticeChannelOption = event.getOption(OPTION_CHANNEL);
		if (noticeChannelOption != null
				&& noticeChannelOption.getAsChannel() instanceof StandardGuildMessageChannel noticeChannel) {
			webhookChannel = noticeChannel;
		}

		OptionMapping roleOption = event.getOption(OPTION_ROLE);
		Role role = roleOption != null ? roleOption.getAsRole() : null;

		OptionMapping linkOption = event.getOption(OPTION_LINK);
		String linkUrl = linkOption != null ? linkOption.getAsString() : null;

		OptionMapping forceOption = event.getOption(OPTION_FORCE);
		boolean isForce = forceOption != null
				&& CreateForce.valueOf(forceOption.getAsString()) == CreateForce.FORCE;

		OptionMapping noMemberOption = event.getOption(OPTION_NO_MEMBER);
		boolean isNoMember = noMemberOption != null
				&& CreateMembers.valueOf(noMemberOption.getAsString()) == CreateMembers.NO_MEMBER;

		String guildId = guild.getId();
		String serverName = guild.getName();
		User authorUser = event.getUser();
		String authorName = DiscordUtil.getDisplayName(event);
		String authorIdName = authorUser.getId() + ":" + authorName;

		// 作成単位の確定
		CreateUnit createUnit = CreateUnit.valueOf(event.getOption(OPTION_UNIT).getAsString());

		// スレッド、サーバテキストそれぞれの処理
		GuildChannel channel = event.getGuildChannel();
		StandardGuildMessageChannel textChannel = null;
		ForumChannel forumChannel = null;
		ThreadChannel threadChannel = null;
		String threadId = null;

		// テキストチャンネルであればそのまま取得する。
		if (channel instanceof StandardGuildMessageChannel standardChannel) {
			textChannel = standardChannel;
		}
		if (channel instanceof ThreadChannel thread) {
			threadChannel = thread;
			threadId = thread.getId();
			if (thread.getParentChannel() instanceof StandardGuildMessageChannel parentText) {
				textChannel = parentText;
			} else if (thread.getParentChannel() instanceof ForumChannel parentForum) {
				forumChannel = parentForum;
			}
		}

		Map<String, String> memberMap = null;
		if (isNoMember) {
			memberMap = new HashMap<>();
			memberMap.put(authorUser.getId(), authorName);
		} else if (textChannel != null) {
			memberMap = DiscordUtil.getMemberIdMap(guild, textChannel, role);
		} else if (forumChannel != null) {
			memberMap = DiscordUtil.getMemberIdMap(guild, forumChannel, role);
		}

		switch (createUnit) {
		case THREAD:
			if (threadId == null) {
				DiscordUtil.sendMessage(hook, "スレッド単位で実施する場合、スレッド内でコマンドを実行してください。");
				return;
			}
			if (textChannel != null) {
				guildId += "#" + textChannel.getId() + "/" + threadId;
				serverName += "#" + textChannel.getName() + "/" + threadChannel.getName();
			} else if (forumChannel != null) {
				guildId += "#" + forumChannel.getId() + "/" + threadId;
				serverName += "#" + forumChannel.getName() + "/" + threadChannel.getName();
			}
			break;
		case CHANNEL:
			if (textChannel == null) {
				DiscordUtil.sendMessage(hook, "チャンネル単位で実施する場合、テキストチャンネル内でコマンドを実行してください。");
				return;
			}
			if (threadChannel != null) {
				DiscordUtil.sendMessage(hook, THREAD_ONLY_MESSAGE);
				return;
			}
			guildId += "#" + textChannel.getId();
			serverName += "#" + textChannel.getName();
			break;
		case SERVER:
			if (textChannel == null) {
				DiscordUtil.sendMessage(hook, "サーバー単位で実施する場合、テキストチャンネル内でコマンドを実行してください。");
				return;
			}
			if (threadChannel != null) {
				DiscordUtil.sendMessage(hook, THREAD_ONLY_MESSAGE);
				return;
			}
			break;
		default:
			break;
		}

		String webhookUrl = null;
		if (webhookChannel != null) {
			// 通知先チャンネルを別にする。
			webhookUrl = DiscordUtil.getWebhookUrl(hook, webhookChannel);
		} else if (createUnit == CreateUnit.THREAD) {
			IWebhookContainer parent = forumChannel != null ? forumChannel : textChannel;
			if (parent == null) {
				DiscordUtil.sendMessage(hook, "このスレッドでは実行できません。");
				return;
			}
			webhookUrl = DiscordUtil.getWebhookUrl(hook, parent, threadId);
		} else {
			webhookUrl = DiscordUtil.getWebhookUrl(hook, textChannel);
		}
		if (webhookUrl == null) {
			return;
		}

		if (memberMap == null || memberMap.isEmpty()) {
			// メンバーの取得に失敗。再度コマンドを実行してもらうようにメッセージを送信。
			DiscordUtil.sendMessage(hook, "メンバー情報の取得に失敗しました。時間をおいて再度コマンドを実行してください。");
			return;
		}
		logger.info("guildId:" + guild.getId() +
				" member count:" + memberMap.size() +
				" shard num:" + event.getJDA().getShardInfo().getShardId());

		List<String> userIdNameList = new ArrayList<>();
		for (Entry<String, String> entry : memberMap.entrySet()) {
			userIdNameList.add(entry.getKey() + ":" + entry.getValue());
		}

		String roleId = null;
		if (role != null) {
			roleId = role.getId();
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
			scheduleData.setSlashCommand(true);

			if (isForce) {
				// 権限上書きは、サーバオーナーのみ可能。
				if (!authorUser.getId().equals(guild.getOwnerId())) {
					DiscordUtil.sendMessage(hook, "権限上書きができるのは、Discordサーバの管理者だけです。");
					return;
				}
				scheduleData.setForce(true);
			}

			result = CsmpService.getInstance().createScheduleAdjustment(scheduleData);
		} else {
			String linkKey = getLinkKey(linkUrl);
			if (linkKey == null) {
				DiscordUtil.sendMessage(hook, "入力されたURLがデイコードのURLではありません。");
				return;
			}
			result = CsmpService.getInstance().linkScheduleAdjustment(guildId, webhookUrl, authorIdName, roleId,
					linkKey);
		}

		if (result == null) {
			DiscordUtil.sendMessage(hook, "エラーが発生しました。再度コマンドを実行してください。");
		} else if (!"ok".equals(result.get("result"))) {
			DiscordUtil.sendMessage(hook, String.valueOf(result.get("error")));
		} else if (result.containsKey("url")) {
			String message = "デイコードを設定しました。下記URLから調整する候補日の日程を入力し、日程登録ボタンを押してください。\n"
					+ "日程登録ボタンを押した際に日程状況一覧ページをDiscordへ通知することで、他の参加者に日程調整用のページを通知できます。\n"
					+ "[候補日設定用URL](" + result.get("url") + " )";
			DiscordUtil.sendMessage(hook, message);
		} else {
			DiscordUtil.sendMessage(hook, "デイコードを設定しました。");
		}
	}

	/**
	 * デイコードのURLからキーを取り出す.
	 * @param linkUrl デイコードのURL
	 * @return キー. URLに key= が含まれない場合はnull.
	 */
	public static String getLinkKey(String linkUrl) {
		int keyStartIndex = linkUrl.indexOf("key=");
		if (keyStartIndex < 0) {
			return null;
		}
		int keyEndIndex = linkUrl.indexOf("&", keyStartIndex);
		if (keyEndIndex < 0) {
			keyEndIndex = linkUrl.length();
		}
		return linkUrl.substring(keyStartIndex + 4, keyEndIndex);
	}

}
