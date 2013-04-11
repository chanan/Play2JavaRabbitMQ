
import infrastructure.Sender;

import java.io.IOException;

import play.*;

public class Global extends GlobalSettings {
	
	@Override
	public void onStart(Application app) {
		Logger.info("Application start");
		try {
			Sender.StartListeners();
		} catch (IOException e) {
			Logger.error(e.toString());
		}
	}

	@Override
	public void onStop(Application arg0) {
		Logger.info("Application stop");
		try {
			Sender.StopListeners();
		} catch (IOException e) {
			Logger.error(e.toString());
		}
	}
}
