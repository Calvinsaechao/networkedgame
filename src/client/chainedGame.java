package client;

import java.awt.Color;
import java.awt.DisplayMode;
import java.awt.GraphicsEnvironment;
import java.awt.geom.AffineTransform;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

import ActionClasses.*;
import net.java.games.input.Controller;
import net.java.games.input.Event;
import ray.input.GenericInputManager;
import ray.input.InputManager;
import ray.input.action.AbstractInputAction;
import ray.input.action.Action;
import ray.networking.IGameConnection.ProtocolType;
import ray.rage.Engine;
import ray.rage.asset.texture.Texture;
import ray.rage.game.Game;
import ray.rage.game.VariableFrameRateGame;
import ray.rage.rendersystem.RenderSystem;
import ray.rage.rendersystem.RenderWindow;
import ray.rage.rendersystem.Renderable.Primitive;
import ray.rage.rendersystem.Viewport;
import ray.rage.rendersystem.gl4.GL4RenderSystem;
import ray.rage.rendersystem.states.RenderState;
import ray.rage.rendersystem.states.TextureState;
import ray.rage.scene.Camera;
import ray.rage.scene.Camera.Frustum.Projection;
import ray.rage.scene.Entity;
import ray.rage.scene.Light;
import ray.rml.Degreef;
import ray.rml.Vector3;
import ray.rml.Vector3f;
import ray.rage.scene.SceneManager;
import ray.rage.scene.SceneNode;
import ray.rage.scene.SkyBox;
import ray.rage.scene.Tessellation;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.*;

public class chainedGame extends VariableFrameRateGame{
	GL4RenderSystem rs;
	float elapsTime = 0.0f;
	private GenericInputManager im;
	private SceneManager sm;
	private static final String SKYBOX_NAME = "SkyBox";
	private ScriptEngine jsEngine;
	private File scriptFile1;
	private long fileLastModifiedTime;
	private Camera3PController orbitController;
	
	//client/server
	private String serverAddress;
	private int serverPort;
	private ProtocolType serverProtocol;
	private ProtocolClient protClient;
	private boolean isClientConnected;
	private Vector<UUID> gameObjectsToRemove;
	
	public chainedGame(String serverAddr, int sPort) {
		super();
		this.serverAddress = serverAddr;
		this.serverPort = sPort;
		this.serverProtocol = ProtocolType.UDP;
		scriptFile1 = new File("client\\setGhostParams.js"); //Read up on File Separator
	}
	
	public static void main(String[] args) {
		Game game = new chainedGame(args[0], Integer.parseInt(args[1]));
	
		try {
			game.startup();
			game.run();
		} catch (Exception e) {
			e.printStackTrace(System.err);
		} finally {
			game.shutdown();
			game.exit();
		}
	}
	
	private void setupNetworking() {
		gameObjectsToRemove = new Vector<UUID>();
		isClientConnected = false;
		try {
			protClient = new ProtocolClient(InetAddress.
					getByName(serverAddress), serverPort, serverProtocol, this);
		} catch(UnknownHostException e) {e.printStackTrace();
		} catch(IOException e) {e.printStackTrace();
		}
		if (protClient == null) {
			System.out.println("missing protocol host");
		}
		else {
			// ask client protocol to send initial join message
			// to server, with a unique identifier for this client
			protClient.sendJoinMessage();
		}
	}
	
	private void executeScript(ScriptEngine engine, File scriptFileName) {
		try
		{ FileReader fileReader = new FileReader(scriptFileName);
		  System.out.println("FileReader directory: " + scriptFileName.getAbsoluteFile());
		  engine.eval(fileReader);
		  fileLastModifiedTime = scriptFile1.lastModified();
		  fileReader.close();}
		catch(FileNotFoundException e1)
		{System.out.println(scriptFileName + " not found " + e1);}
		catch(IOException e2)
		{System.out.println("IO problem with " + scriptFileName + e2);}
		catch(ScriptException e3)
		{System.out.println("Script Exception in " + scriptFileName + e3);}
		catch(NullPointerException e4)
		{System.out.println("Null ptr exception in " + scriptFileName +" " + e4);
		e4.printStackTrace();}
		
	}
	@Override
	protected void setupWindow(RenderSystem rs, GraphicsEnvironment ge) {
		rs.createRenderWindow(new DisplayMode(1000,700,24,60), false);
	}
	
