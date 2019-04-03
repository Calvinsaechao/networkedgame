package ActionClasses;

import ray.input.action.AbstractInputAction;                                                                                                        
import ray.rage.scene.*;                                                                                                                            
import ray.rage.game.*;                                                                                                                             
import ray.rml.*;
import client.chainedGame;
import net.java.games.input.Event; 

public class OrbitLeftAction extends AbstractInputAction {
	
	// Need a constructor
	private Camera3PController controller;
	
	public OrbitLeftAction(Camera3PController ctrl) {
		controller = ctrl;
		System.out.println("Orbit action constructor executed");

	}
	@Override
	public void performAction(float time, Event evt) {
		System.out.println("Orbit action executed begins");

		float rotAmount, cameraAzimuth;
		cameraAzimuth = controller.getAzimuth();
		Camera camera = controller.getCam();
		rotAmount= -0.5f; 
	
		cameraAzimuth += rotAmount;
		cameraAzimuth = cameraAzimuth % 360;
		controller.setAzimuth(cameraAzimuth);
		controller.updateCameraPosition();
		System.out.println("Orbit action executed ends");

		} 
		// similar for OrbitRadiasAction, OrbitElevationAction		
	}
