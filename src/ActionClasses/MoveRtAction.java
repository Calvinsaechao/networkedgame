package ActionClasses;

import client.ProtocolClient;
import client.chainedGame;
import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;
import ray.rage.scene.SceneNode;

public class MoveRtAction extends AbstractInputAction{
	private SceneNode avN;
	private ProtocolClient protClient;
	private chainedGame game;
	public MoveRtAction(SceneNode n, ProtocolClient p, chainedGame g) {
		avN = n;
		protClient = p;
		game = g;
	}
	@Override
	public void performAction(float time, Event e) {
		System.out.println("Move Right Action");
		System.out.println("X pos: " + avN.getWorldPosition().x());
		System.out.println("Z pos: " + avN.getWorldPosition().z());
		avN.moveRight(-0.1f);
		game.updateVerticalPosition();
		protClient.sendMoveMessage(avN.getWorldPosition());
	}

}


