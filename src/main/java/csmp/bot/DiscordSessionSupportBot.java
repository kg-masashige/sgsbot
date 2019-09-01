package csmp.bot;

import java.util.ArrayList;
import java.util.List;

import csmp.bot.command.DiscordCommandBase;
import csmp.bot.command.help.HelpCommand;
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

		List<Class<? extends DiscordCommandBase>> sgsClassList = new ArrayList<>();
		sgsClassList.add(ScenarioSetupCommand.class);
		sgsClassList.add(ScenarioSendSecretCommand.class);
		sgsClassList.add(ScenarioClearCommand.class);

		List<Class<? extends DiscordCommandBase>> scheduleClassList = new ArrayList<>();
		scheduleClassList.add(ScheduleAddCommand.class);
		scheduleClassList.add(ScheduleDeleteCommand.class);
		scheduleClassList.add(ScheduleShowCommand.class);

		String sgsToken = System.getenv("DISCORD_BOT_TOKEN");
		List<Class<? extends DiscordCommandBase>> sgsExecList = new ArrayList<>();
		sgsExecList.addAll(sgsClassList);
		sgsExecList.addAll(scheduleClassList);
		sgsExecList.add(HelpCommand.class);

		DiscordBotController sgsBot = new DiscordBotController(sgsExecList, sgsToken);
		// TODO 複数スレッド化したかったけど、Discord4jが対応してない。
//		sgsBot.start();
		sgsBot.run();


	}

}
