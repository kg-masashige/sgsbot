package csmp.bot;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import csmp.bot.command.ISlashCommand;
import csmp.bot.command.bcdice.BcDiceGetSystemInfoCommand;
import csmp.bot.command.bcdice.BcDiceRollCommand;
import csmp.bot.command.bcdice.BcDiceSearchSystemNameCommand;
import csmp.bot.command.bcdice.BcDiceSetSystemCommand;
import csmp.bot.command.bcdice.PlotOpenCommand;
import csmp.bot.command.bcdice.PlotSetCommand;
import csmp.bot.command.bcdice.TableSetupCommand;
import csmp.bot.command.character.LinkCharacterSheetCommand;
import csmp.bot.command.schedule.ScheduleCreateCommand;
import csmp.bot.command.sgs.ScenarioClearCommand;
import csmp.bot.command.sgs.ScenarioSendSecretCommand;
import csmp.bot.command.sgs.ScenarioSetupCommand;
import csmp.bot.controller.DiscordBotController;
import csmp.bot.controller.DiscordBotController.IntentType;
import csmp.bot.event.IDiscordEvent;
import csmp.bot.event.schedule.ScheduleMemberChangeEvent;
import csmp.service.BcDiceApiService;

public class DiscordSessionSupportBot {

	/**
	 * メイン.
	 * workerプロセスで指定。
	 * @param args
	 */
	public static void main(String[] args) {

		String startMessage = "デイコードはDiscordと連携して日程調整ができるサービスです。\n"
				+ "「/schedule」と入力するだけで、簡単にスケジュール調整ページを作成することができます。\n"
				+ "詳細は https://character-sheets.appspot.com/schedule/ をご確認ください。\r\n"
				+ "「/」を入力した際にコマンドのサジェストが表示されない場合、上記のサイトからbotを導入し直してください。";

		boolean daycord = "true".equals(System.getenv("DAYCORD_SERVICE"));
		boolean dicebot = "true".equals(System.getenv("DICEBOT_SERVICE"));
		boolean shinobigami = "true".equals(System.getenv("SHINOBIGAMI_SERVICE"));

		List<ISlashCommand> commandList = createCommandList(daycord, dicebot, shinobigami);
		if (dicebot) {
			// ダイスボット初期化
			BcDiceApiService.getInstance();
		}
		if (shinobigami) {
			startMessage = "TRPGセッション（主にシノビガミ）を行うためのbotです。\r\n"
					+ "「/sgshelp」で実行可能なコマンドを確認できます。\r\n"
					+ "詳細は https://github.com/kg-masashige/sgsbot をご確認ください。\r\n"
					+ "「/」を入力した際にコマンドのサジェストが表示されない場合、上記のサイトからbotを導入し直してください。";
		}

		String token = System.getenv("DISCORD_BOT_TOKEN");
		IntentType intentType = IntentType.of(System.getenv("INTENT_TYPE"));
		List<IDiscordEvent> daycordEventList = new ArrayList<>();
		daycordEventList.add(new ScheduleMemberChangeEvent());

		DiscordBotController daycordBot = new DiscordBotController(commandList, daycordEventList, token);
		// シャード分割用
		Optional.ofNullable(System.getenv("SHARD_TOTAL"))
				.ifPresent(value -> daycordBot.setTotalShards(Integer.parseInt(value)));
		Optional.ofNullable(System.getenv("SHARD_START_NO"))
				.ifPresent(value -> daycordBot.setStart(Integer.parseInt(value)));
		Optional.ofNullable(System.getenv("SHARD_END_NO"))
				.ifPresent(value -> daycordBot.setEnd(Integer.parseInt(value)));

		daycordBot.execute(startMessage, intentType);

	}

	/**
	 * 有効にするサービスに応じたコマンドの一覧を作成する.
	 * @param daycord デイコード
	 * @param dicebot ダイスボット
	 * @param shinobigami シノビガミ
	 * @return コマンドの一覧（ヘルプを除く）
	 */
	public static List<ISlashCommand> createCommandList(boolean daycord, boolean dicebot, boolean shinobigami) {
		List<ISlashCommand> commandList = new ArrayList<>();
		if (daycord) {
			commandList.add(new ScheduleCreateCommand());
		}
		if (dicebot) {
			commandList.add(new BcDiceRollCommand());
			commandList.add(new TableSetupCommand());
			commandList.add(new PlotSetCommand());
			commandList.add(new PlotOpenCommand());
			commandList.add(new LinkCharacterSheetCommand());
			commandList.add(new BcDiceSearchSystemNameCommand());
			commandList.add(new BcDiceSetSystemCommand());
			commandList.add(new BcDiceGetSystemInfoCommand());
		}
		if (shinobigami) {
			commandList.add(new ScenarioSetupCommand());
			commandList.add(new ScenarioSendSecretCommand());
			commandList.add(new ScenarioClearCommand());
		}
		return commandList;
	}

}
