package com.example;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

import java.util.*;

// #NodeLifeCycleer
public class Node extends AbstractBehavior<Node.NodeLifeCycle> {

	public int nodeId;
	public Boolean isAttacker;
	public Boolean online;
	public Integer timeToLive;
	public Set<Integer> triedPeers;
	public Set<Integer> testedPeers;
	public Map<Integer, ActorRef<Node.NodeLifeCycle>> nodeMap = null;

	public interface NodeLifeCycle{}

	public static class ReceiveAllNodeReferences implements NodeLifeCycle{
		private Map<Integer, ActorRef<Node.NodeLifeCycle>> nodeMap;
		public ReceiveAllNodeReferences(Map<Integer, ActorRef<Node.NodeLifeCycle>> nodeMap){
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

	public static Behavior<NodeLifeCycle> create(int nodeId, Boolean isAttacker, Boolean online, Integer timeToLive, 
										Set<Integer> triedPeers, Set<Integer> testedPeers) {
		return Behaviors.setup(context -> new Node(context, nodeId, isAttacker, online, timeToLive, triedPeers, testedPeers));
	}

	private Node(ActorContext<NodeLifeCycle> context, int nodeId, Boolean isAttacker, Boolean online, Integer timeToLive, 
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
			.onMessage(CommandAddToTest.class, this::addPeersToTest).build();
	}

	private Behavior<NodeLifeCycle> startReceiveAllNodeReferences(ReceiveAllNodeReferences command) {
		// getContext().getLog().info("Hello {}!", command.nodeMap);
		this.nodeMap = command.nodeMap;
		// fillNodeTriedTable();
		System.out.println(this.nodeMap);
		return this;
	}
	private Behavior<NodeLifeCycle> addNodeToTried(CommandAddToTried command){
		this.triedPeers.add(command.tellNodeToAddToTried);
		return this;
	}

	private void fillNodeTriedTable(){
		Random triedPeersCount = new Random();
		int triedPeerCount = triedPeersCount.nextInt(10);
		for(int i =0; i < triedPeerCount; i++){
			this.triedPeers.add(triedPeersCount.nextInt(nodeMap.size()));
		}
		gossipNodeToTriedPeers();
	}

	private void gossipNodeToTriedPeers(){
		Node.CommandAddToTried command = new Node.CommandAddToTried(nodeId);
		for(int eachNode:this.triedPeers){
			nodeMap.get(eachNode).tell(command);
		}
		gossipPeersToTest();
	}
	private void gossipPeersToTest(){
		Random randomPeer = new Random();
		int min = 4, max = 7;
		int gossipPeerCount = randomPeer.nextInt(max-min)+min;
		ArrayList<Integer> gossipPeers = new ArrayList<Integer>();
		for(int i=0; i<gossipPeerCount; i++){
			gossipPeers.add(randomPeer.nextInt(nodeMap.size()));
		}
		Node.CommandAddToTest command = new Node.CommandAddToTest(gossipPeers);
		nodeMap.get(this.nodeId).tell(command);
	}
	private Behavior<NodeLifeCycle> addPeersToTest(CommandAddToTest command){
		for(int eachNode: command.gossipPeers){
			this.testedPeers.add(eachNode);
		}
		return this;
	}

}


