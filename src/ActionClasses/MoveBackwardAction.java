package ActionClasses;

import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;
import ray.rage.scene.SceneNode;

public class MoveBackwardAction extends AbstractInputAction {
	private SceneNode node;
	public MoveBackwardAction(SceneNode node) {
		this.node = node;
	}

	@Override
	public void performAction(float time, Event e) {
		System.out.println("Move Forward Action");
		node.moveBackward(0.01f);
		
	}

}
