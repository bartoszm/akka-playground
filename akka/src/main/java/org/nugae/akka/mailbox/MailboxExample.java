package org.nugae.akka.mailbox;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.BoundedMessageQueueSemantics;
import akka.dispatch.RequiresMessageQueue;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;

import java.util.concurrent.TimeUnit;

/**
 * Created by bartosz on 21.01.15.
 */
public class MailboxExample {
    public static void main(String[] args) {
        Config config = ConfigFactory.parseURL(MailboxExample.class.getResource("/mailbox.conf"));

        ActorSystem sys = ActorSystem.create("MySystem", config);


        ActorRef lazyActor = sys.actorOf(Props.create(VeryLazyActor.class), "lazyActor");
        ActorRef customActor = sys.actorOf(Props.create(CustomLazyActor.class), "customLazyActor");
        lazyActor.tell("please process 1", null);
        lazyActor.tell("please process 2", null);

        customActor.tell("please process 1", null);
        customActor.tell("please process 2", null);
        customActor.tell("please process 3", null);

        Timeout timeout = new Timeout(5, TimeUnit.SECONDS);
        Future<Object> ask = Patterns.ask(lazyActor, "fail depends on mailboc.conf", timeout);
        try {
            Await.result(ask, timeout.duration());
        } catch (Exception e) {/* not very elegant :P  */}

        sys.shutdown();
    }
}


class VeryLazyActor extends UntypedActor implements RequiresMessageQueue<BoundedMessageQueueSemantics> {

    @Override
    public void onReceive(Object msg) throws Exception {
        //wait a bit
        TimeUnit.SECONDS.sleep(1);
        System.out.println(msg);
    }
}

class CustomLazyActor extends UntypedActor implements RequiresMessageQueue<CustomQueueSemantics> {

    @Override
    public void onReceive(Object msg) throws Exception {
        //wait a bit
        TimeUnit.MILLISECONDS.sleep(900);
        System.out.println("CCQ: " + msg);
    }
}