package com.example;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.dispatch.OnComplete;
import akka.pattern.Patterns;
import scala.concurrent.Future;

import java.util.*;

// #NodeLifeCycleer
public class DefenseNode extends AbstractActor {

	public int nodeId;
	public Boolean isAttacker;
	public int attackerRange = 500;
	public Boolean online;
	public Integer timeToLive;
	public Set<Integer> triedPeers;
	public Set<Integer> testedPeers;
	public Map<Integer, ActorRef> nodeMap = null;
	public Set<Integer> requestSet;
	public Map<Integer, TreeNode> masterMap;
	public Map<Integer, TreeNode> toplevelMap;
	public Random random = new Random();
	int connectionTimeout = 5000;

	static class TreeNode{
		int nodeId;
		int count;
		Set<TreeNode> children;
		public TreeNode(int nodeId, int count, Set<TreeNode> children){
			this.nodeId = nodeId;
			this.count = count;
			this.children = children;
		}
	};

	public interface NodeLifeCycle{}

	public static class ReceiveAllNodeReferences implements NodeLifeCycle{
		private Map<Integer, ActorRef> nodeMap;
		public ReceiveAllNodeReferences(Map<Integer, ActorRef> nodeMap){
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
		int peersFromNodeId;
		public ArrayList<Integer> gossipPeers;
		public CommandAddToTest(int peersFromNodeId, ArrayList<Integer> gossipPeers){
			this.peersFromNodeId = peersFromNodeId;
			this.gossipPeers = gossipPeers;
		}
	}

	public static class BeginSimulate implements NodeLifeCycle{}

	public static class ConnectionRequest implements NodeLifeCycle{
		public int connectionRequestFrom;
		public ArrayList<Integer> gossipPeers;
		ConnectionRequest(ArrayList<Integer> gossipPeers, Integer replyTo){
			this.connectionRequestFrom = replyTo;
			this.gossipPeers = gossipPeers;
		}
	}

	public static class ConnectionResponse implements NodeLifeCycle{
		public ArrayList<Integer> gossipPeers;
		Integer connectionRequestFrom;
		ConnectionResponse(ArrayList<Integer> gossipPeers, Integer replyTo){
			this.gossipPeers = gossipPeers;
			this.connectionRequestFrom = replyTo;
		}
	}

	// public static Behavior<NodeLifeCycle> create(int nodeId, Boolean isAttacker, Boolean online, Integer timeToLive, 
	// 									Set<Integer> triedPeers, Set<Integer> testedPeers) {
	// 	return Behaviors.setup(context -> new DefenseNode(context, nodeId, isAttacker, online, timeToLive, triedPeers, testedPeers));
	// }

	private DefenseNode(int nodeId, Boolean isAttacker, Boolean online, Integer timeToLive, 
					Set<Integer> triedPeers, Set<Integer> testedPeers) {
		// super(context);
		this.nodeId = nodeId;
		this.isAttacker = isAttacker;
		this.online = online;
		this.timeToLive = timeToLive;
		this.triedPeers = triedPeers;
		this.testedPeers = testedPeers;
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
			.match(ReceiveAllNodeReferences.class, this::startReceiveAllNodeReferences)
			.match(CommandAddToTried.class, this::addNodeToTried)
			.match(CommandAddToTest.class, this::addPeersToTest)
			.match(BeginSimulate.class, this::simulate)
			.match(ConnectionRequest.class, this::uponConnectionRequest).build();

	}

	private void startReceiveAllNodeReferences(ReceiveAllNodeReferences command) {
		// getContext().getLog().info("Hello {}!", command.nodeMap);
		this.nodeMap = command.nodeMap;
		fillNodeTriedTable();
		System.out.println(this.nodeMap);
		// return this;
	}
	private void addNodeToTried(CommandAddToTried command){
		if(!this.triedPeers.contains(command.tellNodeToAddToTried))
			this.triedPeers.add(command.tellNodeToAddToTried);
		// return this;
	}

	private void fillNodeTriedTable(){
		// Random triedPeersCount = new Random();
		int triedPeerCount = this.random.nextInt(10);
		for(int i =0; i < triedPeerCount; i++){
			int nodeId = this.random.nextInt(nodeMap.size());
			this.triedPeers.add(nodeId);
			TreeNode newNode = new TreeNode(nodeId,0,new HashSet<TreeNode>());
			masterMap.put(nodeId, newNode);
			toplevelMap.put(nodeId, newNode);
			//create tree map master{node id, count of active children, set of children}
			//create TreeNode of tried peer and add to top level treenode map
		}
		gossipNodeToTriedPeers();
	}

