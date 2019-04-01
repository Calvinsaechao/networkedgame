package client;

import java.awt.Color;
import java.awt.DisplayMode;
import java.awt.GraphicsEnvironment;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.UUID;
import java.util.Vector;

import ActionClasses.*;
import ray.input.GenericInputManager;
import ray.input.InputManager;
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

public class chainedGame extends VariableFrameRateGame{
	GL4RenderSystem rs;
	float elapsTime = 0.0f;
	private GenericInputManager im;
	private SceneManager sm;
	private static final String SKYBOX_NAME = "SkyBox";
	
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
		
	
		/*
		camera.setRt((Vector3f)Vector3f.createFrom(1.0f, 0.0f, 0.0f));
		camera.setUp((Vector3f)Vector3f.createFrom(0.0f, 1.0f, 0.0f));
		camera.setFd((Vector3f)Vector3f.createFrom(0.0f, 0.0f, 1.0f));
		camera.setPo((Vector3f)Vector3f.createFrom(0.0f, 0.0f, 0.0f));
		*/
		
		SceneNode cameraNode = rootNode.createChildSceneNode(camera.getName() + "Node");
		cameraNode.attachObject(camera);
		camera.setMode('n');
		cameraNode.moveBackward(3.0f);
		camera.getFrustum().setFarClipDistance(1000.0f);
	}

	@Override
	protected void setupScene(Engine eng, SceneManager sm) throws IOException {
		// TODO Auto-generated method stub
		this.sm =sm;
		im = new GenericInputManager();
		setupNetworking();
		// make skybox
		makeSkybox(eng,sm);
		// make terrain
		// make avatars
		sm.getAmbientLight().setIntensity(new Color(.3f, .3f, .3f));
		Light plight=sm.createLight("testLamp1", Light.Type.POINT);
		plight.setAmbient(new Color(.1f,.1f,.1f));
		plight.setDiffuse(new Color(.8f,.8f,.8f));
		plight.setSpecular(new Color(1.0f,1.0f,1.0f));
		plight.setRange(60f);
		
		SceneNode plightNode = sm.getRootSceneNode().createChildSceneNode("plightNode");
		plightNode.attachObject(plight);
		plightNode.setLocalPosition(1f,1f, 1f);
		
		Vector3f playerApos = (Vector3f)Vector3f.createFrom(0f,0f,0f);
		Avatar playerA = new Avatar(protClient.getID(), playerApos);
		addAvatarToGameWorld(playerA, sm);
		
		Vector3f ghostApos = (Vector3f)Vector3f.createFrom(0f,0f,0f);
		GhostAvatar ghostA = new GhostAvatar(protClient.getID(), ghostApos);
		//addGhostAvatarToGameWorld(ghostA);
		
		setupInputs();
	}
	
	protected void setupInputs() {
		im = new GenericInputManager();
		String kbName = im.getKeyboardName();
		
		SceneNode AvatarN = sm.getSceneNode("playerNode");
		Action moveForwardAction = new MoveForwardAction(AvatarN, protClient);
		Action moveBackwardAction = new MoveBackwardAction(AvatarN, protClient);
		im.associateAction(kbName,
				net.java.games.input.Component.Identifier.Key.W,
				moveForwardAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateAction(kbName,
				net.java.games.input.Component.Identifier.Key.S,
				moveBackwardAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
	}

	@Override
	protected void update(Engine engine) {
		// TODO Auto-generated method stub
		rs = (GL4RenderSystem) engine.getRenderSystem();
		SceneManager sm = engine.getSceneManager();
		elapsTime += engine.getElapsedTimeMillis();
		im.update(elapsTime);
		processNetworking(elapsTime, sm);
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
			SceneNode ghostN = sm.getRootSceneNode().createChildSceneNode(
					avatar.getID().toString());
			ghostN.attachObject(ghostE);
					ghostN.setLocalPosition(0, 0, 0);
					avatar.setNode(ghostN);
					avatar.setEntity(ghostE);
					protClient.addGhostAvatar(avatar);
					//avatar.setPosition(0, 0, 0);
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
		//playerN.setLocalPosition(0,0,0);
		avatar.setNode(playerN);
		avatar.setEntity(playerE);
		//avatar.setPosition(0, 0, 0);
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
}