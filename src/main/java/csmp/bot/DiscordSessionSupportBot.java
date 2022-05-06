package csmp.bot;

import java.util.ArrayList;
import java.util.List;

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

		List<Class<? extends IDiscordCommand>> sgsClassList = new ArrayList<>();
		sgsClassList.add(ScenarioSetupCommand.class);
		sgsClassList.add(ScenarioSendSecretCommand.class);
		sgsClassList.add(ScenarioClearCommand.class);

		List<Class<? extends IDiscordCommand>> bcDiceBotClassList = new ArrayList<>();
		bcDiceBotClassList.add(TableSetupCommand.class);
		bcDiceBotClassList.add(PlotOpenCommand.class);
		bcDiceBotClassList.add(PlotSetCommand.class);
		bcDiceBotClassList.add(LinkCharacterSheetCommand.class);
		bcDiceBotClassList.add(BcDiceSearchSystemNameCommand.class);
		bcDiceBotClassList.add(BcDiceSetSystemCommand.class);
		bcDiceBotClassList.add(BcDiceGetSystemInfoCommand.class);
		bcDiceBotClassList.add(TableRollCommand.class);
		bcDiceBotClassList.add(BcDiceRollCommand.class);

		List<Class<? extends IDiscordCommand>> daycordList = new ArrayList<>();
		daycordList.add(ScheduleCreateCommand.class);

		List<Class<? extends IDiscordEvent>> daycordEventList = new ArrayList<>();
		daycordEventList.add(ScheduleMemberChangeEvent.class);

		String sgsToken = System.getenv("DISCORD_BOT_TOKEN");
		List<Class<? extends IDiscordCommand>> sgsExecList = new ArrayList<>();
		sgsExecList.addAll(sgsClassList);
		sgsExecList.addAll(daycordList);
		sgsExecList.addAll(bcDiceBotClassList);

		String daycordToken = System.getenv("DAYCORD_BOT_TOKEN");
		DiscordBotController daycordBot = new DiscordBotController(daycordList, daycordEventList, daycordToken);
		daycordBot.setCacheSize(0);
		daycordBot.setCacheStorageTimeInSeconds(0);

		daycordBot.execute("デイコードはDiscordと連携して日程調整ができるサービスです。\n"
				+ "「/schedule」と入力するだけで、簡単にスケジュール調整ページを作成することができます。\n"
				+ "詳細は https://character-sheets.appspot.com/schedule/ をご確認ください。\r\n"
				+ "「/」を入力した際にコマンドのサジェストが表示されない場合、上記のサイトからbotを導入し直してください。");

		// ダイスボット初期化
		BcDiceApiService.getInstance();

		DiscordBotController sgsBot = new DiscordBotController(sgsExecList, daycordEventList, sgsToken);
		sgsBot.execute("TRPGセッション（主にシノビガミ）を行うためのbotです。\r\n"
				+ "「/sgshelp」で実行可能なコマンドを確認できます。\r\n"
				+ "詳細は https://github.com/kg-masashige/sgsbot をご確認ください。\r\n"
				+ "「/」を入力した際にコマンドのサジェストが表示されない場合、上記のサイトからbotを導入し直してください。");


	}

}
