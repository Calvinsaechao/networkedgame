package client;

import java.awt.DisplayMode;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.UUID;
import java.util.Vector;

import ray.input.GenericInputManager;
import ray.networking.IGameConnection.ProtocolType;
import ray.rage.Engine;
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
import ray.rml.Vector3;
import ray.rml.Vector3f;
import ray.rage.scene.SceneManager;
import ray.rage.scene.SceneNode;

public class chainedGame extends VariableFrameRateGame{
	GL4RenderSystem rs;
	float elapsTime = 0.0f;
	private GenericInputManager im;
	private SceneManager sm;
	
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
		// TODO Auto-generated method stub
		SceneNode rootNode = sm.getRootSceneNode();
		Camera camera = sm.createCamera("MainCamera", Projection.PERSPECTIVE);
		camera.setMode('n');
		
		rw.getViewport(0).setCamera(camera);
		
		camera.setRt((Vector3f)Vector3f.createFrom(1.0f, 0.0f, 0.0f));
		camera.setUp((Vector3f)Vector3f.createFrom(0.0f, 1.0f, 0.0f));
		camera.setFd((Vector3f)Vector3f.createFrom(0.0f, 0.0f, -1.0f));
		camera.setPo((Vector3f)Vector3f.createFrom(0.0f, 0.0f, 0.0f));
		
		SceneNode cameraNode = rootNode.createChildSceneNode(camera.getName() + "Node");
		cameraNode.attachObject(camera);
	}

	@Override
	protected void setupScene(Engine arg0, SceneManager arg1) throws IOException {
		// TODO Auto-generated method stub
		im = new GenericInputManager();
		// make skybox
		// make terrain
		// make avatars
		
	}

	@Override
	protected void update(Engine engine) {
		// TODO Auto-generated method stub
		rs = (GL4RenderSystem) engine.getRenderSystem();
		sm = engine.getSceneManager();
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
	
	public Vector3 getPlayerPosition() {
		SceneNode dolphinN = sm.getSceneNode("dolphinNode");
		return dolphinN.getWorldPosition();
	}
	
	public void addGhostAvatarToGameWorld(GhostAvatar avatar) throws IOException{
		if (avatar != null) {  
			Entity ghostE = sm.createEntity("ghost", "whatever.obj");
			ghostE.setPrimitive(Primitive.TRIANGLES);
			SceneNode ghostN = sm.getRootSceneNode().createChildSceneNode(
					avatar.getID().toString());
			ghostN.attachObject(ghostE);
					ghostN.setLocalPosition(0, 0, 0);
					avatar.setNode(ghostN);
					avatar.setEntity(ghostE);
					avatar.setPosition(0, 0, 0); } 
	}
	
	public void addAvatarToGameWorld(Avatar avatar) throws IOException{
		
	}
	
	public void setIsConnected(boolean bool) {
		isClientConnected = bool;
	}
}