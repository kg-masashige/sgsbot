package csmp.bot.command;

import csmp.bot.model.CommandHelpData;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

/**
 * スラッシュコマンド.
 * @author kgmas
 *
 */
public interface ISlashCommand {

	/**
	 * コマンド名取得.
	 * @return コマンド名.
	 */
	public String getCommandName();

	/**
	 * コマンド登録用の定義を作成する.
	 * @return コマンド定義.
	 */
	public SlashCommandData createCommandData();

	/**
	 * コマンド実行.
	 * サーバー内で実行された場合のみ呼ばれる。
	 * @param event スラッシュコマンドイベント.
	 */
	public void execute(SlashCommandInteractionEvent event) throws Exception;

	/**
	 * ヘルプ情報の取得.
	 * @return ヘルプ情報. ヘルプに表示しない場合はnull.
	 */
	public CommandHelpData getCommandHelpData();

	/**
	 * モーダル送信時の処理.
	 * モーダルのIDが「コマンド名:」で始まる場合に呼ばれる。
	 * @param event モーダル送信イベント.
	 */
	public default void executeModal(ModalInteractionEvent event) throws Exception {
	}

}
