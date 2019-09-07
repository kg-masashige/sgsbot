package csmp.bot.command.sgs;

import csmp.bot.command.IDiscordCommand;
import csmp.bot.model.CommandHelpData;
import csmp.bot.model.DiscordMessageData;

/**
 * シナリオ情報読み込みのみ実施コマンド.
 * 機能はセットアップで実装。
 * @author kgmas
 *
 */
public class ScenarioReadCommand implements IDiscordCommand {

	@Override
	public boolean judgeExecute(DiscordMessageData dmd) {
		return false;
	}

	@Override
	public boolean checkInput(DiscordMessageData dmd) {
		return true;
	}

	@Override
	public void execute(DiscordMessageData dmd) {
	}

	@Override
	public void warning(DiscordMessageData dmd) {
	}

	@Override
	public CommandHelpData getCommandHelpData() {
		return new CommandHelpData(
				"/sgsread <シナリオシートURL>",
				"シナリオシートの情報の読み込みだけ行う。");
	}
}