	protected void setupWindowViewports(RenderWindow rw) {
		Viewport viewport = rw.getViewport(0);
		viewport.setDimensions(0f, 0f, 1f, 1f);
	}
	
	@Override
	protected void setupCameras(SceneManager sm, RenderWindow rw) {
	
		SceneNode rootNode = sm.getRootSceneNode();
		Camera camera = sm.createCamera("MainCamera", Projection.PERSPECTIVE);
		rw.getViewport(0).setCamera(camera);
		SceneNode cameraNode = rootNode.createChildSceneNode(camera.getName() + "Node");
		cameraNode.attachObject(camera);
		camera.setMode('n');
		cameraNode.moveBackward(3.0f);
		camera.getFrustum().setFarClipDistance(1000.0f);
	}

	@Override
	protected void setupScene(Engine eng, SceneManager sm) throws IOException {
		this.sm =sm;
		im = new GenericInputManager();
		setupNetworking();
		//-------------Skybox-------------//
		makeSkybox(eng,sm);
		
		//-------------Lights-------------//
		sm.getAmbientLight().setIntensity(new Color(.3f, .3f, .3f));
		Light plight=sm.createLight("testLamp1", Light.Type.POINT);
		plight.setAmbient(new Color(.1f,.1f,.1f));
		plight.setDiffuse(new Color(.8f,.8f,.8f));
		plight.setSpecular(new Color(1.0f,1.0f,1.0f));
		plight.setRange(60f);
		SceneNode plightNode = sm.getRootSceneNode().createChildSceneNode("plightNode");
		plightNode.attachObject(plight);
		plightNode.setLocalPosition(1f,1f, 1f);
		
		//-------------Avatars-------------//
		Vector3f playerApos = (Vector3f)Vector3f.createFrom(0f,0f, 6.0f);
		Avatar playerA = new Avatar(protClient.getID(), playerApos);
		addAvatarToGameWorld(playerA, sm);
		
		//-------------Terrain-------------//
		Tessellation tessE = sm.createTessellation("tessE", 7);
		tessE.setSubdivisions(8f);
		SceneNode tessN = sm.getRootSceneNode().createChildSceneNode("TessN");
		tessN.attachObject(tessE);
		tessN.translate(Vector3f.createFrom(-6.2f,-2.2f,2.7f));
		//tessN.yaw(Degreef.createFrom(37.2f));
		tessN.scale(400, 800, 400);
		tessE.setHeightMap(this.getEngine(), "heightmap.png");
		tessE.setTexture(this.getEngine(), "grassy.jpg");
		tessE.setNormalMap(this.getEngine(), "normal_map.png");
		
		//--------Relative Objects--------//
		makePlanet(sm, (Vector3)Vector3f.createFrom(-3f, 2.0f, -3f));
		//makeTree (sm, (Vector3)Vector3f.createFrom(3f, 1f, -3f));
		//makeRock (sm, (Vector3)Vector3f.createFrom(-3f, 1f, -2f));
		
		
		//Script Engine
		ScriptEngineManager factory = new ScriptEngineManager();	
		List <ScriptEngineFactory> list = factory.getEngineFactories();
		jsEngine = factory.getEngineByName("js");
		this.executeScript(jsEngine, scriptFile1);
		
		setupInputs();
		setupOrbitCameras(eng, sm);
	}
	
	protected void makePlanet(SceneManager sm, Vector3 pos) throws IOException {
		Entity planetE = sm.createEntity("planet", "earth.obj");
		planetE.setPrimitive(Primitive.TRIANGLES);
		SceneNode planetN = sm.getRootSceneNode().createChildSceneNode(planetE.getName() +"Node");
		planetN.attachObject(planetE);
		planetN.setLocalPosition(pos);
	}
	
	protected void makeTree (SceneManager sm, Vector3 pos) throws IOException{
		Entity treeE = sm.createEntity("tree", "tree.obj");
		treeE.setPrimitive(Primitive.TRIANGLES);
		SceneNode treeN = sm.getRootSceneNode().createChildSceneNode(treeE.getName() + "Node");
		Texture texTree = this.getEngine().getTextureManager().getAssetByPath("tree.png");
        TextureState texTreeState = (TextureState)sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
        texTreeState.setTexture(texTree);
        treeE.setRenderState(texTreeState);
		treeN.attachObject(treeE);
		treeN.setLocalPosition(pos);
	}
	
