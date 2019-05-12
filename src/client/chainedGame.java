package client;

import java.awt.Color;
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.geom.AffineTransform;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.lang.Integer;

import ray.physics.PhysicsBallSocketConstraint;
import ray.physics.PhysicsEngine;
import ray.physics.PhysicsObject;
import ray.physics.PhysicsEngineFactory;
import ActionClasses.*;
import Controllers.ElevationController;
import Controllers.NPCcontroller;
import Controllers.RotationController;
import InterfaceClasses.IPlayer;
import net.java.games.input.Controller;
import net.java.games.input.Event;
import ray.audio.AudioManagerFactory;
import ray.audio.AudioResource;
import ray.audio.AudioResourceType;
import ray.audio.IAudioManager;
import ray.audio.Sound;
import ray.audio.SoundType;
import ray.input.GenericInputManager;
import ray.input.InputManager;
import ray.input.action.AbstractInputAction;
import ray.input.action.Action;
import ray.networking.IGameConnection.ProtocolType;
import ray.rage.Engine;
import ray.rage.asset.material.Material;
import ray.rage.asset.material.MaterialManager;
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
import ray.rml.Matrix4;
import ray.rml.Matrix4f;
import ray.rml.Vector3;
import ray.rml.Vector3f;
import ray.rage.scene.SceneManager;
import ray.rage.scene.SceneNode;
import ray.rage.scene.SkeletalEntity;
import ray.rage.scene.SkyBox;
import ray.rage.scene.Tessellation;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.*;

/**
 * TODO:
 * Countdown screen
 * Win screen
 * Collisions with box and coin
 * Sound
 * 
 * NOTES:
 * Start track: x=2.04, z=357
 * End Track: x=2.04, z=-214.2
 * 
 * Car1 start: x=2.04, z=357
 * Car2 start: x=-76.65, z=357
 *
 */

public class chainedGame extends VariableFrameRateGame{
	GL4RenderSystem rs;
	float elapsTime = 0.0f, elapsTimeSec;
	String elapsTimeStr,dispStr;
	private GenericInputManager im;
	private SceneManager sm;
	private static final String SKYBOX_NAME = "SkyBox";
	private ScriptEngine jsEngine;
	private File scriptFile1;
	private long fileLastModifiedTime;
	private Camera3PController orbitController;
	private NPCcontroller npcCtrl;
	
	//players
	private ArrayList<IPlayer> players;
	private SceneNode playerN, manN, cameraN;
	
	//client/server
	private String serverAddress;
	private int serverPort;
	private ProtocolType serverProtocol;
	private ProtocolClient protClient;
	private boolean isClientConnected;
	private Vector<UUID> gameObjectsToRemove;
	
	//physics
	private final static String GROUND_E = "Ground";
	private final static String GROUND_N = "GroundNode";
	private PhysicsEngine physicsEng;
	PhysicsObject carBlueP, carYellowP;
	PhysicsObject chainPhysObj;
	
	// NPC state
	boolean isClose = false;
	boolean animationStarted = false;
	
