package csmp.bot.command.sgs;

import java.lang.invoke.MethodHandles;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.channel.ChannelCategory;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.ServerTextChannelBuilder;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.PermissionsBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.server.ServerUpdater;
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
import csmp.service.CsmpService;
import csmp.utl.DateUtil;
import csmp.utl.DiscordUtil;

/**
 * シナリオ情報セットアップコマンド.
 * @author kgmas
 *
 */
public class ScenarioSetupCommand implements IDiscordCommand, IDiscordSlashCommand {

	/**
	 * ロガー
	 */
	private static Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	public boolean judgeExecute(DiscordMessageData dmd) {
		if (dmd.getGuild() == null) {
			return false;
		}
		if (dmd.getText().startsWith("/sgss ") || dmd.getText().startsWith("/sgsread ")) {
			return true;
		}
		return false;
	}

	@Override
	public boolean checkInput(DiscordMessageData dmd) {
		if (dmd.getCommandArray().length < 2) {
			return false;
		}

		return true;
	}

	@Override
	public void execute(DiscordMessageData dmd) throws InterruptedException, ExecutionException {
		String categoryName = "";
		if (dmd.getCommandArray().length >= 3) {
			// カテゴリ指定。カテゴリ名を取得する。
			categoryName = dmd.getCommandArray()[2];
		}

		Server guild = dmd.getGuild();
		CsmpService csmpService = CsmpService.getInstance();
		Map<Long, Map<Object,Object>> guildScenarioInfo = null;
		if (categoryName.isEmpty()) {
			guildScenarioInfo = csmpService.getGuildScenarioInfo();

			if (guildScenarioInfo.containsKey(guild.getId())) {
				DiscordUtil.sendMessage("このサーバーは既にシナリオが登録されています。「/sgsclear」コマンドを使ってシナリオをクリアしてください。", dmd);
				return;
			}

		}
		// シナリオ情報取得
		Map<Object, Object> secretMap = csmpService.getScenarioSheetInfo(dmd.getCommandArray()[1]);
		if (secretMap == null) {
			DiscordUtil.sendMessage("シナリオシートのURLが誤っているか、公開中にチェックが入っていません。", dmd);
			return;
		}
		String scenarioName = (String)((Map<String, Object>)secretMap.get("base")).get("name");
		// シナリオ名のメッセージ出力
		DiscordUtil.sendMessage("シナリオ：[" + scenarioName + "](" + dmd.getCommandArray()[1].replace("edit.html", "detail") + ")", dmd, false);

		if (categoryName.isEmpty()) {
			guildScenarioInfo.put(guild.getId(), secretMap);
		}
		logger.info("シナリオ名：" + scenarioName);
		if ("/sgsread".equals(dmd.getCommandArray()[0])) {
			return;
		}

		Date current = Calendar.getInstance().getTime();
		SimpleDateFormat sdf = DateUtil.getDateFormat();


		ChannelCategory category = null;
		if (categoryName.isEmpty()) {
			new ServerUpdater(guild).setName(scenarioName + "_" + sdf.format(current)).update();
		} else {
			List<ChannelCategory> channelCategories = dmd.getGuild().getChannelCategories();
			for (ChannelCategory channelCategory : channelCategories) {
				if (categoryName.equals(channelCategory.getName())) {
					category = channelCategory;
				}
			}

			if (category == null) {
				category = dmd.getGuild().createChannelCategoryBuilder().setName(categoryName).create().join();
			}
		}

		Map<String, Role> roleMap = new HashMap<>();
		for (Role role : guild.getRoles()) {
			roleMap.put(role.getName().toLowerCase(), role);
		}

		Map<String, ServerTextChannel> channelMap = new HashMap<>();
		if (category != null) {
			category.getChannels().forEach(action -> {
				action.asServerTextChannel().ifPresent(channel ->
				channelMap.put(channel.getName().toLowerCase(), channel));
			});

		} else {
			List<ServerTextChannel> channels = guild.getTextChannels();
			for (ServerTextChannel channel : channels) {
				channelMap.put(channel.getName().toLowerCase(), channel);
			}

		}

		// PC情報の取得
		List<Map<String, Object>> pcList = (List<Map<String, Object>>)secretMap.get("pc");
		for (Map<String, Object> pcInfo : pcList) {
			String roleName = "PC" + CsmpService.text(pcInfo, "name");
			String channelName = roleName;
			if (!categoryName.isEmpty()) {
				// ロール名はカテゴリつき、チャンネル名はカテゴリなし。
				roleName = categoryName + "_" + roleName;
			}

			Role role = roleMap.get(roleName.toLowerCase());
			if (role == null) {
				role = guild.createRoleBuilder()
						.setName(roleName)
						.create().get();
				roleMap.put(roleName.toLowerCase(), role);
			}


			ServerTextChannel tc = channelMap.get(channelName.toLowerCase());

			if (tc == null) {
				ServerTextChannelBuilder stcBuilder = new ServerTextChannelBuilder(guild)
						.setName(channelName)
						.addPermissionOverwrite(
								guild.getEveryoneRole(), new PermissionsBuilder().setDenied(PermissionType.VIEW_CHANNEL).build())
						.addPermissionOverwrite(
								role, new PermissionsBuilder().setAllowed(PermissionType.VIEW_CHANNEL).build());

				if (category != null) {
					stcBuilder.setCategory(category);
				} else {
					dmd.getCategory().ifPresent(ctg -> stcBuilder.setCategory(ctg));
				}


				tc = stcBuilder.create().get();

				channelMap.put(channelName.toLowerCase(), tc);
			}

			String textMessage = "■" + channelName + "　推奨：" +CsmpService.text(pcInfo, "recommend") + "\r\n" +
					"・使命：【" +CsmpService.text(pcInfo, "mission") + "】" + "\r\n" +
					"・導入：\r\n" +
					CsmpService.text(pcInfo, "intro") + "\r\n" +
					"・秘密：\r\n" +
					CsmpService.text(pcInfo, "secret");

			tc.sendMessage(textMessage);
		}
	}

