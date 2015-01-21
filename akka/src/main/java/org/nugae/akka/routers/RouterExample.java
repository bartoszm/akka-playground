package org.nugae.akka.routers;

import actor.util.DeadLetterListener;
import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import akka.routing.ActorRefRoutee;
import akka.routing.RoundRobinRoutingLogic;
import akka.routing.Routee;
import akka.routing.Router;
import msg.Work;
import scala.PartialFunction;
import scala.concurrent.duration.Duration;
import scala.runtime.BoxedUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/**
 * Created by bartosz on 21.01.15.
 */
public class RouterExample {
    public static void main(String[] args) {
        ActorSystem sys = ActorSystem.create("MySystem");
        ActorRef master = sys.actorOf(Props.create(Master.class), "master");
        ActorRef dl = sys.actorOf(Props.create(DeadLetterListener.class), "dll");

        sys.eventStream().subscribe(dl, DeadLetter.class);

        Inbox inbox = Inbox.create(sys);
        for(int i = 0; i < 100; ++i) {
            inbox.send(master, new Work("pl", i));
        }

    }
}

class WorkProcessingException extends Exception {
    public WorkProcessingException(Work w) {
        super(w.payload());
    }
}

class Worker extends UntypedActor {
    //yeah I know
    private static Random r = new Random(1);
    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    @Override
    public void onReceive(Object msg) throws Exception {
        if(msg instanceof Work) {
            if(r.nextDouble() < 0.1) {
                log.debug("sending work back to owner");
                sender().tell(msg, self());
                throw new WorkProcessingException((Work) msg);
            }
            log.debug("processed:" + ((Work) msg).payload());
        } else {
            throw new IllegalArgumentException("unknown content " + msg);
        }
    }

    @Override
    public void preStart() throws Exception {
        log.debug("starting " + self().path());
    }

    @Override
    public void unhandled(Object msg) {
        log.warning("message unhandled" + msg);

    }
}

class Master extends AbstractActor {
    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private Router router;

    Master() {

        List<Routee> routees = new ArrayList<>(5);
        for(int i = 0; i < 5; ++i) {
            ActorRef ref = getContext().actorOf(Props.create(Worker.class), "w" + i);
            getContext().watch(ref);
            routees.add(new ActorRefRoutee(ref));
        }
        router = new Router(new RoundRobinRoutingLogic(), routees);
    }

    @Override
    public PartialFunction<Object, BoxedUnit> receive() {
        return ReceiveBuilder
                .match(Terminated.class, this::handleTermination)
                .match(Work.class, this::delegate)
                 .build();
    }

    private void delegate(Work work) {
        log.debug("work {}", sender());
        router().route(work, sender());
    }

    private Router router() {
        return router;
    }

    private void removeFromRouter(ActorRef ref) {
        router = router.removeRoutee(ref);
    }


    private void handleTermination(Terminated p) {
        log.warning("worker terminated: {}", p.actor().path());
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return new OneForOneStrategy(10, Duration.create("1 minute"),
                (Throwable e) -> {
                    if (e instanceof WorkProcessingException) {
                        log.info("terminating {}", sender());
                        removeFromRouter(sender());

                        return SupervisorStrategy.stop();
                    }
                    return SupervisorStrategy.restart();
                }
        );
    }
}