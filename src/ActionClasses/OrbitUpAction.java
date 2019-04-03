package ActionClasses;

import ray.input.action.AbstractInputAction;                                                                                                        
import ray.rage.scene.*;                                                                                                                            
import ray.rage.game.*;                                                                                                                             
import ray.rml.*;
import client.chainedGame;
import net.java.games.input.Event; 

public class OrbitUpAction extends AbstractInputAction {
	
	// Need a constructor
	private Camera3PController controller;
	
	public OrbitUpAction(Camera3PController ctrl) {
		controller = ctrl;
		System.out.println("Elevation action constructor executed");
	}
	@Override
	public void performAction(float time, Event evt) {
		float rotAmount, cameraElevation;
		cameraElevation = controller.getElevation();
		Camera camera = controller.getCam();
		if (evt.getValue() < -0.2){
			rotAmount=-0.2f; 
		}else{
			if (evt.getValue() > 0.2) {
				rotAmount= 0.2f; 
			}else{
				rotAmount=0.0f; 
			}
		}
		cameraElevation += rotAmount;
		cameraElevation = cameraElevation % 360;
		controller.setElevation(cameraElevation);
		controller.updateCameraPosition();
		System.out.println("Elevation action executed");
		} 
		// similar for OrbitRadiasAction, OrbitElevationAction		
	}