	@Override
	public void warning(DiscordMessageData dmd) {
		dmd.getChannel().sendMessage("コマンドは「/sgss <シナリオシートURL>」と入力してください。");
	}

	@Override
	public CommandHelpData getCommandHelpData() {
		List<CommandHelpData> list = new ArrayList<>();
		list.add(new CommandHelpData(
				"/sgsread <シナリオシートURL>",
				"シナリオシートの情報の読み込みだけ行う。"));
		list.add(new CommandHelpData(
				"/sgss <シナリオシートURL>",
				"シナリオシートの情報を読み込んでチャンネル、権限を設定する。"));

		return new CommandHelpData(list);
	}

	@Override
	public SlashCommandBuilder entryCommand() {
		SlashCommandOption link = SlashCommandOption.create(
				SlashCommandOptionType.STRING, "シナリオシートurl",
				"キャラクターシート倉庫に登録しているシナリオシートのURLを指定してください。", true);

		SlashCommandOption category = SlashCommandOption.create(
				SlashCommandOptionType.STRING, "カテゴリ名",
				"指定されたカテゴリを作成し、その配下に各PC用のチャンネルを作成します。指定した名前のカテゴリが存在する場合はその配下に作成します。", false);

		SlashCommandOption readonly = SlashCommandOption.createWithChoices(
				SlashCommandOptionType.STRING, "読み込み専用",
				"シナリオシートの読み込みだけを行い、チャンネルやロールの設定を行わない場合に指定してください。", false,
				Arrays.asList(
						SlashCommandOptionChoice.create("読み込み専用", ReadOnly.READONLY.name())
						)
				);

		return new SlashCommandBuilder().setName(getCommandName())
				.setDescription("シノビガミのシナリオシートを読み込み、PCごとのチャンネルとロールを作成し、秘密を配付します。")
				.addOption(link)
				.addOption(category)
				.addOption(readonly)
				;
	}

	public enum ReadOnly {
		READONLY, NOT_READONLY
	}

	@Override
	public String getCommandName() {
		return "sgss";
	}

	@Override
	public void executeSlashCommand(DiscordMessageData dmd) throws InterruptedException, ExecutionException {
		SlashCommandInteraction interaction = dmd.getInteraction();
		List<SlashCommandInteractionOption> options = interaction.getOptions();

		String commandText = "/sgss ";

		Map<String, SlashCommandInteractionOption> optionMap = new HashMap<>();
		for (SlashCommandInteractionOption option : options) {
			optionMap.put(option.getName(), option);
		}

		// シナリオシートURL
		SlashCommandInteractionOption urlOption = optionMap.get("シナリオシートurl");
		String url = urlOption.getStringValue().orElse(null);

		// 読み込み専用オプション
		SlashCommandInteractionOption readOnlyOption = optionMap.get("読み込み専用");
		if (readOnlyOption != null) {
			String readOnlyValue = readOnlyOption.getStringValue().orElse(null);
			ReadOnly readOnly = ReadOnly.valueOf(readOnlyValue);

			if (readOnly == ReadOnly.READONLY) {
				commandText = "/sgsread ";
			}

		}

		dmd.setText(commandText + url);

		// 読み込み専用オプション
		SlashCommandInteractionOption categoryOption = optionMap.get("カテゴリ名");
		if (categoryOption != null) {
			categoryOption.getStringValue().ifPresent(category -> {
				dmd.setText(dmd.getText() + " " + category);
			});
		}

		execute(dmd);

	}
}
