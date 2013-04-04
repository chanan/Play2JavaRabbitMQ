package framework;

import akka.actor.UntypedActor;

public class CommandActor extends UntypedActor {
private ICommand command;
	
	public CommandActor(ICommand command) {
		this.command = command;
	}
	
	@Override
	public void onReceive(Object obj) throws Exception {
		command.Execute(obj.toString() + " " + getSelf().path().name());
	}
}
