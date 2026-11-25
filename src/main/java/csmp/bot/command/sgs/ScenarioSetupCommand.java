package csmp.bot.command.sgs;

import java.lang.invoke.MethodHandles;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import csmp.bot.command.ISlashCommand;
import csmp.bot.model.CommandHelpData;
import csmp.service.CsmpService;
import csmp.utl.DateUtil;
import csmp.utl.DiscordUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.attribute.ICategorizableChannel;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;

/**
 * シナリオ情報セットアップコマンド.
 * @author kgmas
 *
 */
public class ScenarioSetupCommand implements ISlashCommand {

	/**
	 * ロガー
	 */
	private static Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private static final String OPTION_URL = "シナリオシートurl";
	private static final String OPTION_CATEGORY = "カテゴリ名";
	private static final String OPTION_READONLY = "読み込み専用";

	public enum ReadOnly {
		READONLY, NOT_READONLY
	}

	private static final EnumSet<Permission> VIEW = EnumSet.of(Permission.VIEW_CHANNEL);

	@SuppressWarnings("unchecked")
	@Override
	public void execute(SlashCommandInteractionEvent event) {
		String sheetUrl = event.getOption(OPTION_URL).getAsString().trim();
		OptionMapping categoryOption = event.getOption(OPTION_CATEGORY);
		String categoryName = categoryOption != null ? categoryOption.getAsString().trim() : "";
		OptionMapping readOnlyOption = event.getOption(OPTION_READONLY);
		boolean readOnly = readOnlyOption != null
				&& ReadOnly.valueOf(readOnlyOption.getAsString()) == ReadOnly.READONLY;

		// シナリオシートの取得やチャンネル作成に時間がかかるため、先に応答を保留する。
		event.deferReply(true).queue();
		InteractionHook hook = event.getHook();

		Guild guild = event.getGuild();
		CsmpService csmpService = CsmpService.getInstance();
		Map<Long, Map<Object,Object>> guildScenarioInfo = csmpService.getGuildScenarioInfo();
		if (categoryName.isEmpty() && guildScenarioInfo.containsKey(guild.getIdLong())) {
			DiscordUtil.sendMessage(hook, "このサーバーは既にシナリオが登録されています。「/sgsclear」コマンドを使ってシナリオをクリアしてください。");
			return;
		}

		// シナリオ情報取得
		Map<Object, Object> secretMap = csmpService.getScenarioSheetInfo(sheetUrl);
		if (secretMap == null) {
			DiscordUtil.sendMessage(hook, "シナリオシートのURLが誤っているか、公開中にチェックが入っていません。");
			return;
		}
		String scenarioName = (String)((Map<String, Object>)secretMap.get("base")).get("name");

		// シナリオ名のメッセージ出力
		String scenarioInfoMessage = "シナリオ：[" + scenarioName + "](" + sheetUrl.replace("edit.html", "detail") + ")";
		event.getChannel().sendMessage(scenarioInfoMessage).queue();

		if (categoryName.isEmpty()) {
			guildScenarioInfo.put(guild.getIdLong(), secretMap);
		}

		logger.info("シナリオ名：" + scenarioName);
		if (readOnly) {
			DiscordUtil.sendMessage(hook, "シナリオシートを読み込みました。");
			return;
		}

		Date current = Calendar.getInstance().getTime();
		SimpleDateFormat sdf = DateUtil.getDateFormat();

		// PC情報の取得
		List<Map<String, Object>> pcList = (List<Map<String, Object>>)secretMap.get("pc");

		// ロール設定
		Map<String, Role> roleMap = new HashMap<>();
		for (Role role : guild.getRoles()) {
			roleMap.put(role.getName().toLowerCase(), role);
		}

		Member member = event.getMember();
		List<Role> pcRoleList = new ArrayList<>();
		for (Map<String, Object> pcInfo : pcList) {
			String roleName = toRoleName(categoryName, pcInfo);
			Role role = roleMap.get(roleName.toLowerCase());
			if (role == null) {
				role = guild.createRole().setName(roleName).complete();
				roleMap.put(roleName.toLowerCase(), role);
			}
			pcRoleList.add(role);
			// コマンド実行ユーザにロールを付与
			guild.addRoleToMember(member, role).queue();
		}

		Category category = null;
		if (categoryName.isEmpty()) {
			guild.getManager().setName(scenarioName + "_" + sdf.format(current)).queue();
		} else {
			List<Category> categories = guild.getCategoriesByName(categoryName, false);
			if (!categories.isEmpty()) {
				category = categories.get(0);
			} else {
				ChannelAction<Category> categoryAction = guild.createCategory(categoryName);
				if (!pcRoleList.isEmpty()) {
					categoryAction = categoryAction.addPermissionOverride(guild.getPublicRole(), null, VIEW);
					for (Role role : pcRoleList) {
						categoryAction = categoryAction.addPermissionOverride(role, VIEW, null);
					}
				}
				category = categoryAction.complete();
				guild.createTextChannel("相談・調整", category).complete();
			}
		}

		// チャンネル設定
		Map<String, TextChannel> channelMap = new HashMap<>();
		List<TextChannel> channels = category != null ? category.getTextChannels() : guild.getTextChannels();
		for (TextChannel channel : channels) {
			channelMap.put(channel.getName().toLowerCase(), channel);
		}

		Category currentCategory = null;
		if (event.getGuildChannel() instanceof ICategorizableChannel categorizable) {
			currentCategory = categorizable.getParentCategory();
		}

		for (Map<String, Object> pcInfo : pcList) {
			String channelName = "PC" + CsmpService.text(pcInfo, "name");
			// ロール名はカテゴリつき、チャンネル名はカテゴリなし。
			Role role = roleMap.get(toRoleName(categoryName, pcInfo).toLowerCase());
			TextChannel tc = channelMap.get(channelName.toLowerCase());
			if (tc == null) {
				Category parent = category != null ? category : currentCategory;
				tc = guild.createTextChannel(channelName, parent)
						.addPermissionOverride(guild.getPublicRole(), null, VIEW)
						.addPermissionOverride(role, VIEW, null)
						.complete();
				channelMap.put(channelName.toLowerCase(), tc);
			}

			String textMessage = "■" + channelName + "　推奨：" +CsmpService.text(pcInfo, "recommend") + "\r\n" +
					"・使命：【" +CsmpService.text(pcInfo, "mission") + "】" + "\r\n" +
					"・導入：\r\n" +
					CsmpService.text(pcInfo, "intro") + "\r\n" +
					"・秘密：\r\n" +
					CsmpService.text(pcInfo, "secret");
			DiscordUtil.sendToChannel(tc, textMessage);
		}

		DiscordUtil.sendMessage(hook, "PCごとのロールとチャンネルを作成し、秘密を配付しました。");
	}

