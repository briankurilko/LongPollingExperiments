package com.skillshare.demo;

import com.skillshare.demo.api.SkillShareService;
import com.skillshare.demo.pojos.Comment;
import com.skillshare.demo.pojos.Talk;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SkillShareServiceNoDbImpl implements SkillShareService {

    private final Map<String, Talk> talks;
    private int version;
    private final ScheduledExecutorService executor;
    private final List<CountDownLatch> work;

    private static final Pattern WAIT_REGEX = Pattern.compile("\\bwait=(\\d+)");

    public SkillShareServiceNoDbImpl() {
        this.talks = new HashMap<>();
        this.version = 0;
        this.executor = Executors.newSingleThreadScheduledExecutor();
        // If your tasks takes longer than 10 seconds, it will still create a new task after 10 seconds
        // and put it in the queue of tasks that await completion.
        this.work = new CopyOnWriteArrayList<>();
    }

    @Override
    public ResponseEntity<Talk> getTalk(String topic) {
        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");

        synchronized (this) {
            if (talks.containsKey(topic)) {
                return new ResponseEntity<>(talks.get(topic), headers, HttpStatus.OK);
            }
        }

        throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("No talk '%s' found", topic));
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
            int currentVersion;
            synchronized (this) {
                currentVersion = version;
            }
            if (clientVersion != currentVersion) {
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

        synchronized (this) {
            talks.put(topic, Talk.builder()
                    .title(topic)
                    .presenter(talk.getPresenter())
                    .summary(talk.getSummary())
                    .comments(new ArrayList<>())
                    .build());
        }

        updated();

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    public ResponseEntity<Void> deleteTalk(String topic) {
        synchronized (this) {
            talks.remove(topic);
        }
        updated();
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    public ResponseEntity<Void> addComments(String topic, Comment comment) {
        if (comment == null || comment.getAuthor() == null || comment.getMessage() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad comment data");
        }
        synchronized (this) {
            if (talks.containsKey(topic)) {
                talks.get(topic).getComments().add(comment);
                updated();
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("No talk '%s' found", topic));
    }

    private ResponseEntity<List<Talk>> talkResponse() {
        List<Talk> talksList = new ArrayList<>();
        MultiValueMap<String, String> headers = new HttpHeaders();
        synchronized (this) {
            talksList.addAll(talks.values());
            headers.add("Content-Type", "application/json");
            headers.add("ETag", String.valueOf(version));
            headers.add("Cache-Control", "no-store");
        }

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
        synchronized (this) {
            version++;
        }
        work.forEach(CountDownLatch::countDown);
        work.clear();
    }
}
