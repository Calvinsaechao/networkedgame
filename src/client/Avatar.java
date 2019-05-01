package client;

import java.util.UUID;

import InterfaceClasses.IPlayer;
import ray.rage.scene.Entity;
import ray.rage.scene.SceneNode;
import ray.rml.Vector3;

public class Avatar implements IPlayer{
	private UUID id;
	private SceneNode node;
	private Entity entity;
	
	public Avatar(UUID id, Vector3 position) {
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
	public void setPosition(float x, float y, float z) {
		node.setLocalPosition(x, y, z);
	}
	
	public Vector3 getPosition() {
		return node.getLocalPosition();
	}
}