	protected void makeRock (SceneManager sm, Vector3 pos) throws IOException{
		Entity rockE = sm.createEntity("rock", "rock.obj");
		rockE.setPrimitive(Primitive.TRIANGLES);
		SceneNode rockN = sm.getRootSceneNode().createChildSceneNode(rockE.getName() + "Node");
		Texture texRock = this.getEngine().getTextureManager().getAssetByPath("rock.png");
        TextureState texRockState = (TextureState)sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
        texRockState.setTexture(texRock);
        rockE.setRenderState(texRockState);
		rockN.attachObject(rockE);
		rockN.setLocalPosition(pos);
	}
	protected void setupInputs() {
		im = new GenericInputManager();
		ArrayList<Controller> controllers = im.getControllers();
		//String kbName = im.getKeyboardName();
		
		SceneNode AvatarN = sm.getSceneNode("playerNode");
		Action moveForwardAction = new MoveForwardAction(AvatarN, protClient);
		Action moveBackwardAction = new MoveBackwardAction(AvatarN, protClient);
		Action moveRightAction = new MoveRightAction(AvatarN, protClient);
		Action moveLeftAction = new MoveLeftAction(AvatarN, protClient);
		Action orbitAroundAction = new OrbitRightAction(orbitController);
		Action orbitLeftAction = new OrbitLeftAction(orbitController);
		Action orbitUpAction = new OrbitUpAction(orbitController);
    	Action orbitDownAction = new OrbitDownAction(orbitController);
		Action sendCloseConPckAction = new SendCloseConnectionPacketAction();
		for (Controller c : controllers) {
   		 if (c.getType() == Controller.Type.KEYBOARD) {
   			 im.associateAction(c,
				net.java.games.input.Component.Identifier.Key.W,
				moveForwardAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
   			 im.associateAction(c,
				net.java.games.input.Component.Identifier.Key.S,
				moveBackwardAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
   			 im.associateAction(c,
   					net.java.games.input.Component.Identifier.Key.A,
   					moveLeftAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
   			 im.associateAction(c,
   					net.java.games.input.Component.Identifier.Key.D,
   					moveRightAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
   			 im.associateAction(c,
				net.java.games.input.Component.Identifier.Key.ESCAPE,
				sendCloseConPckAction, InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
   			im.associateAction(c, 
   					net.java.games.input.Component.Identifier.Key.LEFT,
   					orbitAroundAction,InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
			 im.associateAction(c, 
					 net.java.games.input.Component.Identifier.Key.RIGHT,
					 orbitLeftAction,InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
			 im.associateAction(c,
					 net.java.games.input.Component.Identifier.Key.UP, 
					 orbitUpAction,InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
 			im.associateAction(c,
					 net.java.games.input.Component.Identifier.Key.DOWN, 
					 orbitDownAction,InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
   		 }
		}
	}
	
	//-------------Orbit Camera-------------//
	
	protected void setupOrbitCameras(Engine eng, SceneManager sm) {
		SceneNode avatarN = sm.getSceneNode("playerNode");
    	SceneNode cameraN = sm.getSceneNode("MainCameraNode");
    	Camera camera = sm.getCamera("MainCamera");
    	camera.setMode('n');
    	orbitController = new Camera3PController(camera, cameraN, avatarN, im);
	}

	@Override
	protected void update(Engine engine) {
		rs = (GL4RenderSystem) engine.getRenderSystem();
		SceneManager sm = engine.getSceneManager();
		elapsTime += engine.getElapsedTimeMillis();
		processNetworking(elapsTime, sm);
		long modTime = scriptFile1.lastModified();
		if(modTime > fileLastModifiedTime) {
			System.out.println("Updating file: " + scriptFile1.toString());
			fileLastModifiedTime = modTime;
			this.executeScript(jsEngine, scriptFile1);
			float scale = Double.valueOf((Double)jsEngine.get("scale")).floatValue();
			protClient.scaleGhostAvatars(scale);
		}
		im.update(elapsTime);
		orbitController.updateCameraPosition();
 	}
	
	protected void processNetworking(float elapsTime, SceneManager sm) {
		//Process packets received by the client from the server
		if (protClient != null)
			protClient.processPackets();
		// remove ghost avatars for players who have left the game
		Iterator<UUID> it = gameObjectsToRemove.iterator();
		while(it.hasNext()) {   
			sm.destroySceneNode(it.next().toString());
			} 
		gameObjectsToRemove.clear();
	}
	
	public void makeSkybox(Engine eng, SceneManager sm) throws IOException{
		Texture bottom = eng.getTextureManager().getAssetByPath("down.png");
		Texture front = eng.getTextureManager().getAssetByPath("front.png");
		Texture back = eng.getTextureManager().getAssetByPath("back.png");
		Texture top = eng.getTextureManager().getAssetByPath("up.png");
		Texture left = eng.getTextureManager().getAssetByPath("left.png");
		Texture right = eng.getTextureManager().getAssetByPath("right.png");
		AffineTransform xform = new AffineTransform();
		xform.translate(0, front.getImage().getHeight());
		xform.scale(1d, -1d);
		front.transform(xform);
		top.transform(xform);
		bottom.transform(xform);
		left.transform(xform);
		right.transform(xform);
		back.transform(xform);
		
		SkyBox sb = sm.createSkyBox(SKYBOX_NAME);
		sb.setTexture(front, SkyBox.Face.FRONT);
		sb.setTexture(back, SkyBox.Face.BACK);
		sb.setTexture(left, SkyBox.Face.LEFT);
		sb.setTexture(right, SkyBox.Face.RIGHT);
		sb.setTexture(top, SkyBox.Face.TOP);
		sb.setTexture(bottom, SkyBox.Face.BOTTOM);
		sm.setActiveSkyBox(sb);
	}
	
	public Vector3 getPlayerPosition() {
		SceneNode dolphinN = sm.getSceneNode("playerNode");
		return dolphinN.getWorldPosition();
	}
	
	public void addGhostAvatarToGameWorld(GhostAvatar avatar) throws IOException{
		
		
		if (avatar != null) {  
			Entity ghostE = sm.createEntity(avatar.getID().toString(), "cube.obj");
			ghostE.setPrimitive(Primitive.TRIANGLES);
			SceneNode ghostN = sm.getRootSceneNode().createChildSceneNode(avatar.getID().toString());
			ghostN.attachObject(ghostE);
			ghostN.setLocalPosition(0, 0, 0);
			float scale = Double.valueOf((Double)jsEngine.get("scale")).floatValue();
			ghostN.scale(Vector3f.createFrom(scale,scale,scale));
			avatar.setNode(ghostN);
			avatar.setEntity(ghostE);
			protClient.addGhostAvatar(avatar);
		} 
	}
	
	public void addAvatarToGameWorld(Avatar avatar, SceneManager sm) throws IOException{
		if (avatar != null) {
		Entity playerE = sm.createEntity("player", "dolphinHighPoly.obj");
		playerE.setPrimitive(Primitive.TRIANGLES);
		SceneNode playerN = sm.getRootSceneNode().createChildSceneNode("playerNode");
		playerN.attachObject(playerE);
		playerN.moveUp(0.3f);
		playerN.yaw(Degreef.createFrom(180.0f));
		avatar.setNode(playerN);
		avatar.setEntity(playerE);
		}
		
	}
	
	public void setIsConnected(boolean bool) {
		isClientConnected = bool;
	}
	
	public void updateGhostAvatarPosition(GhostAvatar ghostAvatar, Vector3 ghostPosition) {
		ghostAvatar.setPosition(ghostPosition.x(),
				ghostPosition.y(),
				ghostPosition.z());
	}
	
	public void removeGhostAvatarFromGameWorld(GhostAvatar avatar) {
		if(avatar != null) gameObjectsToRemove.add(avatar.getID());
	}
	
	private class SendCloseConnectionPacketAction extends AbstractInputAction {

		@Override
		public void performAction(float time, Event evt) {
			if(protClient != null && isClientConnected == true) {
				protClient.sendByeMessage();
			}
		}
	}
}