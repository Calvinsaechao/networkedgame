package ActionClasses;

import client.ProtocolClient;
import client.chainedGame;
import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;
import ray.rage.scene.SceneNode;
import ray.rml.Degreef;

public class TurnAction extends AbstractInputAction {

	private SceneNode avN;
	private ProtocolClient protClient;
	private chainedGame game;
	public TurnAction(SceneNode n, ProtocolClient p, chainedGame g) {
		avN = n;
		protClient = p;
		game = g;
	}
	@Override
	public void performAction(float time, Event e) {
		if(e.getComponent().getIdentifier().getName().equalsIgnoreCase("E")) {
			System.out.println("Turning right...");
			avN.yaw(Degreef.createFrom(-2));
		}
		else if (e.getComponent().getIdentifier().getName().equalsIgnoreCase("Q")) {
			System.out.println("Turning left...");
			avN.yaw(Degreef.createFrom(2));
		}
		//TODO: turn message
		protClient.sendOrientationMessage(avN.getWorldPosition());
	}
}
