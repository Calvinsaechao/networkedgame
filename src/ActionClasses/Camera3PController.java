package ActionClasses;

import ray.input.InputManager;
import ray.input.action.AbstractInputAction;
import ray.input.action.Action;
import ray.rage.scene.*;                                                                                                                            
import ray.rage.game.*;                                                                                                                             
import ray.rml.*;

import java.util.ArrayList;

import client.chainedGame;
import net.java.games.input.Controller;
import net.java.games.input.Event; 
// write setters/getters for variables DONE
public class Camera3PController {
	private Camera camera; //the camera being controlled
	private SceneNode cameraN; //the node the camera is attached to
	private SceneNode target; //the target the camera looks at
	private float cameraAzimuth; //rotation of camera around Y axis
	private float cameraElevation; //elevation of camera above target
	private float radias; //distance between camera and target
	private Vector3 targetPos; //target’s position in the world
	private Vector3 worldUpVec;
	public Camera3PController(Camera cam, SceneNode camN,SceneNode targ, InputManager im) {
		camera = cam;
		cameraN = camN;
		target = targ;
		cameraAzimuth = 0f; // start from BEHIND and ABOVE the target
		cameraElevation = 20.0f; // elevation is in degrees
		radias = 2.0f;
		worldUpVec = Vector3f.createFrom(0.0f, 1.0f, 0.0f);
		setupInput(im);
	}
	
	 // Updates camera position: computes azimuth, elevation, and distance
	 // relative to the target in spherical coordinates, then converts those
	 // to world Cartesian coordinates and setting the camera position
	 public void updateCameraPosition() {
	   	double theta = Math.toRadians(cameraAzimuth); // rot around target
    	double phi = Math.toRadians(cameraElevation); // altitude angle
	   	double x = radias * Math.cos(phi) * Math.sin(theta);
    	double y = radias * Math.sin(phi);
    	double z = radias * Math.cos(phi) * Math.cos(theta);
	   	cameraN.setLocalPosition(Vector3f.createFrom((float)x, (float)y, (float)z).add(target.getWorldPosition()));
	   	cameraN.lookAt(target, worldUpVec);
	 }
	    
	 private void setupInput(InputManager im){
    	Action orbitAAction = new OrbitRightAction(this);// pass controller
    	Action orbitALction = new OrbitLeftAction(this);
    	Action orbitUpAction = new OrbitUpAction(this);
    	Action orbitDownAction = new OrbitDownAction(this);
    	ArrayList<Controller> controllers = im.getControllers();
    	for (Controller c : controllers) {
    		if (c.getType() == Controller.Type.KEYBOARD) {
    			System.out.println ("keyboard found");
     			 im.associateAction(c, 
     					 net.java.games.input.Component.Identifier.Key.RIGHT, 
     					 orbitAAction,InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
     			 im.associateAction(c,
     					 net.java.games.input.Component.Identifier.Key.LEFT, 
     					 orbitALction,InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
     			im.associateAction(c,
    					 net.java.games.input.Component.Identifier.Key.UP, 
    					 orbitUpAction,InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
     			im.associateAction(c,
    					 net.java.games.input.Component.Identifier.Key.DOWN, 
    					 orbitDownAction,InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
    			 
    			 
    		}
    	}
	  }
	 
	 public float getAzimuth() {
		 return cameraAzimuth;
	 }
	 
	 public void setAzimuth(float azim) {
		 cameraAzimuth = azim;
	 }
	 
	 
	 public float getElevation() {
		 return cameraElevation;
	 }
	 
	 public void setElevation(float elev) {
		 cameraElevation = elev;
	 }
	 
	 public Camera getCam() {
		 return camera;
	 }
	 
	 public SceneNode getSceneNode() {
		 return cameraN;
	 }
	 
	 public SceneNode getTarget() {
		 return target;
	 }
}