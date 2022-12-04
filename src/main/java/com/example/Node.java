package com.example;

import akka.NotUsed;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.dispatch.OnComplete;
import akka.pattern.Patterns;
import scala.concurrent.Future;

import java.time.Duration;
import java.util.*;

// #NodeLifeCycleer
public class Node extends AbstractActor {

	public int nodeId;
	public Boolean isAttacker;
	public Boolean online;
	public Integer timeToLive;
	public Set<Integer> triedPeers;
	public Set<Integer> testedPeers;
	public Map<Integer, ActorRef> nodeMap = null;
	public Set<Integer> requestSet;
	public Random random = new Random();
	int connectionTimeout = 5000;

	public interface NodeLifeCycle{}

	public static class ReceiveAllNodeReferences implements NodeLifeCycle {
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
		public ArrayList<Integer> gossipPeers;
		public CommandAddToTest(ArrayList<Integer> gossipPeers){
			this.gossipPeers = gossipPeers;
		}
	}

	public static class BeginSimulate implements NodeLifeCycle{}

	public static class ConnectionRequest implements NodeLifeCycle{
		// public int connectionRequestFrom;
		public ArrayList<Integer> gossipPeers;
		Integer source;
		ConnectionRequest(ArrayList<Integer> gossipPeers, Integer replyTo){
			this.gossipPeers = gossipPeers;
			this.source = replyTo;
		}
	}


	// public static Behavior<NodeLifeCycle> create(int nodeId, Boolean isAttacker, Boolean online, Integer timeToLive, 
	// 									Set<Integer> triedPeers, Set<Integer> testedPeers) {
	// 	return Behaviors.setup(context -> new Node(context, nodeId, isAttacker, online, timeToLive, triedPeers, testedPeers));
	// }

	private Node(int nodeId, Boolean isAttacker, Boolean online, Integer timeToLive, 
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
			this.triedPeers.add(this.random.nextInt(nodeMap.size()));
		}
		gossipNodeToTriedPeers();
	}

	private void gossipNodeToTriedPeers(){
		Node.CommandAddToTried command = new Node.CommandAddToTried(nodeId);
		for(int eachNode:this.triedPeers){
			nodeMap.get(eachNode).tell(command,getSelf());
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
		
			Node.CommandAddToTest command = new Node.CommandAddToTest(gossipPeers);
			nodeMap.get(eachNode).tell(command, getSelf());
		}
	}
	private void addPeersToTest(CommandAddToTest command){
		for(int eachNode: command.gossipPeers){
			if(eachNode!= this.nodeId && !this.triedPeers.contains(eachNode))
				this.testedPeers.add(eachNode);
				//
		}
		// return this;
	}

	public void simulate(BeginSimulate command){
		conectionRequest();
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
		NodeLifeCycle command = new ConnectionRequest(gossipPeers, this.nodeId);
		int randomPeer = this.testedPeers.stream().skip(this.random.nextInt(this.testedPeers.size())).findFirst().orElse(null);
		
		Future<Object> f = Patterns.ask(nodeMap.get(randomPeer), command, connectionTimeout);

		System.out.println("asking...");
        f.onComplete(new OnComplete<Object>(){
            public void onComplete(Throwable t, Object result){
              System.out.println(((ArrayList<Integer>)result));
            }
        }, getContext().getSystem().dispatcher());
		// Flow<ArrayList<Integer>, NodeLifeCycle, NotUsed> askFlow = 
		// 			ActorFlow.ask(nodeMap.get(randomPeer), connectionTimeout, ConnectionRequest::new);
		
		// Source.repeat(command).via(askFlow).map(reply -> reply.gossipPeers).runWith(Sink.seq(), getContext().getSystem());

	}

	public void uponConnectionRequest(ConnectionRequest command){

		if(requestSet.contains(command.source)){
			for(int eachNode:command.gossipPeers) {
				if(eachNode!= this.nodeId && !this.triedPeers.contains(eachNode))
					this.testedPeers.add(eachNode);
			}
			this.triedPeers.add(command.source);
			this.testedPeers.remove(command.source);
		}
		else{
			//repond no , //Create class behaviour answerconnectionrequest -> //update request map //restart simulation
			if(!this.testedPeers.contains(command.connectionRequestFrom))
				this.testedPeers.add(command.connectionRequestFrom);
		}
		// return this;
	}

}


