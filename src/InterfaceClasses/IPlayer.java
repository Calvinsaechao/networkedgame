package InterfaceClasses;

import java.util.UUID;

import ray.rml.Vector3;

public interface IPlayer{
	public Vector3 getPosition();
	public UUID getID();
	public void collided();
	public boolean isCollided();
}
