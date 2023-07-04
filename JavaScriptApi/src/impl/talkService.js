let version = 0;
const talks = new Map();
let waiting = [];
const defaultHeaders = { "Content-Type": "text/plain" };

function getTalk(topic) {
  let headers = defaultHeaders;
  if (talks.has(topic)) {
    headers = { "Content-Type": "application/json" };
    return {
      body: talks.get(topic),
      headers,
    };
  }

  return { status: 404, body: `No talk '${title}' found`, headers };
}

function pollAllTalks(tag, wait) {
  const waitTime = /\bwait=(\d+)/.exec(wait);
  const tagWithoutQuotes = /"(.*)"/.exec(tag);

  if (!tagWithoutQuotes || tagWithoutQuotes[1] != version) {
    return talkResponse();
  }
  if (!waitTime) {
    return { status: 304 };
  }
  return waitForChange(Number(waitTime[1]));
}

function addNewTalk(topic, talk) {
  if (
    !talk ||
    typeof talk.presenter != "string" ||
    typeof talk.summary != "string"
  ) {
    return { status: 400, body: "Bad talk data", headers: defaultHeaders };
  }
  talks.set(topic, {
    title: topic,
    presenter: talk.presenter,
    summary: talk.summary,
    comments: [],
  });
  updated();
  return { status: 204, headers: defaultHeaders };
}

function deleteTalk(topic) {
  if (talks.has(topic)) {
    talks.delete(topic);
    updated();
  }
  return { status: 204 };
}

function addComments(topic, comment) {
  if (
    !comment ||
    typeof comment.author != "string" ||
    typeof comment.message != "string"
  ) {
    return { status: 400, body: "Bad comment data" };
  }

  if (talks.has(topic)) {
    talks.get(topic).comments.push(comment);
    updated();
    return { status: 204 };
  }
  return { status: 404, body: `No talk '${title}' found` };
}

function waitForChange(waitTime) {
  return new Promise((resolve) => {
    waiting.push(resolve);
    setTimeout(() => {
      if (!waiting.includes(resolve)) return;
      waiting = waiting.filter((r) => r != resolve);
      resolve({ status: 304 });
    }, waitTime * 1000);
  });
}

function updated() {
  version++;
  waiting.forEach((resolve) => resolve(talkResponse()));
  waiting = [];
}

function talkResponse() {
  let currentTalks = [];
  for (let talk of talks.values()) {
    currentTalks.push(talk);
  }
  return {
    body: JSON.stringify(currentTalks),
    headers: {
      "Content-Type": "application/json",
      ETag: `"${version}"`,
      "Cache-Control": "no-store",
    },
  };
}

export { getTalk, pollAllTalks, addNewTalk, deleteTalk, addComments };
