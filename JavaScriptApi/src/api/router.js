import { Router } from "express";
import {
  getTalk,
  pollAllTalks,
  addNewTalk,
  deleteTalk,
  addComments,
} from "../impl/talkService.js";

const router = Router();

router.get("/:topic", (req, res) => {
  console.log("hello?");
  const response = getTalk(req.params["topic"]);
  res
    .status(response.status)
    .setHeader("Content-Type", response.headers["Content-Type"])
    .send(response.body);
});

router.get("", async (req, res) => {
  const response = await pollAllTalks(req.get("If-None-Match"), req.get("Prefer"));

  if (response.status === 304) {
    res.status(304).send();
  } else {
    res
      .setHeader("Content-Type", response.headers["Content-Type"])
      .setHeader("etag", response.headers["ETag"])
      .setHeader("Cache-Control", response.headers["Cache-Control"])
      .send(response.body);
  }
});

router.put("/:topic", (req, res) => {
  const response = addNewTalk(req.params["topic"], req.body);
  if (response.status === 400) {
    res
      .status(400)
      .setHeader("Content-Type", response.headers["Content-Type"])
      .send(response.body);
  } else {
    res.status(response.status).send();
  }
});

router.delete("/:topic", (req, res) => {
  deleteTalk(req.params["topic"]);
  res.status(204).send();
});

router.post("/:topic/comments", (req, res) => {
  const response = addComments(req.params["topic"], req.body);
  if (response.status >= 400) {
    res.status(response.status).send(response.body);
  } else {
    res.status(response.status).send();
  }
});

export default router;
