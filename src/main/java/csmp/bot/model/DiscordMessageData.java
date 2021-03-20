package csmp.bot.model;

import java.util.Optional;

import org.javacord.api.entity.channel.ChannelCategory;
import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;

public class DiscordMessageData {

	public DiscordMessageData(MessageCreateEvent event) {
		this.message = event;
	}

	/**
	 * メッセージ.
	 */
	private MessageCreateEvent message = null;

	/**
	 * テキスト.
	 */
	private String text = null;

	/**
	 * ギルド.
	 */
	private Server guild = null;

	/**
	 * チャンネル.
	 */
	private TextChannel channel = null;

	/**
	 * コマンド配列.
	 */
	private String[] commandArray = null;

	/**
	 * カテゴリ
	 */
	private ChannelCategory category = null;

	/**
	 * チャンネルの取得.
	 */
	public TextChannel getChannel() {
		if (channel == null) {
			channel = message.getChannel();
		}
		return channel;
	}

	/**
	 * ギルドの取得.
	 */
	public Server getGuild() {
		if (guild == null) {
			guild = message.getServer().orElse(null);
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
			String content = message.getMessageContent();
			if (content == null) {
				content = "";
			}
			if (content.startsWith("／")) {
				content = content.replace("／", "/");
			}

			text = content;
		}

		return text;
	}

	/**
	 * メッセージの取得.
	 * @return
	 */
	public MessageCreateEvent getMessage() {
		return message;
	}

	/**
	 * カテゴリーの取得
	 * @return category
	 */
	public Optional<ChannelCategory> getCategory() {
    	for (ChannelCategory channelCategory : getGuild().getChannelCategories()) {
			for (ServerChannel serverChannel : channelCategory.getChannels()) {
				if (serverChannel.getId() == getChannel().getId()) {
					category = channelCategory;
					break;
				}
			}
		}

    	return Optional.ofNullable(category);
	}

}
