package Controllers;

import java.util.Iterator;

import ray.rage.scene.Node;
import ray.rage.scene.SceneNode;
import ray.rage.scene.controllers.AbstractController;
import ray.rml.Degreef;

public class RotationController extends AbstractController{
	private SceneNode target;
	private float speed;
	public RotationController(SceneNode target, float speed) {
		this.target = target;
		this.speed = speed;
	}
	@Override
	protected void updateImpl(float elapsedTimeMillis) {
		// TODO Auto-generated method stub
		Iterator<Node> i = super.controlledNodesList.iterator();
		while(i.hasNext()) {
			SceneNode node = (SceneNode) i.next();
			if (node == target) {
				target.yaw(Degreef.createFrom(speed));
			}
		}
	}
}