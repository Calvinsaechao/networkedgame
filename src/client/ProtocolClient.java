package client;

import ray.networking.IGameConnection.ProtocolType;
import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;
import java.util.Vector;

import ray.networking.client.GameConnectionClient;
import ray.rml.Vector3;
import ray.rml.Vector3f;

public class ProtocolClient extends GameConnectionClient{
	private chainedGame game;
	private UUID id;
	private Vector<GhostAvatar> ghostAvatars;
	
	public ProtocolClient(InetAddress remAddr, int remPort,
			ProtocolType pType, chainedGame game) throws IOException{
				super(remAddr, remPort, pType);
				this.game=game;
				this.id=UUID.randomUUID();
				this.ghostAvatars = new Vector<GhostAvatar>();
			}
			@Override
			protected void processPacket(Object o) {
				String message = (String)o;
				String[] msgTokens = message.split(",");
				
				if(msgTokens.length > 0) {
					if(msgTokens[0].compareTo("join") == 0) {
						//format: join, success or join, failure
						if(msgTokens[1].compareTo("sucess") == 0) {
							game.setIsConnected(true);
							sendCreateMessage(game.getPlayerPosition());
						}
						if(msgTokens[1].compareTo("failure") == 0) {
							game.setIsConnected(false);
						}
					}
					
					if(msgTokens[0].compareTo("bye") == 0) {
						//format: bye, remoteid
						UUID ghostID = UUID.fromString(msgTokens[1]);
						removeGhostAvatar(ghostID);
					}
					
					if((msgTokens[0].compareTo("dsfr") == 0)
						|| (msgTokens[0].compareTo("create") == 0)){
							//format: create, remoteId, x,y,z or dsfr, remoteID, x,y,z
							UUID ghostID = UUID.fromString(msgTokens[1]);
							Vector3 ghostPosition = Vector3f.createFrom(
									Float.parseFloat(msgTokens[2]),
									Float.parseFloat(msgTokens[3]),
									Float.parseFloat(msgTokens[4]));
							try {
								createGhostAvatar(ghostID, ghostPosition);
							}	catch (Exception e) {
								System.out.println("error creating ghost avatar");
								e.printStackTrace();
							}
					}
					//add more messages
				}
			}
			public void sendJoinMessage() {
				try {
					sendPacket(new String("join," + id.toString()));
				} catch(IOException e) { e.printStackTrace();
				}
			}
			public void sendCreateMessage(Vector3 pos) {
				//format: (create, localId, x,y,z)
				try {
					String message = new String("create," + id.toString());
					message += "," + pos.x() +"," + pos.y() + "," + pos.z();
					sendPacket(message);
				} catch (IOException e) { e.printStackTrace();
				}
			}
			//add send messages
			
			public void createGhostAvatar(UUID ghostID, Vector3 ghostPosition) {
				GhostAvatar avatar = new GhostAvatar(ghostID, ghostPosition);
				try {
					game.addGhostAvatarToGameWorld(avatar);
				} catch(IOException e) { e.printStackTrace();
				}
			}
			
			public void removeGhostAvatar(UUID ghostID) {
				System.out.println("removeGhostAvatar unimplemented");
			}
}

