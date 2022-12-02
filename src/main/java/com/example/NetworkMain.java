package com.example;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

import java.util.Random;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NetworkMain extends AbstractBehavior<NetworkMain.ReceivePeerInformation>{
    public static class ReceivePeerInformation{
        
        public ReceivePeerInformation() {
        } 
    }

    
    private Map<Integer, ActorRef<Node.Greet>> allNodes;
    Random attackerRandom = new Random();
    Random ttlRandom = new Random();

    private NetworkMain(ActorContext<ReceivePeerInformation> context, Integer numberOfNodes) {
        super(context);
        //#create-actors
        Boolean isAttacker = false;
        Integer timeToLive;
        Set<Integer> triedPeers;
        Set<Integer> testedPeers;
        for(int i = 0; i < numberOfNodes; i++){
            isAttacker = attackerRandom.nextInt(2) == 0;
            timeToLive = ttlRandom.nextInt(10000) + 10000;
            triedPeers = new HashSet<>();
            testedPeers = new HashSet<>();
            allNodes.put(i, context.spawn(Node.create(isAttacker, true, timeToLive,triedPeers, testedPeers), "peer"));
        }
    }

    public static Behavior<ReceivePeerInformation> create(Integer numberOfNodes) {
        return Behaviors.setup(context -> new NetworkMain(context, numberOfNodes));
    }

    @Override
    public Receive<ReceivePeerInformation> createReceive() {
        return newReceiveBuilder().onMessage(ReceivePeerInformation.class, this::doSomething).build();
    }

    private Behavior<ReceivePeerInformation> doSomething(ReceivePeerInformation info) { 
        return this;
    }
}