	private void gossipNodeToTriedPeers(){
		DefenseNode.CommandAddToTried command = new DefenseNode.CommandAddToTried(nodeId);
		for(int eachNode:this.triedPeers){
			nodeMap.get(eachNode).tell(command, getSelf());
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
		
			DefenseNode.CommandAddToTest command = new DefenseNode.CommandAddToTest(this.nodeId, gossipPeers);
			nodeMap.get(eachNode).tell(command, getSelf());
		}
	}
	private void addPeersToTest(CommandAddToTest command){
		for(int eachNode: command.gossipPeers){
			if(eachNode!= this.nodeId && !this.triedPeers.contains(eachNode) && !masterMap.containsKey(eachNode) )//check if the child already exists in master treenode map
				this.testedPeers.add(eachNode);
				TreeNode newNode = new TreeNode(eachNode,0,new HashSet<TreeNode>());
				masterMap.put(eachNode, newNode);
				masterMap.get(command.peersFromNodeId).children.add(newNode);
				//create treenode for this node, add this to the children set of the one sending the command
		}
		// return this;
	}

	public void simulate(BeginSimulate command){
		if(eclipsed()) {
			System.out.println("Node was eclipsed");
			getContext().getSystem().terminate();
		}
		conectionRequest();
		getSelf().tell(command, getSender());
		// return this;
	}

	private void conectionRequest(){
		int min = 4, max = 7;
		int gossipPeerCount = this.random.nextInt(max-min)+min;
		ArrayList<Integer> gossipPeers = new ArrayList<Integer>();
		for(int i=0; i<gossipPeerCount; i++){
			//take random elements from the tried table
			gossipPeers.add(this.triedPeers.stream().skip(this.random.nextInt(this.triedPeers.size())).findFirst().orElse(null));
		}
		ConnectionRequest command = new ConnectionRequest(gossipPeers, this.nodeId);
		int randomPeer = this.testedPeers.stream().skip(this.random.nextInt(this.testedPeers.size())).findFirst().orElse(null);
		
		Future<Object> f = Patterns.ask(nodeMap.get(randomPeer), command, connectionTimeout);

		System.out.println("asking...");
        f.onComplete(new OnComplete<Object>(){
            public void onComplete(Throwable t, Object result){
				ConnectionResponse response = (ConnectionResponse)result;
				if(response.gossipPeers != null && !response.gossipPeers.isEmpty()){
					for(int eachNode: response.gossipPeers){
						if(eachNode!= nodeId && !triedPeers.contains(eachNode) && !masterMap.containsKey(eachNode) )//check if the child already exists in master treenode map
							testedPeers.add(eachNode);
							TreeNode newNode = new TreeNode(eachNode,0,new HashSet<TreeNode>());
							masterMap.put(eachNode, newNode);
							masterMap.get(response.connectionRequestFrom).children.add(newNode);
							//create treenode for this node, add this to the children set of the one sending the command
					}
					triedPeers.add(command.connectionRequestFrom);
					testedPeers.remove(command.connectionRequestFrom);
					requestSet.clear();
					System.out.println("connection made between " + nodeId + "and" + response.connectionRequestFrom);
				}
				else{
					requestSet.add(randomPeer);
					System.out.println("connection Rejected between " + nodeId + "and" + response.connectionRequestFrom);
				}
            }
        }, getContext().getSystem().dispatcher());

	}

	public void uponConnectionRequest(ConnectionRequest command){

		if(requestSet.contains(command.connectionRequestFrom)){
			// for(int eachNode:command.gossipPeers) {
			// 	if(eachNode!= this.nodeId && !this.triedPeers.contains(eachNode))//check if the child already exists in master treenode map
			// 		this.testedPeers.add(eachNode);
			// 		//create treenode for this node, add this to the children set of the one sending the command
			// }
			for(int eachNode: command.gossipPeers){
				if(eachNode!= this.nodeId && !this.triedPeers.contains(eachNode) && !masterMap.containsKey(eachNode) )//check if the child already exists in master treenode map
					this.testedPeers.add(eachNode);
					TreeNode newNode = new TreeNode(eachNode,0,new HashSet<TreeNode>());
					masterMap.put(eachNode, newNode);
					masterMap.get(command.connectionRequestFrom).children.add(newNode);
					//create treenode for this node, add this to the children set of the one sending the command
			}
			this.triedPeers.add(command.connectionRequestFrom);
			this.testedPeers.remove(command.connectionRequestFrom);
			requestSet.clear();

			int min = 4, max = 7;
			int gossipPeerCount = this.random.nextInt(max-min)+min;
			ArrayList<Integer> gossipPeers = new ArrayList<Integer>();
			for(int i=0; i<gossipPeerCount; i++){
				//take random elements from the tried table
				gossipPeers.add(this.triedPeers.stream().skip(this.random.nextInt(this.triedPeers.size())).findFirst().orElse(null));
			}

			ConnectionResponse response = new ConnectionResponse(gossipPeers, this.nodeId);
			getSender().tell(response, getSelf());
		}
		else{
			//repond no , //Create class behaviour answerconnectionrequest -> //update request map //restart simulation
			if(!this.testedPeers.contains(command.connectionRequestFrom))
				this.testedPeers.add(command.connectionRequestFrom);
			ConnectionResponse response = new ConnectionResponse(null, nodeId);
			getSender().tell(response, getSelf());
				
		}
		// return this;
	}

	private boolean eclipsed(){
		int count = 0;
		for (Integer peer : triedPeers) {
			if(peer < attackerRange){
				count++;
			}
		}
		return count == triedPeers.size();
	}

}


