package com.skillshare.demo.pojos;

import java.util.HashMap;
import java.util.Map;

public final class Talks {

    private final Map<String, Talk> talks;
    private final int version;

    public Talks(Map<String, Talk> talks, int version) {
        this.talks = new HashMap<>(talks);
        this.version = version;
    }

    public Talk getTalk(String topic) {
        return talks.get(topic);
    }

    public Map<String, Talk> getAllTalks() {
        return new HashMap<>(talks);
    }

    public int getVersion() {
        return version;
    }
}
