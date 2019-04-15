package client;

import java.util.UUID;

import ray.rage.scene.Entity;
import ray.rage.scene.SceneNode;
import ray.rml.Vector3;
import ray.rml.Vector3f;

public class GhostAvatar {
	private UUID id;
	private SceneNode node;
	private Entity entity;
	
	public GhostAvatar(UUID id, Vector3 position) {
		this.id=id;
	}
	
	public UUID getID() {
		return id;
	}
	
	public void setNode(SceneNode node) {
		this.node = node;
	}
	
	public void setEntity(Entity entity) {
		this.entity = entity;
	}
	
	public void scale(float scale) {
		node.scale(Vector3f.createFrom(scale,scale,scale));
	}
	
	public void setPosition(float x, float y, float z) {
		node.setLocalPosition(x, y, z);
	}
	public void setOrientation(float x, float y, float z) {
		node.lookAt(Vector3f.createFrom(new float[] {x,y,z}));
	}
}
