package framework;

import play.Logger;

public class LoggingCommand implements ICommand {
	@Override
	public void Execute(String message) {
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Logger.info(String.format("Got message on %s : %s", Thread.currentThread().getName(), message));
	}
}
