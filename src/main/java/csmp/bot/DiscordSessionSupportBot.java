package csmp.bot;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import csmp.bot.command.IDiscordCommand;
import csmp.bot.command.bcdice.BcDiceGetSystemInfoCommand;
import csmp.bot.command.bcdice.BcDiceRollCommand;
import csmp.bot.command.bcdice.BcDiceSearchSystemNameCommand;
import csmp.bot.command.bcdice.BcDiceSetSystemCommand;
import csmp.bot.command.bcdice.PlotOpenCommand;
import csmp.bot.command.bcdice.PlotSetCommand;
import csmp.bot.command.bcdice.TableRollCommand;
import csmp.bot.command.bcdice.TableSetupCommand;
import csmp.bot.command.character.LinkCharacterSheetCommand;
import csmp.bot.command.schedule.ScheduleCreateCommand;
import csmp.bot.command.sgs.ScenarioClearCommand;
import csmp.bot.command.sgs.ScenarioSendSecretCommand;
import csmp.bot.command.sgs.ScenarioSetupCommand;
import csmp.bot.controller.DiscordBotController;
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

		List<Class<? extends IDiscordCommand>> botCommandClassList = new ArrayList<>();
		if ("true".equals(System.getenv("DAYCORD_SERVICE"))) {
			botCommandClassList.add(ScheduleCreateCommand.class);
		}
		if ("true".equals(System.getenv("DICEBOT_SERVICE"))) {
			botCommandClassList.add(TableSetupCommand.class);
			botCommandClassList.add(PlotOpenCommand.class);
			botCommandClassList.add(PlotSetCommand.class);
			botCommandClassList.add(LinkCharacterSheetCommand.class);
			botCommandClassList.add(BcDiceSearchSystemNameCommand.class);
			botCommandClassList.add(BcDiceSetSystemCommand.class);
			botCommandClassList.add(BcDiceGetSystemInfoCommand.class);
			botCommandClassList.add(TableRollCommand.class);
			botCommandClassList.add(BcDiceRollCommand.class);

			// ダイスボット初期化
			BcDiceApiService.getInstance();
		}
		if ("true".equals(System.getenv("SHINOBIGAMI_SERVICE"))) {
			botCommandClassList.add(ScenarioSetupCommand.class);
			botCommandClassList.add(ScenarioSendSecretCommand.class);
			botCommandClassList.add(ScenarioClearCommand.class);
			startMessage = "TRPGセッション（主にシノビガミ）を行うためのbotです。\r\n"
					+ "「/sgshelp」で実行可能なコマンドを確認できます。\r\n"
					+ "詳細は https://github.com/kg-masashige/sgsbot をご確認ください。\r\n"
					+ "「/」を入力した際にコマンドのサジェストが表示されない場合、上記のサイトからbotを導入し直してください。";
		}

		String token = System.getenv("DISCORD_BOT_TOKEN");
		boolean isMessageIntent = "true".equals(System.getenv("MESSAGE_INTENT"));
		List<Class<? extends IDiscordEvent>> daycordEventList = new ArrayList<>();
		daycordEventList.add(ScheduleMemberChangeEvent.class);

		DiscordBotController daycordBot = new DiscordBotController(botCommandClassList, daycordEventList, token);
		daycordBot.setCacheSize(0);
		daycordBot.setCacheStorageTimeInSeconds(0);
		// シャード分割用
		Optional.ofNullable(System.getenv("SHARD_TOTAL"))
		.ifPresent(value -> daycordBot.setTotalShards(Integer.parseInt(value)));
		Optional.ofNullable(System.getenv("SHARD_START_NO"))
				.ifPresent(value -> daycordBot.setStart(Integer.parseInt(value)));
		Optional.ofNullable(System.getenv("SHARD_END_NO"))
				.ifPresent(value -> daycordBot.setEnd(Integer.parseInt(value)));


		daycordBot.execute(startMessage, isMessageIntent);

	}

}
