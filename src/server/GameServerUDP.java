package server;

import java.util.UUID;
import java.io.IOException;
import java.net.InetAddress;

import ray.networking.server.GameConnectionServer;
import ray.networking.server.IClientInfo;

public class GameServerUDP extends GameConnectionServer<UUID> {

	public GameServerUDP(int localPort) throws IOException {
		super(localPort, ProtocolType.UDP);
	}
	
	@Override
	public void processPacket(Object o, InetAddress senderIP, int sndPort) {
		String message = (String) o;
		String[] msgTokens = message.split(",");
		
		if (msgTokens.length > 0) {
			// server receives JOIN message
			// format : join, localId
			if(msgTokens[0].compareTo("join") == 0) {
				try {
					IClientInfo ci;
					ci = getServerSocket().createClientInfo(senderIP, sndPort);
					UUID clientID = UUID.fromString(msgTokens[1]);
					addClient(ci, clientID);
					sendJoinedMessage(clientID, true);
				}catch(IOException e) {e.printStackTrace();}
			}
			
			// sever receives CREATE message
			// format : create, localId,x,y,z
			if(msgTokens[0].compareTo("create") == 0) {
				UUID clientID = UUID.fromString(msgTokens[1]);
				String[] pos = {msgTokens[2], msgTokens[3], msgTokens[4]};
				sendCreateMessages(clientID, pos);
				sendWantsDetailsMessages(clientID);
			}
			
			// server receives a BYE message
			// format : bye, localId
			if(msgTokens[0].compareTo("bye") == 0) {
				UUID clientID = UUID.fromString(msgTokens[1]);
				sendByeMessages(clientID);
				removeClient(clientID);
			}
			
			//server receives a DETAILS-FOR message
			if(msgTokens[0].compareTo("dsfr") == 0) {
				// CODE HERE
			}
			
			//server receives a MOVE message
			if(msgTokens[0].compareTo("move") == 0) {
				// CODE HERE
			}
		}// main if
		

	}// end process packet

	private void sendCreateMessages(UUID clientID, String[] pos) {
		// format : create, remoteId, x, y, z
		try {
			String message = new String("create," + clientID.toString());
			message += "," + position[0];
			message += "," + position[1];
			message += "," + position[2];
			forwardPacketToAll(message, clientID);
		}catch(IOException e) {e.printStackTrace();}
		
	}

	private void sendJoinedMessage(UUID clientID, boolean success) {
		//format : join, success or join, failure
				try {
					String message = new String("join,");
					if (success) message += "success";
					else message += "failure";
					sendPacket(message, clientID);
				}catch (IOException e) {e.printStackTrace();}
	}

	private void sendByeMessages(UUID clientID) {
		// format : bye, clientId
		try {
			String message = new String("bye," + clientID.toString());
			forwardPacketToAll(message, clientID);
		}catch(IOException e) {e.printStackTrace();}
		
	}

	private void sendWantsDetailsMessages(UUID clientID) {
		// format : wsds, clientID
		try {
			String message = new String("wsds," + clientID.toString());
			forwardPacketToAll(message, clientID);
		}catch(IOException e) {e.printStackTrace();}
		
	}

	private void sendDetailsMessages(UUID clientID) {
		// TODO Auto-generated method stub
		
	}
	
	private void sendMoveMessages(UUID clientID) {
		// TODO Auto-generated method stub
		
	}
	

	
	
}

