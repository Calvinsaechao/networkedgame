package ActionClasses;

import client.ProtocolClient;
import client.chainedGame;
import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;
import ray.rage.scene.SceneNode;

public class MoveFwdAction extends AbstractInputAction {

	private SceneNode avN;
	private ProtocolClient protClient;
	private chainedGame game;
	public MoveFwdAction(SceneNode n, ProtocolClient p, chainedGame g) {
		avN = n;
		protClient = p;
		game = g;
	}
	@Override
	public void performAction(float time, Event e) {
		System.out.println("Move Forward Action");
		System.out.println("X pos: " + avN.getWorldPosition().x());
		System.out.println("Z pos: " + avN.getWorldPosition().z());
		avN.moveForward(0.1f);
		game.updateVerticalPosition();
		protClient.sendMoveMessage(avN.getWorldPosition());
	}
}
