package com.example;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import org.junit.ClassRule;
import org.junit.Test;

//#definition
public class AkkaQuickstartTest {

    @ClassRule
    public static final TestKitJunitResource testKit = new TestKitJunitResource();
//#definition

    //#test
    @Test
    public void testGreeterActorSendingOfGreeting() {
        // TestProbe<Node.Greeted> testProbe = testKit.createTestProbe();
        // ActorRef<Node.Greet> underTest = testKit.spawn(Node.create(), "greeter");
        // underTest.tell(new Node.Greet("Charles", testProbe.getRef()));
        // testProbe.expectMessage(new Node.Greeted("Charles", underTest));
    }
    //#test
}
