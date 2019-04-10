package ActionClasses;

import client.ProtocolClient;
import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;
import ray.rage.scene.SceneNode;

public class MoveRightAction extends AbstractInputAction {

	private SceneNode avN;
	private ProtocolClient protClient;
	
	public MoveRightAction(SceneNode n, ProtocolClient p) {
		avN = n;
		protClient = p;
	}
	@Override
	public void performAction(float time, Event e) {
		System.out.println("Move Right Action");
		avN.moveRight(0.01f);
		protClient.sendMoveMessage(avN.getWorldPosition());
	}
}
