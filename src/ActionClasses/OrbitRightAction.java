package ActionClasses;

import ActionClasses.Camera3PController;
import client.ProtocolClient;
import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;
import ray.rage.scene.Camera;
import ray.rage.scene.SceneNode;

public class OrbitRightAction extends AbstractInputAction {

	private Camera3PController controller;
	
	public OrbitRightAction(Camera3PController ctrl) {
		controller = ctrl;

	}
	@Override
	public void performAction(float time, Event evt) {

		float rotAmount, cameraAzimuth;
		cameraAzimuth = controller.getAzimuth();
		//Camera camera = controller.getCam();
		SceneNode cameraN = controller.getSceneNode();
		// in game: create getters for azimuth
		rotAmount= 0.5f; 
		cameraAzimuth += rotAmount;
		cameraAzimuth = cameraAzimuth % 360;
		controller.setAzimuth(cameraAzimuth);
		controller.updateCameraPosition();

		} 
		// similar for OrbitRadiasAction, OrbitElevationAction		
	}

