package com.example;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

import java.util.*;

// #greeter
public class Node extends AbstractBehavior<Node.Greet> {

	public Boolean isAttacker;
	public Boolean online;
	public Integer timeToLive;
	public Set<Integer> triedPeers;
	public Set<Integer> testedPeers;

	public static final class Greet {
		public final String whom;
		public final ActorRef<GreeterBot.Greeted> replyTo;

		public Greet(String whom, ActorRef<GreeterBot.Greeted> replyTo) {
		this.whom = whom;
		this.replyTo = replyTo;
		}
	}

	public static Behavior<Greet> create(Boolean isAttacker, Boolean online, Integer timeToLive, 
										Set<Integer> triedPeers, Set<Integer> testedPeers) {
		return Behaviors.setup(context -> new Node(context, isAttacker, online, timeToLive, triedPeers, testedPeers));
	}

	private Node(ActorContext<Greet> context, Boolean isAttacker, Boolean online, Integer timeToLive, 
					Set<Integer> triedPeers, Set<Integer> testedPeers) {
		super(context);
		this.isAttacker = isAttacker;
		this.online = online;
		this.timeToLive = timeToLive;
		this.triedPeers = triedPeers;
		this.testedPeers = testedPeers;
	}

	@Override
	public Receive<Greet> createReceive() {
		return newReceiveBuilder().onMessage(Greet.class, this::onGreet).build();
	}

	private Behavior<Greet> onGreet(Greet command) {
		getContext().getLog().info("Hello {}!", command.whom);
		//#greeter-send-message
		command.replyTo.tell(new GreeterBot.Greeted(command.whom, getContext().getSelf()));
		//#greeter-send-message
		return this;
	}
}


