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
		if(e.getComponent().getIdentifier().getName().equalsIgnoreCase("D")) {
			avN.yaw(Degreef.createFrom(-2));
		}
		else if (e.getComponent().getIdentifier().getName().equalsIgnoreCase("A")) {
			avN.yaw(Degreef.createFrom(2));
		}
		avN.moveBackward(-1);
		protClient.sendOrientationMessage(avN.getWorldPosition());
		avN.moveBackward(1);
	}
}
