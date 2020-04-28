package csmp.bot.command;

import java.util.concurrent.ExecutionException;

import csmp.bot.model.CommandHelpData;
import csmp.bot.model.DiscordMessageData;

public interface IDiscordCommand {

	public abstract boolean judgeExecute(DiscordMessageData dmd);

	public abstract boolean checkInput(DiscordMessageData dmd);

	public abstract void execute(DiscordMessageData dmd) throws InterruptedException, ExecutionException;

	public abstract void warning(DiscordMessageData dmd);

	public abstract CommandHelpData getCommandHelpData();

}
