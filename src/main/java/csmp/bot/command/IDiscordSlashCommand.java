package csmp.bot.command;

import java.util.concurrent.ExecutionException;

import org.javacord.api.interaction.SlashCommandBuilder;

import csmp.bot.model.DiscordMessageData;

public interface IDiscordSlashCommand {

	/**
	 * コマンド登録.
	 * @return コマンド登録用Buidlder.
	 */
	public SlashCommandBuilder entryCommand();

	/**
	 * コマンド名取得.
	 * @return コマンド名.
	 */
	public String getCommandName();

	/**
	 * コマンド実行.
	 * @param dmd メッセージデータ.
	 */
	public void executeSlashCommand(DiscordMessageData dmd) throws InterruptedException, ExecutionException;

}