	// Sound
	IAudioManager audioMgr;
	Sound helloSound, carSound, crashSound;
	
	
	public chainedGame(String serverAddr, int sPort) {
		super();
		this.serverAddress = serverAddr;
		this.serverPort = sPort;
		this.serverProtocol = ProtocolType.UDP;
		scriptFile1 = new File("client\\setGhostParams.js"); //Read up on File Separator
		this.players = new ArrayList<IPlayer>();
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
		GraphicsDevice device = ge.getDefaultScreenDevice();
		DisplaySettingsDialog dispSetting = new DisplaySettingsDialog(device);
		dispSetting.showIt();
		rs.createRenderWindow(dispSetting.getSelectedDisplayMode(), false);
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
		Light plight=sm.createLight("testLamp1", Light.Type.DIRECTIONAL);
		plight.setAmbient(new Color(.1f,.1f,.1f));
		plight.setDiffuse(new Color(.8f,.8f,.8f));
		plight.setSpecular(new Color(1.0f,1.0f,1.0f));
		SceneNode plightNode = sm.getRootSceneNode().createChildSceneNode("plightNode");
		plightNode.attachObject(plight);
		plightNode.setLocalPosition(1f,1f, 1f);
		
		Light dlight = sm.createLight("testLamp2", Light.Type.SPOT);
	    dlight.setAmbient(new Color(0.2f, 0.2f, 0.2f));
	    dlight.setDiffuse(new Color(0.5f, 0.5f, 0.5f));
	    dlight.setSpecular(new Color(1.0f, 1.0f, 1.0f));
	    dlight.setRange(20.0f);
	    dlight.setVisible(true);
		SceneNode dlightNode = sm.getRootSceneNode().createChildSceneNode("dlightNode");
	    dlightNode.attachObject(dlight);
	    dlightNode.moveUp(2.5f);
		
		//--------Relative Objects--------//
		
		makeBox1(sm, (Vector3)Vector3f.createFrom(2.04f, -2.27f, 255f));
		makeBox2(sm, (Vector3)Vector3f.createFrom(-76.65f, -2.27f, 66.66f));
		makeGoldCoin(sm, (Vector3)Vector3f.createFrom(2.04f, -1.0f, 66.66f));
		makeGoldCoin2(sm, (Vector3)Vector3f.createFrom(-76.65f, -1.0f, 255f));
				//makeMan(sm, (Vector3)Vector3f.createFrom(31.9f, -1.8f, 145.07f));
		makeChain(sm, (Vector3)Vector3f.createFrom(-1.9f, -1.0f, 40.2f));
		
		//--------Physics--------//
		makeGround(sm, (Vector3)Vector3f.createFrom(-1.9f, 0f, 40.2f));
		initPhysicsSystem();
		createRagePhysicsWorld();
		
		//-------------Avatars-------------//
		Vector3f playerApos = (Vector3f)Vector3f.createFrom(-2.0f, 0f, 204f);
		Avatar playerA = new Avatar(protClient.getID(), playerApos);
		addAvatarToGameWorld(playerA, sm);
		
		//-------------Terrain-------------//
		Tessellation tessE = sm.createTessellation("tessE", 7);
		tessE.setSubdivisions(9f);
		SceneNode tessN = sm.getRootSceneNode().createChildSceneNode("tessN");
		tessN.attachObject(tessE);
		tessN.translate(Vector3f.createFrom(-6.2f,-2.2f,2.7f));
		//tessN.yaw(Degreef.createFrom(37.2f));
		tessN.scale(900, 1800, 900);
		tessE.setHeightMap(this.getEngine(), "height_map.png");
		tessE.setTexture(this.getEngine(), "road1.png");
		tessE.setNormalMap(this.getEngine(), "normal_map.png");
		
		//--------NPCs--------//
		setupNPC(sm);
		
		
		
		//Script Engine
		ScriptEngineManager factory = new ScriptEngineManager();	
		List <ScriptEngineFactory> list = factory.getEngineFactories();
		jsEngine = factory.getEngineByName("js");
		this.executeScript(jsEngine, scriptFile1);
		
		setupInputs();
		setupOrbitCameras(eng, sm);
		//initAudio(sm);
	}
	
	private void initPhysicsSystem() {
		String engine = "ray.physics.JBullet.JBulletPhysicsEngine";
		float[] gravity = {0, -20f, 0};
		
		physicsEng = PhysicsEngineFactory.createPhysicsEngine(engine);
		physicsEng.initSystem();
		physicsEng.setGravity(gravity);
	}
	
	private void createRagePhysicsWorld() {
		float mass = 1.0f;
		float up[] = {0,1,0};
		double[] temptf;
		float[] size = {2.0f, 2.0f, 2.0f};
		UUID ghostID = (UUID)GhostAvatar.getUUID();
		
		SceneNode root = getEngine().getSceneManager().getRootSceneNode();
		
		SceneNode chainN = (SceneNode)root.getChild("chainNode");
		temptf = toDoubleArray(chainN.getLocalTransform().toFloatArray());
		chainPhysObj = physicsEng.addSphereObject(physicsEng.nextUID(), mass, temptf, 2.0f);
		//chainPhysObj.setBounciness(1.0f);
		chainN.setPhysicsObject(chainPhysObj);
				
		SceneNode gndNode = (SceneNode)root.getChild(GROUND_N);
		temptf = toDoubleArray(gndNode.getLocalTransform().toFloatArray());
		PhysicsObject gndPlaneP = physicsEng.addStaticPlaneObject(physicsEng.nextUID(),
																		temptf, up, 0.0f);
		
		gndPlaneP.setBounciness(1.0f);
		gndNode.scale(3f,.05f,3f);
		gndNode.setPhysicsObject(gndPlaneP);
	
		System.out.println("Created physics world...");
	}
	
