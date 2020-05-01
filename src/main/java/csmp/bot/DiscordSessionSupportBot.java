package csmp.bot;

import java.util.ArrayList;
import java.util.List;

import csmp.bot.command.IDiscordCommand;
import csmp.bot.command.bcdice.BcDiceRollCommand;
import csmp.bot.command.bcdice.BcDiceSetSystemCommand;
import csmp.bot.command.bcdice.PlotOpenCommand;
import csmp.bot.command.character.LinkCharacterSheetCommand;
import csmp.bot.command.schedule.ScheduleAddCommand;
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
		sgsBot.execute();


	}

}
