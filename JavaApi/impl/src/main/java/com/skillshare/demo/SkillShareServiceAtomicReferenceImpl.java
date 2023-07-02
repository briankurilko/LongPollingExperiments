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
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SkillShareServiceAtomicReferenceImpl implements SkillShareService {

    private final AtomicReference<Talks> talks; //try atomic stamped reference, maybe
    private final ScheduledExecutorService executor;
    private final List<CompletableFuture<ResponseEntity<List<Talk>>>> work;

    private static final Pattern WAIT_REGEX = Pattern.compile("\\bwait=(\\d+)");

    public SkillShareServiceAtomicReferenceImpl() {
        this.talks = new AtomicReference<>(new Talks(new HashMap<>(), 0));
        this.executor = Executors.newSingleThreadScheduledExecutor();
        this.work = new CopyOnWriteArrayList<>();
    }

    @Override
    public ResponseEntity<Talk> getTalk(String topic) {
        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");

        return Optional.ofNullable(talks.get().getTalk(topic))
                .map(talk -> new ResponseEntity<>(talk, headers, HttpStatus.OK))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("No talk '%s' found", topic)));
    }

    @Override
    public ResponseEntity<List<Talk>> pollAllTalks(String tag, String wait) throws Exception {
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
            if (clientVersion != talks.get().getVersion()) {
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

        while (true) {
            var currentTalks = this.talks.get();
            Map<String, Talk> allTalks = currentTalks.getAllTalks();
            int currentVersion = currentTalks.getVersion();

            allTalks.put(topic, Talk.builder()
                    .title(topic)
                    .presenter(talk.getPresenter())
                    .summary(talk.getSummary())
                    .comments(new ArrayList<>())
                    .build());

            if (this.talks.compareAndSet(currentTalks, new Talks(allTalks, currentVersion + 1))) {
                updated();
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
        }

    }

    @Override
    public ResponseEntity<Void> deleteTalk(String topic) {
        while (true) {
            var currentTalks = this.talks.get();
            Map<String, Talk> allTalks = currentTalks.getAllTalks();
            int currentVersion = currentTalks.getVersion();

            allTalks.remove(topic);

            if (this.talks.compareAndSet(currentTalks, new Talks(allTalks, currentVersion + 1))) {
                updated();
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
        }

    }

    @Override
    public ResponseEntity<Void> addComments(String topic, Comment comment) {
        if (comment == null || comment.getAuthor() == null || comment.getMessage() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad comment data");
        }
        if (talks.get().getTalk(topic) != null) {
            while (true) {
                var currentTalks = this.talks.get();
                if (currentTalks.getTalk(topic) == null) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("No talk '%s' found", topic));
                }
                Map<String, Talk> allTalks = currentTalks.getAllTalks();
                int currentVersion = currentTalks.getVersion();

                allTalks.get(topic).getComments().add(comment);

                if (this.talks.compareAndSet(currentTalks, new Talks(allTalks, currentVersion + 1))) {
                    updated();
                    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
                }
            }
        }

        throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("No talk '%s' found", topic));
    }

    private ResponseEntity<List<Talk>> talkResponse() {
        MultiValueMap<String, String> headers = new HttpHeaders();
        Talks currentTalks = this.talks.get();
        List<Talk> talksList = new ArrayList<>(currentTalks.getAllTalks().values());
        headers.add("Content-Type", "application/json");
        headers.add("ETag", String.valueOf(currentTalks.getVersion()));
        headers.add("Cache-Control", "no-store");

        return new ResponseEntity<>(talksList, headers, HttpStatus.OK);
    }

    private ResponseEntity<List<Talk>> waitForChange(Integer waitTime) throws Exception {
        CompletableFuture<ResponseEntity<List<Talk>>> completableFuture = new CompletableFuture<>();
        work.add(completableFuture);
        executor.schedule(() -> {
            work.remove(completableFuture);
            completableFuture.complete(new ResponseEntity<>(null, null, HttpStatus.NOT_MODIFIED));
        }, waitTime, TimeUnit.SECONDS);
        return completableFuture.get();
    }

    private void updated() {
        ResponseEntity<List<Talk>> response = this.talkResponse();
        work.forEach(completableFuture -> completableFuture.complete(response));
        work.clear();
    }
}
