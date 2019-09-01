package csmp.bot.model;

import java.util.Optional;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.TextChannel;

public class DiscordMessageData {

	public DiscordMessageData(Message message) {
		this.message = message;
	}

	/**
	 * メッセージ.
	 */
	private Message message = null;

	/**
	 * テキスト.
	 */
	private String text = null;

	/**
	 * ギルド.
	 */
	private Guild guild = null;

	/**
	 * チャンネル.
	 */
	private TextChannel channel = null;

	/**
	 * コマンド配列.
	 */
	private String[] commandArray = null;

	/**
	 * チャンネルの取得.
	 */
	public TextChannel getChannel() {
		if (channel == null) {
			channel = (TextChannel)message.getChannel().block();
		}
		return channel;
	}

	/**
	 * ギルドの取得.
	 */
	public Guild getGuild() {
		if (guild == null) {
			guild = message.getGuild().block();
		}
		return guild;
	}

	/**
	 * コマンド配列の取得.
	 * @return
	 */
	public String[] getCommandArray() {
		if (commandArray == null) {
			String text = getText();
			commandArray = text.split(" ");
		}
		return commandArray;
	}

	/**
	 * テキストの取得.
	 * @return
	 */
	public String getText() {
		if (text == null) {
			Optional<String> content = message.getContent();
			if (content.isPresent()) {
				text = content.get();
			}
			if (text == null) {
				text = "";
			}
		}

		return text;
	}

	/**
	 * メッセージの取得.
	 * @return
	 */
	public Message getMessage() {
		return message;
	}

}