	private float[] toFloatArray(double[] arr) {
		if (arr == null) return null;
		int n = arr.length;
		float[] ret = new float[n];
		for (int i = 0; i < n; i++) {
			ret[i] = (float)arr[i];
		}
		return ret;
	}
	
	private double[] toDoubleArray(float[] arr) {
		if (arr == null) return null;
		int n = arr.length;
		double[] ret = new double[n];
		for (int i = 0; i < n; i++) {
			ret[i] = (double)arr[i];
		}
		return ret;
	}
	
	protected void makePlanet(SceneManager sm, Vector3 pos) throws IOException {
		Entity planetE = sm.createEntity("planet", "earth.obj");
		planetE.setPrimitive(Primitive.TRIANGLES);
		SceneNode planetN = sm.getRootSceneNode().createChildSceneNode(planetE.getName() +"Node");
		planetN.attachObject(planetE);
		planetN.setLocalPosition(pos);
	}
	
	protected void makeChain(SceneManager sm, Vector3 pos) throws IOException {
		Entity chainE = sm.createEntity("chain", "chain.obj");
		chainE.setPrimitive(Primitive.TRIANGLES);
		SceneNode chainN = sm.getRootSceneNode().createChildSceneNode(chainE.getName()+"Node");
		chainN.attachObject(chainE);
		chainN.setLocalPosition(pos);
	}
	
	protected void makeBox1 (SceneManager sm, Vector3 pos) throws IOException {
		Entity boxE = sm.createEntity("box1", "box.obj");
		boxE.setPrimitive(Primitive.TRIANGLES);
		SceneNode boxN = sm.getRootSceneNode().createChildSceneNode(boxE.getName() + "Node");
		Texture texBox = this.getEngine().getTextureManager().getAssetByPath("wood.jpg");
		TextureState texBoxState = (TextureState)sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
		texBoxState.setTexture(texBox);
		boxE.setRenderState(texBoxState);
		boxN.attachObject(boxE);
		boxN.scale(3f, 3f, 3f);
		boxN.setLocalPosition(pos);
	}
	
	protected void makeBox2 (SceneManager sm, Vector3 pos) throws IOException {
		Entity boxE = sm.createEntity("box2", "box.obj");
		boxE.setPrimitive(Primitive.TRIANGLES);
		SceneNode boxN = sm.getRootSceneNode().createChildSceneNode(boxE.getName() + "Node");
		Texture texBox = this.getEngine().getTextureManager().getAssetByPath("wood.jpg");
		TextureState texBoxState = (TextureState)sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
		texBoxState.setTexture(texBox);
		boxE.setRenderState(texBoxState);
		boxN.attachObject(boxE);
		boxN.scale(3f, 3f, 3f);
		boxN.setLocalPosition(pos);
	}
	
	protected void makeMan(SceneManager sm, Vector3 pos) throws IOException{
		Entity manSE = sm.createEntity("man", "man.obj");
		manSE.setPrimitive(Primitive.TRIANGLES);
		SceneNode manN = sm.getRootSceneNode().createChildSceneNode(manSE.getName() + "Node");
		Texture texMan = this.getEngine().getTextureManager().getAssetByPath("man_tex.png");
		TextureState texManState = (TextureState)sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
		texManState.setTexture(texMan);
		manSE.setRenderState(texManState);
		manN.attachObject(manSE);
		manN.scale(1.8f, 1.8f, 1.8f);
		manN.setLocalPosition(pos);
	}
	
	
	protected void makeGround(SceneManager sm, Vector3 pos) throws IOException {
		Entity gndE = sm.createEntity(GROUND_E, "cube.obj");
		SceneNode gndNode = sm.getRootSceneNode().createChildSceneNode(GROUND_N);
		gndNode.attachObject(gndE);
	}
	
