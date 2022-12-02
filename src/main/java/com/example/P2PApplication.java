package com.example;

import akka.actor.typed.ActorSystem;

import java.util.Scanner; 
import java.io.IOException;

public class P2PApplication {
	public static void main(String[] args) {

		Scanner input = new Scanner(System.in);
        System.out.println("Enter the number of nodes");
        int numberOfNodes = Integer.parseInt(input.nextLine());  

        final ActorSystem<NetworkMain.ReceivePeerInformation> networkMain =
								 ActorSystem.create(NetworkMain.create(numberOfNodes), "System");


        System.out.println(">>> Press ENTER to exit <<<");
		input.nextLine();
		input.close();
		networkMain.terminate();
		
    }
}
