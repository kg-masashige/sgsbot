package csmp.bot;

import java.util.ArrayList;
import java.util.List;

import csmp.bot.command.IDiscordCommand;
import csmp.bot.command.bcdice.BcDiceRollCommand;
import csmp.bot.command.bcdice.BcDiceSetSystemCommand;
import csmp.bot.command.bcdice.PlotOpenCommand;
import csmp.bot.command.character.LinkCharacterSheetCommand;
import csmp.bot.command.schedule.ScheduleAddCommand;
import csmp.bot.command.schedule.ScheduleCreateCommand;
import csmp.bot.command.schedule.ScheduleDeleteCommand;
import csmp.bot.command.schedule.ScheduleShowCommand;
import csmp.bot.command.sgs.ScenarioClearCommand;
import csmp.bot.command.sgs.ScenarioSendSecretCommand;
import csmp.bot.command.sgs.ScenarioSetupCommand;
import csmp.bot.controller.DiscordBotController;

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

		List<Class<? extends IDiscordCommand>> scheduleClassList = new ArrayList<>();
		scheduleClassList.add(ScheduleAddCommand.class);
		scheduleClassList.add(ScheduleDeleteCommand.class);
		scheduleClassList.add(ScheduleShowCommand.class);
		scheduleClassList.add(ScheduleCreateCommand.class);

		List<Class<? extends IDiscordCommand>> bcDiceBotClassList = new ArrayList<>();
		bcDiceBotClassList.add(PlotOpenCommand.class);
		bcDiceBotClassList.add(LinkCharacterSheetCommand.class);
		bcDiceBotClassList.add(BcDiceSetSystemCommand.class);
		bcDiceBotClassList.add(BcDiceRollCommand.class);


		String sgsToken = System.getenv("DISCORD_BOT_TOKEN");
		List<Class<? extends IDiscordCommand>> sgsExecList = new ArrayList<>();
		sgsExecList.addAll(sgsClassList);
		sgsExecList.addAll(scheduleClassList);
		sgsExecList.addAll(bcDiceBotClassList);

		DiscordBotController sgsBot = new DiscordBotController(sgsExecList, sgsToken);
		sgsBot.execute("TRPGセッション（主にシノビガミ）を行うためのbotです。\r\n"
				+ "「/sgshelp」で実行可能なコマンドを確認できます。\r\n"
				+ "詳細は https://github.com/kg-masashige/sgsbot をご確認ください。");

		String daycordToken = System.getenv("DAYCORD_BOT_TOKEN");

		List<Class<? extends IDiscordCommand>> daycordList = new ArrayList<>();
		daycordList.add(ScheduleCreateCommand.class);
		DiscordBotController daycordBot = new DiscordBotController(daycordList, daycordToken);
		daycordBot.execute("デイコードはDiscordと連携して日程調整ができるサービスです。\n"
				+ "「/スケジュール」と入力するだけで、簡単にスケジュール調整ページを作成することができます。");

	}

}
