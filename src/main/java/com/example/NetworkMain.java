package com.example;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

import java.util.Random;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

public class NetworkMain extends AbstractBehavior<NetworkMain.ReceivePeerInformation>{
    public static class ReceivePeerInformation{
        public ReceivePeerInformation() {}
    }
    
    public int numberOfNodes;
    private Map<Integer, ActorRef<Node.NodeLifeCycle>> allNodes = new HashMap<>();
    Random attackerRandom = new Random();
    Random ttlRandom = new Random();

    private NetworkMain(ActorContext<ReceivePeerInformation> context, Integer numberOfNodes) {
        super(context);
        //#create-actors
        this.numberOfNodes = numberOfNodes;
        Boolean isAttacker = false;
        Integer timeToLive;
        Set<Integer> triedPeers;
        Set<Integer> testedPeers;
        for(int i = 0; i < numberOfNodes; i++){
            isAttacker = attackerRandom.nextInt(2) == 0;
            timeToLive = ttlRandom.nextInt(10000) + 10000;
            triedPeers = new HashSet<>();
            testedPeers = new HashSet<>();
            allNodes.put(i, context.spawn(Node.create(i, isAttacker, true, timeToLive,triedPeers, testedPeers), "peer"+i));
        }
        for(int i =0; i < numberOfNodes; i++){
            Node.ReceiveAllNodeReferences command = new Node.ReceiveAllNodeReferences(allNodes);
            allNodes.get(i).tell(command);
        }
    }

    public static Behavior<ReceivePeerInformation> create(Integer numberOfNodes) {
        return Behaviors.setup(context -> new NetworkMain(context, numberOfNodes));
    }

    @Override
    public Receive<ReceivePeerInformation> createReceive() {
        return newReceiveBuilder().onMessage(ReceivePeerInformation.class, this::startSimulate).build();
    }

    private Behavior<ReceivePeerInformation> startSimulate(ReceivePeerInformation info) {
        for(int i=0; i < this.numberOfNodes; i++){
            Node.BeginSimulate command = new Node.BeginSimulate();
            allNodes.get(i).tell(command);
        }
        return this;
    }
}
