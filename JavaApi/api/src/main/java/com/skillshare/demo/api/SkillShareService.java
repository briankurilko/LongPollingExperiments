package com.skillshare.demo.api;

import com.skillshare.demo.pojos.Comment;
import com.skillshare.demo.pojos.Talk;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/talks")
public interface SkillShareService {

    @GetMapping("/{topic}")
    ResponseEntity<Talk> getTalk(@PathVariable String topic);

    @GetMapping
    ResponseEntity<List<Talk>> pollAllTalks(@RequestHeader(value = "If-None-Match", required = false) String tag, @RequestHeader(value = "Prefer", required = false) String wait) throws InterruptedException;

    @PutMapping("/{topic}")
    ResponseEntity<Void> addNewTalk(@PathVariable String topic, @RequestBody Talk talk);

    @DeleteMapping("/{topic}")
    ResponseEntity<Void> deleteTalk(@PathVariable String topic);

    @PostMapping("/{topic}/comments")
    ResponseEntity<Void> addComments(@PathVariable String topic, @RequestBody Comment comment);
}