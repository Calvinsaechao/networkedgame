package Controllers;

import java.util.Iterator;

import ray.rage.scene.Node;
import ray.rage.scene.SceneNode;
import ray.rage.scene.controllers.AbstractController;
import ray.rml.Vector3;
import ray.rml.Vector3f;

public class ElevationController extends AbstractController{
	private float original;
	private SceneNode target;
	private float speed;
	private float distance;
	public ElevationController(SceneNode target, float speed, float distance) {
		this.original = target.getLocalPosition().y();
		this.target = target;
		this.speed = speed;
		this.distance = distance;
	}
	@Override
	protected void updateImpl(float elapsedTimeMillis) {
		// TODO Auto-generated method stub
		Iterator<Node> i = super.controlledNodesList.iterator();
		while(i.hasNext()) {
			SceneNode node = (SceneNode) i.next();
			if (node == target) {
				Vector3 pos = node.getLocalPosition();
				float top = original + distance;
				float bottom = original - distance;
				float current = pos.y();
				float change = 0f;
				if(current > top || current < bottom) speed = -speed;
				change = current + speed;
				Vector3 newPos = (Vector3)Vector3f.createFrom(pos.x(), change, pos.z());
				target.setLocalPosition(newPos);
			}
		}
	}
}