package com.example;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

import java.util.*;

// #NodeLifeCycleer
public class DefenseNode extends AbstractBehavior<DefenseNode.NodeLifeCycle> {

	public int nodeId;
	public Boolean isAttacker;
	public Boolean online;
	public Integer timeToLive;
	public Set<Integer> triedPeers;
	public Set<Integer> testedPeers;
	public Map<Integer, ActorRef<DefenseNode.NodeLifeCycle>> nodeMap = null;
	public Set<Integer> requestSet;
	public Random random = new Random();

	public interface NodeLifeCycle{}

	public static class ReceiveAllNodeReferences implements NodeLifeCycle{
		private Map<Integer, ActorRef<DefenseNode.NodeLifeCycle>> nodeMap;
		public ReceiveAllNodeReferences(Map<Integer, ActorRef<DefenseNode.NodeLifeCycle>> nodeMap){
			this.nodeMap = nodeMap;
		}
	}
	public static class CommandAddToTried implements NodeLifeCycle {
		public int tellNodeToAddToTried;
		
		public CommandAddToTried(int tellNodeToAddToTried){
			this.tellNodeToAddToTried = tellNodeToAddToTried;
		}
	}

	public static class CommandAddToTest implements NodeLifeCycle {
		public ArrayList<Integer> gossipPeers;
		public CommandAddToTest(ArrayList<Integer> gossipPeers){
			this.gossipPeers = gossipPeers;
		}
	}

	public static class BeginSimulate implements NodeLifeCycle{}

	public static class ConnectionRequest implements NodeLifeCycle{
		public int connectionRequestFrom;
		public ArrayList<Integer> gossipPeers;
		ConnectionRequest(int connectionRequestFrom, ArrayList<Integer> gossipPeers ){
			this.connectionRequestFrom = connectionRequestFrom;
			this.gossipPeers = gossipPeers;
		}
	}


	public static Behavior<NodeLifeCycle> create(int nodeId, Boolean isAttacker, Boolean online, Integer timeToLive, 
										Set<Integer> triedPeers, Set<Integer> testedPeers) {
		return Behaviors.setup(context -> new DefenseNode(context, nodeId, isAttacker, online, timeToLive, triedPeers, testedPeers));
	}

	private DefenseNode(ActorContext<NodeLifeCycle> context, int nodeId, Boolean isAttacker, Boolean online, Integer timeToLive, 
					Set<Integer> triedPeers, Set<Integer> testedPeers) {
		super(context);
		this.nodeId = nodeId;
		this.isAttacker = isAttacker;
		this.online = online;
		this.timeToLive = timeToLive;
		this.triedPeers = triedPeers;
		this.testedPeers = testedPeers;
	}

	@Override
	public Receive<NodeLifeCycle> createReceive() {
		return newReceiveBuilder()
			.onMessage(ReceiveAllNodeReferences.class, this::startReceiveAllNodeReferences)
			.onMessage(CommandAddToTried.class, this::addNodeToTried)
			.onMessage(CommandAddToTest.class, this::addPeersToTest)
			.onMessage(BeginSimulate.class, this::simulate)
			.onMessage(ConnectionRequest.class, this::uponConnectionRequest).build();

	}

	private Behavior<NodeLifeCycle> startReceiveAllNodeReferences(ReceiveAllNodeReferences command) {
		// getContext().getLog().info("Hello {}!", command.nodeMap);
		this.nodeMap = command.nodeMap;
		fillNodeTriedTable();
		System.out.println(this.nodeMap);
		return this;
	}
	private Behavior<NodeLifeCycle> addNodeToTried(CommandAddToTried command){
		if(!this.triedPeers.contains(command.tellNodeToAddToTried))
			this.triedPeers.add(command.tellNodeToAddToTried);
		return this;
	}

	private void fillNodeTriedTable(){
		// Random triedPeersCount = new Random();
		int triedPeerCount = this.random.nextInt(10);
		for(int i =0; i < triedPeerCount; i++){
			this.triedPeers.add(this.random.nextInt(nodeMap.size()));
			//create tree map master{node id, count of active children, set of children}
			//create TreeNode of tried peer and add to top level treenode map
		}
		gossipNodeToTriedPeers();
	}

	private void gossipNodeToTriedPeers(){
		DefenseNode.CommandAddToTried command = new DefenseNode.CommandAddToTried(nodeId);
		for(int eachNode:this.triedPeers){
			nodeMap.get(eachNode).tell(command);
		}
		gossipPeersToTest();
	}
	private void gossipPeersToTest(){
		// Random randomPeer = new Random();
		int min = 4, max = 7;
		ArrayList<Integer> gossipPeers = new ArrayList<Integer>();
		for(int eachNode:this.triedPeers){
			int gossipPeerCount = this.random.nextInt(max-min)+min;
			gossipPeers.clear();
			for(int i=0; i<gossipPeerCount; i++){
				gossipPeers.add(this.triedPeers.stream().skip(this.random.nextInt(this.triedPeers.size())).findFirst().orElse(null));
			}
		
			DefenseNode.CommandAddToTest command = new DefenseNode.CommandAddToTest(gossipPeers);
			nodeMap.get(eachNode).tell(command);
		}
	}
	private Behavior<NodeLifeCycle> addPeersToTest(CommandAddToTest command){
		for(int eachNode: command.gossipPeers){
			if(eachNode!= this.nodeId && !this.triedPeers.contains(eachNode))//check if the child already exists in master treenode map
				this.testedPeers.add(eachNode);
				//create treenode for this node, add this to the children set of the one sending the command
		}
		return this;
	}

	public Behavior<NodeLifeCycle> simulate(BeginSimulate command){
		conectionRequest();
		return this;
	}

	private void conectionRequest(){
		int min = 4, max = 7;
		int gossipPeerCount = this.random.nextInt(max-min)+min;
		ArrayList<Integer> gossipPeers = new ArrayList<Integer>();
		for(int i=0; i<gossipPeerCount; i++){
			//take random elements from the tried table
			gossipPeers.add(this.triedPeers.stream().skip(this.random.nextInt(this.triedPeers.size())).findFirst().orElse(null));
		}
		DefenseNode.ConnectionRequest command = new DefenseNode.ConnectionRequest(this.nodeId,gossipPeers);
		int randomPeer = this.testedPeers.stream().skip(this.random.nextInt(this.testedPeers.size())).findFirst().orElse(null);
		// nodeMap.get(randomPeer).ask(command);

	}

	public Behavior<NodeLifeCycle> uponConnectionRequest(ConnectionRequest command){

		if(requestSet.contains(command.connectionRequestFrom)){
			for(int eachNode:command.gossipPeers) {
				if(eachNode!= this.nodeId && !this.triedPeers.contains(eachNode))//check if the child already exists in master treenode map
					this.testedPeers.add(eachNode);
					//create treenode for this node, add this to the children set of the one sending the command
			}
			this.triedPeers.add(command.connectionRequestFrom);
		}
		else{
			//repond no , //Create class behaviour answerconnectionrequest -> //update request map //restart simulation
			if(!this.testedPeers.contains(command.connectionRequestFrom))
				this.testedPeers.add(command.connectionRequestFrom);
				
		}
		return this;
	}



}


