package Controllers;

import client.NPC;
import ray.rage.scene.Node;
import ray.rage.scene.SceneNode;
import ray.rml.Degreef;

public class NPCcontroller{
	//private int numNPCs = 5;
	//private NPC[] NPClist = new NPC[numNPCs];
	private SceneNode npc;
	private boolean cheering = false;
	private boolean idle = true;
	/*
	public void setupNPCs() {
		//...
	} */
	public NPCcontroller(SceneNode npcN) {
		npc = npcN;
	}
	
	// Assume initial/default state is idle
	// state changes if a car approaches from idle to cheering
	// assume if state == 1 --> idle
	//		  if state == 2 --> cheering
	// possible future state if car crashes, state changes to sad?
	public int getState() {
		int x=0;
		if (cheering) x=2;
		else if (idle) x=1;
		return x;
	}
	
	public void setState(int x) {
		if (x==1) {
			idle = true;
			cheering = false;
		}else if (x==2) {
			cheering = true;
			idle = false;
		}
	}
	
	

}
