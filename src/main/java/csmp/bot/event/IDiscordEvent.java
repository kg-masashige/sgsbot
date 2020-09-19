package csmp.bot.event;

import java.util.concurrent.ExecutionException;

import csmp.bot.model.DiscordEventData;

public interface IDiscordEvent {

	public abstract void execute(DiscordEventData ded) throws InterruptedException, ExecutionException;

}
