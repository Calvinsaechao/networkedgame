package client;

import ray.networking.IGameConnection.ProtocolType;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Vector;

import java.util.Iterator;
import javax.vecmath.Vector3d;

import ray.networking.client.GameConnectionClient;
import ray.rml.Vector3;
import ray.rml.Vector3f;

public class ProtocolClient extends GameConnectionClient{
	private chainedGame game;
	private UUID id;
	private ArrayList<GhostAvatar> ghostAvatars;
	public boolean winner;
	
	public ProtocolClient(InetAddress remAddr, int remPort,
			ProtocolType pType, chainedGame game) throws IOException{
				super(remAddr, remPort, pType);
				this.game=game;
				this.id=UUID.randomUUID();
				this.ghostAvatars = new ArrayList<GhostAvatar>();

			}
			@Override
			protected void processPacket(Object o) {
				String message = (String)o;
				String[] msgTokens = message.split(",");
				
				if(msgTokens.length > 0) {
					if(msgTokens[0].compareTo("join") == 0) {
						//format: join, success or join, failure
						if(msgTokens[1].compareTo("success") == 0) {
							game.setIsConnected(true);
							// call 
							sendCreateMessage(game.getPlayerPosition());
							System.out.println(id.toString()+", you have joined successfully.");
						}
						if(msgTokens[1].compareTo("failure") == 0) {
							game.setIsConnected(false);
							System.out.println(id.toString()+", you have failed to join.");
						}
					}

					
					if(msgTokens[0].compareTo("win") == 0) {
						//format: win, remoteid, boolean
						System.out.println("client recieved packets");
						System.out.println(id.toString());
						System.out.println(msgTokens[1]);
						if (id.toString().equalsIgnoreCase(msgTokens[1])) {
							System.out.println("Test true");
							game.winner();
						}
						else game.playersCollided();
						winner = true;
					}
					
					if(msgTokens[0].compareTo("bye") == 0) {
						//format: bye, remoteid
						UUID ghostID = UUID.fromString(msgTokens[1]);
						removeGhostAvatar(ghostID);
					}
					
					if(msgTokens[0].compareTo("moveCar") == 0) {
						//format: moveCar
						System.out.println("Client recieve moveCar");
						sendMoveMessage(moveCar(id));
						System.out.println("client moved car and updated other clients");
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
								e.printStackTrace();
							}
					}
					if(msgTokens[0].compareTo("wsds") == 0){//wants details
						//format : wsds, senderID
						Vector3 playerPosition = game.getPlayerPosition();//Get the players position
						UUID ghostID = UUID.fromString(msgTokens[1]);
						//UUID avatarID = id;
						sendDetailsForMessage(ghostID, playerPosition);//Send details of the player position to the server
					}
					if(msgTokens[0].compareTo("move") == 0) {
						//format: move, ghostID, x,y,z
						UUID ghostID = UUID.fromString(msgTokens[1]);
						Vector3 ghostPosition = Vector3f.createFrom(
								Float.parseFloat(msgTokens[2]), 
								Float.parseFloat(msgTokens[3]),
								Float.parseFloat(msgTokens[4]));
						updateAvatarPosition(ghostID, ghostPosition);
					}
					if(msgTokens[0].compareTo("orientation") == 0) {
						//format: orientation, ghostID, x,y,z
						UUID ghostID = UUID.fromString(msgTokens[1]);
						Vector3 ghostPosition = Vector3f.createFrom(
								Float.parseFloat(msgTokens[2]), 
								Float.parseFloat(msgTokens[3]),
								Float.parseFloat(msgTokens[4]));
						updateAvatarOrientation(ghostID, ghostPosition);
					}
					//add more messages
				}
			}
			private GhostAvatar getGhostAvatar(UUID id) {
				Iterator<GhostAvatar> it = (Iterator<GhostAvatar>)ghostAvatars.iterator();
				GhostAvatar ghostAvatar = null;
				while(it.hasNext()) {
					ghostAvatar = it.next();
					if (ghostAvatar.getID().compareTo(id) == 0) {
						return ghostAvatar;
					}
				}
				return null;
			}
			private void updateAvatarPosition(UUID ghostID, Vector3 ghostPosition) {
				game.updateGhostAvatarPosition(getGhostAvatar(ghostID), ghostPosition);
				
			}
			private void updateAvatarOrientation(UUID ghostID, Vector3 ghostPosition) {
				game.updateAvatarOrientation(getGhostAvatar(ghostID), ghostPosition);
			}
			public void sendJoinMessage() {
				try {
					sendPacket(new String("join," + id.toString()));
				} catch(IOException e) { e.printStackTrace();
				}
			}
	
			public void sendDetailsForMessage(UUID ghostID, Vector3 playerPosition) {
				// format: dsfr, ghostID, avatarID, x, y, z
				try {
					String message = new String("dsfr," + ghostID.toString() + "," + id.toString());
					message += "," + playerPosition.x() +"," + playerPosition.y() + "," + playerPosition.z();
					sendPacket(message);
				} catch (IOException e) { e.printStackTrace();}
				
			}
			public void sendCreateMessage(Vector3 pos) {
				//format: (create, localId, x,y,z)
				try {
					String message = new String("create," + id.toString());
					message += "," + pos.x() +"," + pos.y() + "," + pos.z();
					sendPacket(message);
				} catch (IOException e) { e.printStackTrace();}
			}
			//add send messages
			
			public void sendWinMessage() {
				try {
					System.out.println("chain calls send msg");
					String message = new String("win," + id.toString());
					sendPacket(message);
				}	catch(IOException e) { e.printStackTrace();}
			}
			
			public void createGhostAvatar(UUID ghostID, Vector3 ghostPosition) {
				GhostAvatar avatar = new GhostAvatar(ghostID, ghostPosition);
				if(!ghostExists(ghostID)) {
					try {
						game.addGhostAvatarToGameWorld(avatar);
						System.out.println("Ghost added");
					} catch(IOException e) { e.printStackTrace();
					}
				}
			}
			
			public void scaleGhostAvatars(float scale) {
				Iterator<GhostAvatar> it = ghostAvatars.iterator();
				while(it.hasNext()) {
					it.next().scale(scale);
				}
			}
			
			private boolean ghostExists(UUID id) {
				Iterator<GhostAvatar> it = ghostAvatars.iterator();
				while(it.hasNext()) {
					if (id.toString().equalsIgnoreCase((it.next()).getID().toString())) {
						return true;
					}
				}
				return false;
			}
			
			public void removeGhostAvatar(UUID ghostID) {
				System.out.println("removeGhostAvatar unimplemented");
			}
			
			public void addGhostAvatar(GhostAvatar avatar) {
				ghostAvatars.add(avatar);
			}
			
			public void sendMoveMessage(Vector3 pos) {
				// format : move, ghostID, x, y ,z 
				String message = new String("move," + id.toString());
				message += "," + pos.x() +"," + pos.y() + "," + pos.z();
				try {
					sendPacket(message);
				} catch (IOException e) {e.printStackTrace();}	
			}
			
			public void sendOrientationMessage(Vector3 orientation) {
				// format : orientation, ghostID, x, y, z
				String message = new String("orientation,") + id.toString();
				message += ","+orientation.x()+","+orientation.y()+","+orientation.z();
				try {
					sendPacket(message);
				}catch (IOException e) {e.printStackTrace();}
			}
			
			public UUID getID() {
				return id;
			}
			
			public Vector3 moveCar(UUID id) {
				return game.moveCar(id);
			} 
			
			public void sendByeMessage() {
				//format: bye, clientID
				String message = "bye,";
				message += id.toString();
				try {
					sendPacket(message);
				} catch (IOException e) {e.printStackTrace();}	
			}
}

