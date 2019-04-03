package ActionClasses;

import ActionClasses.Camera3PController;
import client.ProtocolClient;
import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;
import ray.rage.scene.Camera;
import ray.rage.scene.SceneNode;

public class OrbitAroundAction extends AbstractInputAction {

	private Camera3PController controller;
	
	public OrbitAroundAction(Camera3PController ctrl) {
		controller = ctrl;
		System.out.println("Orbit action constructor executed");

	}
	@Override
	public void performAction(float time, Event evt) {
		System.out.println("Orbit action executed begins");

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
		System.out.println("Orbit action executed ends");

		} 
		// similar for OrbitRadiasAction, OrbitElevationAction		
	}