	protected void makeGoldCoin (SceneManager sm, Vector3 pos) throws IOException {
		MaterialManager mm = sm.getMaterialManager();
		Entity goldCoinE = sm.createEntity("goldCoin", "goldcoin.obj");
		goldCoinE.setPrimitive(Primitive.TRIANGLES);
		SceneNode goldCoinN = sm.getRootSceneNode().createChildSceneNode(goldCoinE.getName() + "Node");
		Texture texGoldCoin = this.getEngine().getTextureManager().getAssetByPath("goldcoin.png");
		TextureState texGoldCoinState = (TextureState)sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
		texGoldCoinState.setTexture(texGoldCoin);
		goldCoinE.setRenderState(texGoldCoinState);
		goldCoinN.attachObject(goldCoinE);
		goldCoinN.setLocalPosition(pos);
		Material goldCoinMat = mm.getAssetByName("goldcoin.mtl");
		goldCoinMat.setShininess(1f);
		goldCoinE.setMaterial(goldCoinMat);
		goldCoinN.scale(2.5f,2.5f,2.5f);
		
		//Node controller
		setupElevationController(goldCoinN, sm);
		setupRotationController(goldCoinN, sm);
	}
	
	protected void makeGoldCoin2 (SceneManager sm, Vector3 pos) throws IOException {
		MaterialManager mm = sm.getMaterialManager();
		Entity goldCoinE = sm.createEntity("goldCoin2", "goldcoin.obj");
		goldCoinE.setPrimitive(Primitive.TRIANGLES);
		SceneNode goldCoinN = sm.getRootSceneNode().createChildSceneNode(goldCoinE.getName() + "Node");
		Texture texGoldCoin = this.getEngine().getTextureManager().getAssetByPath("goldcoin.png");
		TextureState texGoldCoinState = (TextureState)sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
		texGoldCoinState.setTexture(texGoldCoin);
		goldCoinE.setRenderState(texGoldCoinState);
		goldCoinN.attachObject(goldCoinE);
		goldCoinN.setLocalPosition(pos);
		Material goldCoinMat = mm.getAssetByName("goldcoin.mtl");
		goldCoinMat.setShininess(1f);
		goldCoinE.setMaterial(goldCoinMat);
		goldCoinN.scale(2.5f,2.5f,2.5f);
		
		//Node controller
		setupElevationController(goldCoinN, sm);
		setupRotationController(goldCoinN, sm);
	}
	
