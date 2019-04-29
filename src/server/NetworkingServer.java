package server;

import java.io.IOException;
import ray.networking.IGameConnection.ProtocolType;

public class NetworkingServer {
	private GameServerUDP thisUDPServer;
	
	private NPCcontroller npcCtrl;
	long lastUpdateTime;
	
	
	public NetworkingServer (int serverPort, String protocol) {
		long startTime = System.nanoTime();
		lastUpdateTime = startTime;
		npcCtrl = new NPCcontroller();
		
		try {
			if (protocol.toUpperCase().compareTo("UDP") == 0) {
				thisUDPServer = new GameServerUDP(serverPort);}
		}catch(IOException e) { e.printStackTrace();}
		
		npcCtrl.setupNPCs();
		npcLoop();
	}
	
	public void npcLoop() {
		while(true) {
			long frameStartTime = System.nanoTime();
			float elapMilSec =  (frameStartTime-lastUpdateTime)/(1000000.0f);
			if(elapMilSec >= 50.0f) {
				lastUpdateTime = frameStartTime;
				 npcCtrl.updateNPCs();       
				 thisUDPServer.sendNPCinfo();
			}
			Thread.yield();
		}
	}
	
	public static void main(String[] args) {
		if (args.length > 1) {
			NetworkingServer app = new NetworkingServer(Integer.parseInt(args[0]), args[1]);
		}
	}

}
