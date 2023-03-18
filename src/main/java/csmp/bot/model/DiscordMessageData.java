package csmp.bot.model;

import java.util.Optional;

import org.javacord.api.entity.channel.ChannelCategory;
import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.interaction.SlashCommandInteraction;

public class DiscordMessageData {

	public DiscordMessageData(MessageCreateEvent event) {
		this.message = event;
	}

	public DiscordMessageData(SlashCommandCreateEvent event) {
		this.slashCommandEvent = event;
		this.interaction = event.getSlashCommandInteraction();
	}

	/**
	 * スラッシュコマンドイベント.
	 */
	private SlashCommandCreateEvent slashCommandEvent = null;

	/**
	 * インタラクション.
	 */
	private SlashCommandInteraction interaction = null;

	/**
	 * メッセージ.
	 */
	private MessageCreateEvent message = null;

	/**
	 * ユーザー
	 */
	private User user = null;

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
			if (message != null) {
				channel = message.getChannel();
			} else {
				channel = getInteraction().getChannel().orElse(null);
			}
		}

		return channel;
	}

	/**
	 * ギルドの取得.
	 */
	public Server getGuild() {
		if (guild == null) {
			if (message != null) {
				guild = message.getServer().orElse(null);
			} else {
				guild = getInteraction().getServer().orElse(null);
			}
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
			if (this.interaction == null) {
				String content = message.getMessageContent();
				if (content == null) {
					content = "";
				}
				if (content.startsWith("／")) {
					content = content.replace("／", "/");
				}

				text = content;
			} else {
				text = interaction.getFullCommandName();
			}
		}

		return text;
	}

	/**
	 * テキストの設定.
	 * スラッシュコマンド時に設定する.
	 * @param text
	 */
	public void setText(String text) {
		this.text = text;
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

	/**
	 * @return interaction
	 */
	public SlashCommandInteraction getInteraction() {
		return interaction;
	}

	/**
	 * @return user
	 */
	public User getUser() {
		if (user == null) {
			if (message != null) {
				user = message.getMessageAuthor().asUser().orElse(null);
			} else {
				user = getInteraction().getUser();
			}
		}

		return user;
	}

}
