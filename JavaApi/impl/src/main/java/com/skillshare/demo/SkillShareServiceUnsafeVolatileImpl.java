package com.skillshare.demo;

import com.skillshare.demo.api.SkillShareService;
import com.skillshare.demo.pojos.Comment;
import com.skillshare.demo.pojos.Talk;
import com.skillshare.demo.pojos.Talks;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SkillShareServiceUnsafeVolatileImpl implements SkillShareService {

    private volatile Talks talks; // THIS ISN'T THREAD SAFE EITHER, after thinking about it. Update operations
                                  // are not atomic - you could have one thread delete while the other puts, and whichever
                                  // finishes first changes the state. So deletes or puts might not work.
    private final ScheduledExecutorService executor;
    private final List<CountDownLatch> work;

    private static final Pattern WAIT_REGEX = Pattern.compile("\\bwait=(\\d+)");

    public SkillShareServiceUnsafeVolatileImpl() {
        this.talks = new Talks(new HashMap<>(), 0);
        this.executor = Executors.newSingleThreadScheduledExecutor();
        this.work = new CopyOnWriteArrayList<>();
    }

    @Override
    public ResponseEntity<Talk> getTalk(String topic) {
        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");

        return Optional.ofNullable(talks.getTalk(topic))
                .map(talk -> new ResponseEntity<>(talk, headers, HttpStatus.OK))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("No talk '%s' found", topic)));
    }

    @Override
    public ResponseEntity<List<Talk>> pollAllTalks(String tag, String wait) throws InterruptedException {
        Integer waitTime = null;
        if (wait != null) {
            Matcher matcher = WAIT_REGEX.matcher(wait);
            if (matcher.matches()) {
                waitTime = Integer.parseInt(matcher.group(1));
            }
        }
        if (tag == null) {
            return talkResponse();
        } else {
            int clientVersion = Integer.parseInt(tag.replace("\"", ""));
            if (clientVersion != talks.getVersion()) {
                return talkResponse();
            }
        }

        if (waitTime == null) {
            return new ResponseEntity<>(null, null, HttpStatus.NOT_MODIFIED);
        }

        return waitForChange(waitTime);
    }

    @Override
    public ResponseEntity<Void> addNewTalk(String topic, Talk talk) {

        if (talk == null
                || talk.getPresenter() == null
                || talk.getSummary() == null
                || talk.getTitle() != null
                || talk.getComments() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad talk data");
        }

        Map<String, Talk> allTalks = talks.getAllTalks();
        int currentVersion = talks.getVersion();

        allTalks.put(topic, Talk.builder()
                .title(topic)
                .presenter(talk.getPresenter())
                .summary(talk.getSummary())
                .comments(new ArrayList<>())
                .build());

        talks = new Talks(allTalks, currentVersion + 1);

        updated();

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    public ResponseEntity<Void> deleteTalk(String topic) {
        Map<String, Talk> allTalks = talks.getAllTalks();
        allTalks.remove(topic);
        int currentVersion = talks.getVersion();

        talks = new Talks(allTalks, currentVersion + 1);

        updated();
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    public ResponseEntity<Void> addComments(String topic, Comment comment) {
        if (comment == null || comment.getAuthor() == null || comment.getMessage() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad comment data");
        }
        if (talks.getTalk(topic) != null) {
            Map<String, Talk> allTalks = talks.getAllTalks();
            allTalks.get(topic).getComments().add(comment);
            int currentVersion = talks.getVersion();
            talks = new Talks(allTalks, currentVersion + 1);
            updated();
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);

        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("No talk '%s' found", topic));
    }

    private ResponseEntity<List<Talk>> talkResponse() {
        MultiValueMap<String, String> headers = new HttpHeaders();
        List<Talk> talksList = new ArrayList<>(talks.getAllTalks().values());
        headers.add("Content-Type", "application/json");
        headers.add("ETag", String.valueOf(talks.getVersion()));
        headers.add("Cache-Control", "no-store");


        return new ResponseEntity<>(talksList, headers, HttpStatus.OK);
    }

    private ResponseEntity<List<Talk>> waitForChange(Integer waitTime) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        work.add(latch);
        AtomicBoolean timedOut = new AtomicBoolean(false);
        executor.schedule(() -> {
            work.remove(latch);
            timedOut.set(true);
            latch.countDown();
        }, waitTime, TimeUnit.SECONDS);
        latch.await();
        if (timedOut.get()) {
            return new ResponseEntity<>(null, null, HttpStatus.NOT_MODIFIED);
        }
        return talkResponse();
    }

    private void updated() {
        work.forEach(CountDownLatch::countDown);
        work.clear();
    }
}
