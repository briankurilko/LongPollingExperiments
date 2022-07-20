package com.skillshare.demo;

import com.skillshare.demo.api.SkillShareService;
import com.skillshare.demo.pojos.Comment;
import com.skillshare.demo.pojos.Talk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class SkillShareServiceSpamEndpointTestImpl implements SkillShareService {
    private final WebTarget target;
    private final AtomicLong goodCounter;
    private final AtomicLong badCounter;
    private static final Logger LOGGER = LoggerFactory.getLogger(SkillShareServiceSpamEndpointTestImpl.class);

    public SkillShareServiceSpamEndpointTestImpl(String url) {
        target = ClientBuilder.newClient().target(url);
        goodCounter = new AtomicLong(0);
        badCounter = new AtomicLong(0);
    }

    @Override
    public ResponseEntity<Talk> getTalk(String topic) {

        try {
            Response response = target.request().accept(MediaType.TEXT_PLAIN).buildGet().invoke();
            if (Response.Status.Family.SUCCESSFUL.equals(response.getStatusInfo().getFamily())) {
                LOGGER.info("good count: " + goodCounter.incrementAndGet());
            } else {
                LOGGER.info("bad count: " + badCounter.incrementAndGet());
            }
        } catch (Exception ignored){
            LOGGER.info("had an exception: " + badCounter.incrementAndGet());
        }
        System.out.println(Thread.activeCount());
        return null;
    }

    @Override
    public ResponseEntity<List<Talk>> pollAllTalks(String tag, String wait) throws InterruptedException {
        return null;
    }

    @Override
    public ResponseEntity<Void> addNewTalk(String topic, Talk talk) {
        return null;
    }

    @Override
    public ResponseEntity<Void> deleteTalk(String topic) {
        return null;
    }

    @Override
    public ResponseEntity<Void> addComments(String topic, Comment comment) {
        return null;
    }

}
