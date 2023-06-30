let version = 0;
const talks = new Map();
const work = [];

function getTalk(topic) {
  if (talks.has(topic)) {
    return talks.get(topic);
  }

  throw new Error(`No talk ${topic} found`);
}

function pollAllTalks(tag, wait) {}

function addNewTalk(topic, talk) {}

function deleteTalk(topic) {}

function addComments(topic, comment) {}

export { getTalk, pollAllTalks, addNewTalk, deleteTalk, addComments };