	private static String toRoleName(String categoryName, Map<String, Object> pcInfo) {
		String roleName = "PC" + CsmpService.text(pcInfo, "name");
		if (!categoryName.isEmpty()) {
			roleName = categoryName + "_" + roleName;
		}
		return roleName;
	}

	@Override
	public CommandHelpData getCommandHelpData() {
		List<CommandHelpData> list = new ArrayList<>();
		list.add(new CommandHelpData(
				"/sgss <シナリオシートURL>",
				"シナリオシートの情報を読み込んでチャンネル、権限を設定する。",
				"カテゴリ名を指定するとその配下に作成する。読み込み専用を指定すると読み込みだけ行う。"));
		return new CommandHelpData(list);
	}

	@Override
	public SlashCommandData createCommandData() {
		OptionData link = new OptionData(OptionType.STRING, OPTION_URL,
				"キャラクターシート倉庫に登録しているシナリオシートのURLを指定してください。", true);
		OptionData category = new OptionData(OptionType.STRING, OPTION_CATEGORY,
				"指定されたカテゴリを作成し、その配下に各PC用のチャンネルを作成します。指定した名前のカテゴリが存在する場合はその配下に作成します。", false);
		OptionData readonly = new OptionData(OptionType.STRING, OPTION_READONLY,
				"シナリオシートの読み込みだけを行い、チャンネルやロールの設定を行わない場合に指定してください。", false)
				.addChoice("読み込み専用", ReadOnly.READONLY.name());

		return Commands.slash(getCommandName(), "シノビガミのシナリオシートを読み込み、PCごとのチャンネルとロールを作成し、秘密を配付します。")
				.setContexts(InteractionContextType.GUILD)
				.addOptions(link, category, readonly);
	}

	@Override
	public String getCommandName() {
		return "sgss";
	}

}
