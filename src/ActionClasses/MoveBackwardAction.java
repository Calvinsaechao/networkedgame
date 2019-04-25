package ActionClasses;

import client.ProtocolClient;
import client.chainedGame;
import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;
import ray.rage.scene.SceneNode;

public class MoveBackwardAction extends AbstractInputAction {
	private SceneNode avN;
	private ProtocolClient protClient;
	private chainedGame game;
	public MoveBackwardAction(SceneNode n, ProtocolClient p, chainedGame g) {
		avN = n;
		protClient = p;
		game = g;
	}

	@Override
	public void performAction(float time, Event e) {
		avN.moveBackward(1.0f);
		game.updateVerticalPosition();
		protClient.sendMoveMessage(avN.getWorldPosition());
		
		System.out.println("X" + avN.getWorldPosition().x());
		System.out.println("Y" + avN.getWorldPosition().y());
		System.out.println("Z" + avN.getWorldPosition().z());
	}

}
