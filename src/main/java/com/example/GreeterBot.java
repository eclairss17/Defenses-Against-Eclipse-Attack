package com.example;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

import java.util.Objects;

public class GreeterBot extends AbstractBehavior<GreeterBot.Greeted> {

    public static final class Greeted {
        public final String whom;
        public final ActorRef<Node.Greet> from;
    
        public Greeted(String whom, ActorRef<Node.Greet> from) {
          this.whom = whom;
          this.from = from;
        }
    
    // #greeter
        @Override
        public boolean equals(Object o) {
          if (this == o) return true;
          if (o == null || getClass() != o.getClass()) return false;
          Greeted greeted = (Greeted) o;
          return Objects.equals(whom, greeted.whom) &&
                  Objects.equals(from, greeted.from);
        }
    
        @Override
        public int hashCode() {
          return Objects.hash(whom, from);
        }
    
        @Override
        public String toString() {
          return "Greeted{" +
                  "whom='" + whom + '\'' +
                  ", from=" + from +
                  '}';
        }
    // #greeter
    }

    public static Behavior<Greeted> create(int max) {
        return Behaviors.setup(context -> new GreeterBot(context, max));
    }

    private final int max;
    private int greetingCounter;

    private GreeterBot(ActorContext<GreeterBot.Greeted> context, int max) {
        super(context);
        this.max = max;
    }

    @Override
    public Receive<GreeterBot.Greeted> createReceive() {
        return newReceiveBuilder().onMessage(GreeterBot.Greeted.class, this::onGreeted).build();
    }

    private Behavior<GreeterBot.Greeted> onGreeted(GreeterBot.Greeted message) {
        greetingCounter++;
        getContext().getLog().info("Greeting {} for {}", greetingCounter, message.whom);
        if (greetingCounter == max) {
            return Behaviors.stopped();
        } else {
            message.from.tell(new Node.Greet(message.whom, getContext().getSelf()));
            return this;
        }
    }
}