	public void updateVerticalPosition() {
		SceneNode avatarN = this.getEngine().getSceneManager().getSceneNode("playerNode");
		SceneNode tessN = this.getEngine().getSceneManager().getSceneNode("tessN");
		Tessellation tessE = ((Tessellation)tessN.getAttachedObject("tessE"));
		
		// get avatar position relative to plane
		Vector3 worldAvatarPosition = avatarN.getWorldPosition();
		Vector3 localAvatarPosition = avatarN.getLocalPosition();
		
		// use avatar WORLD coordinates to get coordinates for height
		Vector3 newAvatarPosition = Vector3f.createFrom(localAvatarPosition.x(),
														 tessE.getWorldHeight(worldAvatarPosition.x(), worldAvatarPosition.z())+ 2f,
														 localAvatarPosition.z());
		// use avatar LOCAL coordinates to set position, including height
		avatarN.setLocalPosition(newAvatarPosition);
	}
	protected void setupInputs() {
		im = new GenericInputManager();
		ArrayList<Controller> controllers = im.getControllers();
		
		SceneNode AvatarN = sm.getSceneNode("playerNode");
		Action moveRightAction = new MoveRtAction(AvatarN, protClient, this);
		Action moveBackwardAction = new MoveBackwardAction(AvatarN, protClient, this);
		Action moveForwardAction = new MoveFwdAction(AvatarN, protClient, this);
		Action moveLeftAction = new MoveLeftAction(AvatarN, protClient, this);
		Action orbitAroundAction = new OrbitRightAction(orbitController);
		Action orbitLeftAction = new OrbitLeftAction(orbitController);
		Action orbitUpAction = new OrbitUpAction(orbitController);
    	Action orbitDownAction = new OrbitDownAction(orbitController);
		Action sendCloseConPckAction = new SendCloseConnectionPacketAction();
		Action turnAction = new TurnAction(AvatarN, protClient,this);
		
		for (Controller c : controllers) {
   		 if (c.getType() == Controller.Type.KEYBOARD) {
   			 im.associateAction(c,
				net.java.games.input.Component.Identifier.Key.W,
				moveForwardAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
   			 im.associateAction(c,
				net.java.games.input.Component.Identifier.Key.Q,
				moveLeftAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
   			 im.associateAction(c,
   					net.java.games.input.Component.Identifier.Key.E,
   					moveRightAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
   			 im.associateAction(c,
   					net.java.games.input.Component.Identifier.Key.S,
   					moveBackwardAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
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
 			im.associateAction(c,
					 net.java.games.input.Component.Identifier.Key.A, 
					 turnAction,InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
 			im.associateAction(c,
					 net.java.games.input.Component.Identifier.Key.D, 
					 turnAction,InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
   		 }
   		 if (c.getType() == Controller.Type.MOUSE) {
   			 im.associateAction(c,
   					net.java.games.input.Component.Identifier.Button.LEFT,
   					moveForwardAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
   		 }
		}
	}
	
	//-------------Orbit Camera-------------//
	
	protected void setupOrbitCameras(Engine eng, SceneManager sm) {
		SceneNode avatarN = sm.getSceneNode("playerNode");
    	cameraN = sm.getSceneNode("MainCameraNode");
    	Camera camera = sm.getCamera("MainCamera");
    	camera.setMode('n');
    	cameraN.moveBackward(3f);
    	orbitController = new Camera3PController(camera, cameraN, avatarN, im);
	}
	
	protected void setupElevationController(SceneNode target, SceneManager sm) {
    	ElevationController ecE = new ElevationController(target, .015f, .5f);
    	ecE.addNode(target);
    	sm.addController(ecE);
    }
	
	protected void setupRotationController(SceneNode target, SceneManager sm) {
		RotationController ecE = new RotationController(target, .75f);
		ecE.addNode(target);
		sm.addController(ecE);
	}

	@Override
	protected void update(Engine engine) {
		rs = (GL4RenderSystem) engine.getRenderSystem();
		SceneManager sm = engine.getSceneManager();
		elapsTime += engine.getElapsedTimeMillis();
		elapsTimeSec = Math.round(elapsTime/1000.0f);
		elapsTimeStr = Integer.toString((int) elapsTimeSec);
		dispStr = "PLAYER 1 SCORE: " + elapsTimeStr + "   PLAYER 2 SCORE: " + elapsTimeStr;  
		rs.setHUD(dispStr);
		processNetworking(elapsTime, sm);
		long modTime = scriptFile1.lastModified();
		if(modTime > fileLastModifiedTime) {
			System.out.println("Updating file: " + scriptFile1.toString());
			fileLastModifiedTime = modTime;
			this.executeScript(jsEngine, scriptFile1);
			float scale = Double.valueOf((Double)jsEngine.get("scale")).floatValue();
			protClient.scaleGhostAvatars(scale);
		}
		
		// PHYSICS
		
		Matrix4 mat;
		physicsEng.update(engine.getElapsedTimeMillis());
		for(SceneNode s : sm.getSceneNodes()) {
			if (s.getPhysicsObject()!=null) {
				mat = Matrix4f.createFrom(toFloatArray(
						s.getPhysicsObject().getTransform()));
				s.setLocalPosition(mat.value(0, 3), mat.value(1, 3), mat.value(2, 3));
			}
		}
		
		
		im.update(elapsTime);
		
		// NPC
		orbitController.updateCameraPosition();
		SkeletalEntity manSE = (SkeletalEntity)engine.getSceneManager().getEntity("man");
		checkNPCCollision();
		if (((int)elapsTime)%2==0) {
			manSE.update(); }
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
		double[] temptf;
		float mass = 1.0f;
		float[] size = {2.0f, 2.0f, 2.0f};
		
		if (avatar != null) {  
			Entity playerE = sm.createEntity(avatar.getID().toString(), "ghost_model.obj");
			playerE.setPrimitive(Primitive.TRIANGLES);
			SceneNode playerN = sm.getRootSceneNode().createChildSceneNode(avatar.getID().toString());
			Material carMat = this.getEngine().getMaterialManager().getAssetByPath("ghost_model.mtl");
			Texture texCar = this.getEngine().getTextureManager().getAssetByPath("ghost_tex.png");
			TextureState texCarState = (TextureState)sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
			texCarState.setTexture(texCar);
			playerE.setRenderState(texCarState);
			playerN.attachObject(playerE);
			playerN.scale(2.3f, 2.3f, 2.3f);
			//playerN.moveUp(0.3f);
			playerN.setLocalPosition(-76.65f, -0.2f, 357f);
			avatar.setNode(playerN);
			avatar.setEntity(playerE);
			playerN.yaw(Degreef.createFrom(180));
			protClient.addGhostAvatar(avatar);
			players.add(avatar);
			//Physics
			/**
			SceneNode carYellow = playerN.createChildSceneNode("physicsCarY");
			temptf = toDoubleArray(carYellow.getLocalTransform().toFloatArray());
			carYellowP = physicsEng.addBoxObject(physicsEng.nextUID(), mass, temptf, size);
			//PhysicsBallSocketConstraint connection = physicsEng.addBallSocketConstraint(physicsEng.nextUID(), carYellowP, chainPhysObj);
			
			System.out.println("Ball socket created...");
			**/
		} 
	}
	
	public void addAvatarToGameWorld(Avatar avatar, SceneManager sm) throws IOException{
		double[] temptf;
		float mass = 1.0f;
		float[] size = {2.0f, 2.0f, 2.0f};
		if (avatar != null) {
			Entity playerE = sm.createEntity("player", "car_model.obj");
			playerE.setPrimitive(Primitive.TRIANGLES);
			playerN = sm.getRootSceneNode().createChildSceneNode("playerNode");
			Material carMat = this.getEngine().getMaterialManager().getAssetByPath("car_model.mtl");
			Texture texCar = this.getEngine().getTextureManager().getAssetByPath("car_tex.png");
			TextureState texCarState = (TextureState)sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
			texCarState.setTexture(texCar);
			playerE.setRenderState(texCarState);
			playerN.attachObject(playerE);
			playerN.scale(2.3f, 2.3f, 2.3f);
			playerN.setLocalPosition(2.04f, -0.2f, 357f);
			avatar.setNode(playerN);
			avatar.setEntity(playerE);
			playerN.yaw(Degreef.createFrom(180));
			players.add(avatar);
			//Physics //
			
			SceneNode carBlue = playerN.createChildSceneNode("physicsCarB");
			temptf = toDoubleArray(playerN.getLocalTransform().toFloatArray());
			carBlueP = physicsEng.addBoxObject(physicsEng.nextUID(), mass, temptf, size);
			carBlue.setPhysicsObject(carBlueP);

			PhysicsBallSocketConstraint connection = physicsEng.addBallSocketConstraint(physicsEng.nextUID(), chainPhysObj, carBlueP);
			connection.getBodyA().setTransform(temptf);
			System.out.println("I dont know...");  
		
		}
		
	}
	
	public void setupNPC(SceneManager sm) throws IOException {
		Vector3 pos = (Vector3)Vector3f.createFrom(52.04f, 1f, 255.9f);
		SkeletalEntity manSE = sm.createSkeletalEntity("man", "man_animated.rkm", "man_animated.rks");
		//Entity manSE = sm.createEntity("man", "man.obj");
		manSE.setPrimitive(Primitive.TRIANGLES);
		manN = sm.getRootSceneNode().createChildSceneNode(manSE.getName() + "Node");
		Texture texMan = this.getEngine().getTextureManager().getAssetByPath("man_animated.png");
		TextureState texManState = (TextureState)sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
		texManState.setTexture(texMan);
		manSE.setRenderState(texManState);
		manN.attachObject(manSE);
		manN.scale(2f, 2f, 2f);
		manN.setLocalPosition(pos);
		manN.yaw(Degreef.createFrom(-65));
		//makeMan(sm, (Vector3)Vector3f.createFrom(31.9f, -1.8f, 145.07f));
		npcCtrl = new NPCcontroller(manN);
		//load animations
		manSE.loadAnimation("man_wave", "man_animated.rka");
		//doTheWave();
	}
	
	private void doTheWave() {
		SkeletalEntity manSE = (SkeletalEntity)getEngine().getSceneManager().getEntity("man");
		if (isClose) { 
			if (animationStarted==false) {
				manSE.playAnimation("man_wave", 1f, SkeletalEntity.EndType.LOOP, 3);
				//System.out.println("doing the wave");
				animationStarted=true;
				//System.out.println("anim=true");
				}
		}else {
			if(animationStarted==true) {
				//stop 
				//System.out.println("anim is true - stop");
				manSE.stopAnimation();
				animationStarted=false;
				//System.out.println("anim=false");
			}
		}
		//System.out.println("doing the wave");
	}  
	
	public void setIsConnected(boolean bool) {
		isClientConnected = bool;
	}
	
	public void updateGhostAvatarPosition(GhostAvatar ghostAvatar, Vector3 ghostPosition) {
		ghostAvatar.setPosition(ghostPosition.x(),
				ghostPosition.y(),
				ghostPosition.z());
	}
	
	public void updateAvatarOrientation(GhostAvatar avatar, Vector3 ghostOrientation) {
		avatar.setOrientation(ghostOrientation.x(), ghostOrientation.y(), ghostOrientation.z());
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
	
	public void checkNPCCollision() {
		Vector3 man1pos = (Vector3)Vector3f.createFrom(31.9f, 1f, 145.07f);
		for (IPlayer p : players) {
			Vector3 playerPos = p.getPosition();
			float distX = (float) Math.sqrt(Math.pow(playerPos.x()-man1pos.x(),2));
			float distZ = (float) Math.sqrt(Math.pow(playerPos.z()-man1pos.z(),2));
			if(distX <= 100.0f) {
					if(distZ <= 100.0f) {
						isClose = true;
						//System.out.println("isWaving=true");
						doTheWave();
					}
			}else {
				isClose=false;
			}
			
		}
	}
	
	public void initAudio(SceneManager sm) {
		AudioResource resource1, resource2, resource3;
		audioMgr = AudioManagerFactory.createAudioManager("ray.audio.joal.JOALAudioManager");
		if (!audioMgr.initialize()) {
			System.out.println("Audio Manager failed to initialize! ");
			return;
		}
		//resource1 = audioMgr.createAudioResource("defaultsound.wav", AudioResourceType.AUDIO_SAMPLE);
		resource2 = audioMgr.createAudioResource("assets/sounds/car_moving_loop.wav", AudioResourceType.AUDIO_SAMPLE);
		//resource3 = audioMgr.createAudioResource("defaultsound.wav", AudioResourceType.AUDIO_SAMPLE);
		
		//helloSound = new Sound (resource1, SoundType.SOUND_EFFECT, 100, true);
		carSound = new Sound (resource2, SoundType.SOUND_EFFECT, 100, true);
		//crashSound = new Sound (resource3, SoundType.SOUND_EFFECT, 100, true);
		
		//helloSound.initialize(audioMgr);
		carSound.initialize(audioMgr);
		//crashSound.initialize(audioMgr);
		
		//helloSound.setMaxDistance(10.0f);
		//helloSound.setMinDistance(0.5f);
		//helloSound.setRollOff(5.0f);
		
		carSound.setMaxDistance(10.0f);
		carSound.setMinDistance(0.5f);
		carSound.setRollOff(5.0f);
		
		//crashSound.setMaxDistance(10.0f);
		//crashSound.setMinDistance(0.5f);
		//crashSound.setRollOff(5.0f);
		
		SceneNode carN = sm.getSceneNode("playerNode");
		SceneNode manN = sm.getSceneNode("manNode");
		carSound.setLocation(carN.getWorldPosition());
		helloSound.setLocation(manN.getWorldPosition());
		setEarParameters(sm);
		
		//helloSound.play();
		carSound.play();
		
	}
	
	public Vector3 moveCar(UUID id) {
		Iterator<IPlayer> i = players.iterator();
		while(i.hasNext()) {
			IPlayer player = i.next();
			if (player.getID() == id) {
				Avatar a = (Avatar)player;
				a.setPosition(-76.65f, -.2f, 357f);
				System.out.println("Move Car Success!");
				return a.getPosition();
			}
		}
		return null;
	}
	
	public void setEarParameters(SceneManager sm) {
		SceneNode cameraN = sm.getSceneNode("MainCameraNode");
		Vector3 camDir = cameraN.getWorldForwardAxis();
		
		audioMgr.getEar().setLocation(cameraN.getWorldPosition());
		audioMgr.getEar().setOrientation(camDir, Vector3f.createFrom(0,1,0));
		}
	
}// end game
