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
  getTalk(req.params["topic"]);
  res.send("hi");
});

router.get("", (req, res) => {
  pollAllTalks(req.headers("If-None-Match"), req.headers("Prefer"));
});

router.put("/:topic", (req, res) => {
  addNewTalk(req.params["topic"], req.body);
});

router.delete("/:topic", (req, res) => {
  deleteTalk(req.params["topic"]);
});

router.post("/:topic/comments", (req, res) => {
  addComments(req.params["topic"], req.body);
});

export default router;
