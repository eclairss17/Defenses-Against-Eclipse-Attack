package com.example;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

import java.util.Scanner;

public class P2PApplication {
	public static void main(String[] args) {

		Scanner input = new Scanner(System.in);
        System.out.println("Enter the number of nodes");
        int numberOfNodes = Integer.parseInt(input.nextLine());  

        final ActorSystem system = ActorSystem.create();
		ActorRef mainRef = system.actorOf(Props.create(NetworkMain.class, numberOfNodes));
		mainRef.tell(new Node.BeginSimulate(), mainRef);
		
		// final ActorSystem<DefenseNetworkMain.ReceivePeerInformation> defenseNetworkMain =
		// 						 ActorSystem.create(DefenseNetworkMain.create(numberOfNodes), "DefenseSystem");


        System.out.println(">>> Press ENTER to exit <<<");
		input.nextLine();
		input.close();
		system.terminate();
		// defenseNetworkMain.terminate();
    }
}
