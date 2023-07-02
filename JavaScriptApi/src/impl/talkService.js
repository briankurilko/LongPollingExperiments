let version = 0;
const talks = new Map();
let waiting = [];

function getTalk(topic) {
  if (talks.has(topic)) {
    return talks.get(topic);
  }

  throw new Error(`No talk ${topic} found`);
}

function pollAllTalks(tag, wait) {
    const waitTime = /\bwait=(\d+)/.exec(wait);
    

}

function addNewTalk(topic, talk) {}

function deleteTalk(topic) {}

function addComments(topic, comment) {}

function waitForChange(waitTime) {
  return new Promise((resolve) => {
    waiting.push(resolve);
    setTimeout(() => {
      if (!waiting.includes(resolve)) return;
      waiting = waiting.filter((r) => r != resolve);
      resolve({ status: 304 });
    }, time * 1000);
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
