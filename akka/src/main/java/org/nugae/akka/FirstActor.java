package org.nugae.akka;

import akka.actor.*;
import akka.japi.pf.ReceiveBuilder;
import scala.PartialFunction;
import scala.concurrent.duration.Duration;
import scala.runtime.BoxedUnit;
import test.MyActor;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by bartosz on 20.01.15.
 */




public class FirstActor extends UntypedActor {
    @Override
    public void onReceive(Object msg) throws Exception {
        System.out.println("F " + msg);

    }




    public static void main(String[] args) throws InterruptedException {
        Props firstA = Props.create(FirstActor.class);
        Props secondA = Props.create(SecondActor.class);
        ActorSystem sys = ActorSystem.create("MySystem");
        ActorRef fRef = sys.actorOf(firstA, "first");
        ActorRef sRef = sys.actorOf(secondA, "second");
        ActorRef myActor = sys.actorOf(Props.create(MyActor.class), "scalaActor");
        sRef.tell(new Date(), fRef);
        myActor.tell("ale jaaj", fRef);
        myActor.tell(new Date(), sRef);

        //inbox example
        Inbox inbox = Inbox.create(sys);
        inbox.watch(fRef);
        inbox.send(sRef, 1);

        System.out.println("SOUT:" + inbox.receive(Duration.create(1, TimeUnit.SECONDS)));

        fRef.tell(PoisonPill.getInstance(), ActorRef.noSender());

        System.out.println("SOUT:" + inbox.receive(Duration.create(1, TimeUnit.SECONDS)));

        Thread.sleep(5000);
        sys.shutdown();
    }
}


class SecondActor extends AbstractActor {
    @Override
    public PartialFunction<Object, BoxedUnit> receive() {
        return ReceiveBuilder
                .match(String.class, this::handle)
                .match(Integer.class, this::ping)
                .match(Date.class, this::handle)
                .matchAny(this::unhandled)
                .build();
    }

    private void ping(Integer ping) {

        sender().tell(ping + 1, self());
    }

    private void handle(Date date) {
        System.out.println("received msg at: " + date.getTime() + " from" + sender());
        sender().tell("dupa", self());
    }

    private void handle(String msg) {
        System.out.println("received : " + msg + " from" + sender());
    }
}
